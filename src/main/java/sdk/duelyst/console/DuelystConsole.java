package sdk.duelyst.console;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sdk.duelyst.Card;
import sdk.duelyst.DuelystLibrary;
import sdk.duelyst.Faction;
import sdk.duelyst.console.message.*;
import sdk.utility.ChromeUtil;
import sdk.utility.GauntletOcrUtil;
import sdk.utility.OcrGauntletChoices;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class DuelystConsole {
	private static final String URL = "beta.duelyst.com";
	public static final int DECK_SIZE = 40;
	public static final int HAND_SIZE = 6;
	private static final int STARTING_HAND_SIZE = 3;
	public static final int NUM_CARDS_IN_FULL_GAUNTLET_DECK = 30;
	private boolean retryGauntletScreenshotOnFailure;
	private int numGauntletCardsPicked = 0;

	private static final Logger logger = LoggerFactory.getLogger(DuelystConsole.class);

	public static Process launchDebug(String chromePath) throws IOException, URISyntaxException {
		return ChromeUtil.launchDebug(chromePath, URL, "duelyst-profile");
	}

	private WebSocket webSocket;
	private String playerId;

	private List<DuelystConsoleListener> listeners = new ArrayList<DuelystConsoleListener>();

	public void addListener(DuelystConsoleListener toAdd) {
		listeners.add(toAdd);
	}

	private void sendMessage(DuelystMessage message) {
		logger.debug("Sending message: " + message);
		for (DuelystConsoleListener listener : listeners)
			listener.onMessage(message);
	}

	public void connect(String wsUrl) throws IOException, WebSocketException {
		webSocket = ChromeUtil.connectWebSocket(wsUrl, new WebSocketAdapter() {
			@Override
			public void onTextMessage(WebSocket ws, String message) throws Exception {
				handleWsMessage(message);
			}
		});

		ChromeUtil.enableWsConsole(webSocket, true);
		ChromeUtil.enableWsRuntime(webSocket, true);
		ChromeUtil.enableWsPage(webSocket, true);
	}

	public boolean isOpen() {
		return webSocket != null && webSocket.isOpen();
	}

	public void disconnect() {
		if (isOpen()) {
			ChromeUtil.enableWsConsole(webSocket, false);
			ChromeUtil.enableWsRuntime(webSocket, false);
			ChromeUtil.enableWsPage(webSocket, false);
			ChromeUtil.clearWsConsole(webSocket);
			webSocket.disconnect();
		}
	}

	private void handleWsMessage(String message) {
		try {
			JsonObject jsonObject = Json.createReader(new StringReader(message)).readObject();
			if (wsMessageIsResponse(jsonObject)) {
				int id = jsonObject.getInt("id");
				Optional<DuelystMessageState> maybeCallbackTag = Optional.ofNullable(ChromeUtil.callbacks.get(id));
				ChromeUtil.callbacks.remove(id);

				DuelystMessageState state;
				// Handle empty responses, seems to happen on startup when querying old messages
				if (message.contains("\"result\":{}") || !maybeCallbackTag.isPresent()) {
					return;
				} else {
					state = maybeCallbackTag.get();
				}

				switch (state.type)
				{
					case GAUNTLET_SCREENSHOT:
						// Take a gauntlet screenshot and retry if we can't recognize some of the cards
						String imageString = jsonObject.getJsonObject("result").getString("data");
						BufferedImage img = base64ToPng(imageString);

						Optional<OcrGauntletChoices> maybeOptions = GauntletOcrUtil.getGauntletOptions(img);
						if (maybeOptions.isPresent()) {
							OcrGauntletChoices gauntletChoices = maybeOptions.get();
							logger.debug("Found options: " + gauntletChoices);
							sendMessage(gauntletChoices.toGauntletOptionsMessage());
						} else if (retryGauntletScreenshotOnFailure) {
							// Retry on failure
							logger.info("Retrying screenshot because last one didn't recognize all 3 cards.");
							ChromeUtil.captureGauntletScreenshot(webSocket);
						} else {
							logger.info("Screenshot failed, but not retrying because gauntlet was exited");
						}
						break;
					case CANCEL:
					case EXIT:
					case GAME_END:
					case GAUNTLET_OPTIONS:
						throw new IllegalStateException("MessageType." + state.type + " encountered in message stage switching, but shouldn't be.");
					case GAME_START:
					{
						switch (state.stage)
						{
							case 0:
								getResultObjectWs(jsonObject, "deck", state);
								break;
							case 1:
								for (int i = 0; i < DECK_SIZE; i++) {
									// Need separate messages so stages don't get messed up
									getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
								}

								break;
							case 2:
								int cardId = getResultInt(jsonObject, "id");
								List<Card> deck = ((GameStartedMessage)state.message).deck;
								deck.add(DuelystLibrary.cardsById.get(cardId));

								if (deck.size() == DECK_SIZE) {
									sendMessage(state.message);
								}

								break;
						}
						break;
					}
					case JOINED_GAME:
					{
						int factionId = getResultInt(jsonObject, "faction_id");
						int generalId = getResultInt(jsonObject, "general_id");
						playerId = getResultString(jsonObject, "user_id");

						sendMessage(new JoinedGameMessage(playerId, Faction.fromValue(factionId), generalId));
						break;
					}
					case DECK_UPDATE:
					{
						switch (state.stage)
						{
							case 0:
								getResultObjectWs(jsonObject, "_gameSession", state);
								break;
							case 1:
								getResultObjectWs(jsonObject, "players", state);
								break;
							case 2:
								for (int i = 0; i < 2; i++) {
									// Need separate messages so stages don't get messed up
									getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
								}

								break;
							case 3:
								if (getResultString(jsonObject, "playerId").equals(playerId)) {
									state.message = new DeckUpdateMessage(playerId, getResultString(jsonObject, "username"));
									getResultObjectWs(jsonObject, "deck", state);
								}

								break;
							case 4:
								getResultObjectWs(jsonObject, "_cachedCardsExcludingMissing", state);
								break;
							case 5:


								List<JsonObject> cardObjects = new ArrayList<JsonObject>();
								boolean endFound = false;
								do {
									for (JsonValue result : jsonObject.getJsonObject("result").getJsonArray("result")) {
										if (result.getValueType() == ValueType.OBJECT) {
											JsonObject resultObject = (JsonObject)result;
											String name = resultObject.getString("name");

											if (isUint(name)) {
												cardObjects.add(resultObject);
											} else if (name.equals("length")) {
												int count = resultObject.getJsonObject("value").getInt("value");
												((DeckUpdateMessage)state.message).count = count;

												if (count == 0) {
													sendMessage(state.message);
												} else {
													// Need separate messages so stages don't get messed up
													for (JsonObject cardObject : cardObjects) {
														getResultObjectWs(cardObject, new DuelystMessageState(state));
														Thread.sleep(25); // Stops interface from freezing up
													}

													endFound = true;
													break;
												}
											}
										}
										else {
											endFound = true;
											break;
										}
									}
								} while (!endFound);

								break;
							case 6:
								DeckUpdateMessage update = (DeckUpdateMessage)state.message;
								update.deck.add(DuelystLibrary.cardsById.get(getResultInt(jsonObject, "id")));

								if (update.deck.size() == update.count) {
									sendMessage(state.message);
								}

								break;
						}
						break;
					}
					case CARD_PLAY:
					{
						switch (state.stage)
						{
							case 0:
								getResultObjectWs(jsonObject, "cardDataOrIndex", state);
								getResultObjectWs(jsonObject, "_subActions", new DuelystMessageState(state, MessageType.CARD_DRAW));
								break;
							case 1:
								int cardId = getResultInt(jsonObject, "id");
								((CardPlayedMessage)state.message).card = DuelystLibrary.cardsById.get(cardId);
								sendMessage(state.message);
								break;
						}
						break;
					}
					case TURN_END:
					{
						getResultObjectWs(jsonObject, "_subActions", new DuelystMessageState(state, MessageType.CARD_DRAW));
						break;
					}
					case CARD_DRAW:
					{
						switch (state.stage)
						{
							// Starts at 1 because it's sent from other results
							case 1:
								// More than just card draw
								int i = 0;
								for (JsonValue result : jsonObject.getJsonObject("result").getJsonArray("result")) {
									if (!result.toString().contains("length")) {
										getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
										i++;
									} else {
										break;
									}
								}

								break;
							case 2:
								if (getResultString(jsonObject, "type").equals("DrawCardAction")) {
									getResultObjectWs(jsonObject, "cardDataOrIndex", state, true);
								}
								break;
							case 3:
								int cardId = getResultInt(jsonObject, "id");

								// Handles things like Panddo which for some reason show as card draw
								if (!DuelystLibrary.cardsById.containsKey(cardId)) {
									break;
								}

								sendMessage(new CardDrawnMessage(state.playerId, DuelystLibrary.cardsById.get(cardId)));
								break;
						}
						break;
					}
					case CARD_REPLACE:
					{
						switch (state.stage)
						{
							case 0:
								int replacedIndex = getResultInt(jsonObject, "indexOfCardInHand");
								state.message = new CardReplacedMessage(state.playerId, replacedIndex);
								getResultObjectWs(jsonObject, "cardDataOrIndex", state);
								break;
							case 1:
								int cardId = getResultInt(jsonObject, "id");
								((CardReplacedMessage)state.message).card = DuelystLibrary.cardsById.get(cardId);
								sendMessage(state.message);
								break;
						}
						break;
					}
					case STARTING_HAND:
					{
						switch (state.stage)
						{
							case 0:
								getResultObjectWs(jsonObject, "_gameSession", state);
								break;
							case 1:
								getResultObjectWs(jsonObject, "players", state);
								break;
							case 2:
								for (int i = 0; i < 2; i++) {
									// Need separate messages so stages don't get messed up
									getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
								}

								break;
							case 3:
								if (getResultString(jsonObject, "playerId").equals(state.playerId)) {
									getResultObjectWs(jsonObject, "deck", state);
								}

								break;
							case 4:
								getResultObjectWs(jsonObject, "_cachedCardsInHandExcludingMissing", state);
								break;
							case 5:
								state.message = new StartingHandMessage(state.playerId);
								for (int i = 0; i < 3; i++) {
									// Need separate messages so stages don't get messed up
									getResultObjectWs(jsonObject, i, new DuelystMessageState(state));
								}

								break;
							case 6:
								List<Card> hand = ((StartingHandMessage)state.message).hand;
								hand.add(DuelystLibrary.cardsById.get(getResultInt(jsonObject, "id")));

								if (hand.size() == STARTING_HAND_SIZE) {
									sendMessage(state.message);
								}

								break;
						}
						break;
					}
				}
			}
			else if (wsMessageIsExit(jsonObject)) {
				sendMessage(new ExitMessage());
			}
			else if (wsMessageIsConsole(jsonObject)) {
				if (message.contains("bindCards") && message.contains("objectId")) {
					// Just opened gauntlet
					Optional<JsonObject> maybeParams = Optional.ofNullable(jsonObject.getJsonObject("params"));
					Optional<JsonArray> maybeCardsInDeck = maybeParams
									.flatMap(params -> Optional.ofNullable(params.getJsonObject("message")))
									.flatMap(messageObj -> Optional.ofNullable(messageObj.getJsonArray("parameters")))
									.flatMap(parameters -> parameters.getValuesAs(JsonObject.class)
													.stream()
													.filter(parameter -> parameter.containsKey("objectId"))
													.findFirst()
									).flatMap(cardsObject -> Optional.ofNullable(cardsObject.getJsonObject("preview")))
									.flatMap(cardsPreview -> Optional.ofNullable(cardsPreview.getJsonArray("properties")))
									.filter(cardsArray -> cardsArray.size() < NUM_CARDS_IN_FULL_GAUNTLET_DECK);

					maybeCardsInDeck.ifPresent(cardsInDeck -> {
						numGauntletCardsPicked = cardsInDeck.size();
						retryGauntletScreenshotOnFailure = true;
						ChromeUtil.captureGauntletScreenshot(webSocket);
					});
				} else if (message.contains("DeckLayer -\\u003E addCard")) {
					// Added a card
					logger.debug("Got add card:"+message);
					numGauntletCardsPicked++;
					if (numGauntletCardsPicked < NUM_CARDS_IN_FULL_GAUNTLET_DECK) {
						retryGauntletScreenshotOnFailure = true;
						ChromeUtil.captureGauntletScreenshot(webSocket);
					}
				}

				// Just be lazy and get the deck each time
				boolean objectFound = false;
				if (jsonObject.containsKey("params"))
				{
					JsonObject params = jsonObject.getJsonObject("params");
					if (params.containsKey("message"))
					{
						JsonObject messageObject = params.getJsonObject("message");
						if (messageObject.containsKey("parameters"))
						{
							JsonArray parameters = messageObject.getJsonArray("parameters");
							for (JsonValue parameter : parameters)
							{
								if (parameter.getValueType() == ValueType.OBJECT)
								{
									JsonObject parameterObject = (JsonObject)parameter;
									if (parameterObject.containsKey("objectId") && parameterObject.containsKey("preview"))
									{
										String objectId = parameterObject.getString("objectId");
										JsonObject preview = parameterObject.getJsonObject("preview");
										if (preview.containsKey("properties"))
										{
											boolean actionFound = false;
											String playerId = null;
											JsonArray properties = preview.getJsonArray("properties");
											for (JsonValue property : properties)
											{
												if (property.getValueType() == ValueType.OBJECT)
												{
													JsonObject propertyObject = (JsonObject)property;
													if (propertyObject.getString("name").equals("type")
																	&& propertyObject.getString("type").equals("string")
																	&& propertyObject.getString("value").endsWith("Action"))
													{
														actionFound = true;
													}
													else if (propertyObject.getString("name").equals("ownerId")
																	&& propertyObject.getString("type").equals("string"))
													{
														playerId = propertyObject.getString("value");
													}

													if (actionFound && playerId != null) {
														DuelystMessageState state = new DuelystMessageState(playerId, MessageType.DECK_UPDATE);
														ChromeUtil.getObjectProperties(webSocket, objectId, state);

														objectFound = true;
														break;
													}
												}
											}
										}
									}
								}

								if (objectFound) {
									break;
								}
							}
						}
					}
				}

				// Opening gambit cancelled
				if (message.contains("App:onUserTriggeredCancel")) {
					sendMessage(new CancelMessage());
					retryGauntletScreenshotOnFailure = false;
				}
				// Game ended
				else if (message.contains("GameLayer.terminate")) {
					sendMessage(new GameEndedMessage());
				} else {
					JsonObject msg = jsonObject.getJsonObject("params").getJsonObject("message");

					// Joined game
					if (message.contains("App._joinGame")) {
						JsonObject parameter = getParameterObject(msg, 3);

						String objectId = parameter.getString("objectId");
						ChromeUtil.getObjectProperties(webSocket, objectId, new DuelystMessageState("", MessageType.JOINED_GAME));
					}
					// Game start
					if (message.contains("GameSetup.setupNewSession") && message.contains("userId") && message.contains("SDK")) {
						JsonObject parameter = getParameterObject(msg, 4);

						String objectId = parameter.getString("objectId");
						playerId = getPropertyString(parameter, 0);
						String playerName = getPropertyString(parameter, 1);

						DuelystMessageState state = new DuelystMessageState(playerId, MessageType.GAME_START);
						state.message = new GameStartedMessage(playerId, playerName);

						ChromeUtil.getObjectProperties(webSocket, objectId, state);
					}
					// Starting hand
					else if (message.contains("DrawStartingHandAction") && message.contains("VIEW")) {
						JsonObject parameter = getParameterObject(msg, 5);

						String objectId = parameter.getString("objectId");
						String playerId = getPropertyString(parameter, 1);

						if (playerId.equals(this.playerId)) {
							ChromeUtil.getObjectProperties(webSocket, objectId, new DuelystMessageState(playerId, MessageType.STARTING_HAND));
						}
					}
					// Replace card
					else if (message.contains("ReplaceCardFromHandAction") && message.contains("VIEW")) {
						JsonObject parameter = getParameterObject(msg, 5);

						String objectId = parameter.getString("objectId");
						String playerId = getPropertyString(parameter, 2);

						if (playerId.equals(this.playerId)) {
							ChromeUtil.getObjectProperties(webSocket, objectId, new DuelystMessageState(playerId, MessageType.CARD_REPLACE));
						}
					}
					// End turn
					else if (message.contains("EndTurnAction") && message.contains("VIEW")) {
						JsonObject parameter = getParameterObject(msg, 5);

						String objectId = parameter.getString("objectId");
						String playerId = getPropertyString(parameter, 1);

						if (playerId.equals(this.playerId)) {
							ChromeUtil.getObjectProperties(webSocket, objectId, new DuelystMessageState(playerId, MessageType.TURN_END));
						}
					}
					// Play card
					else if (message.contains("PlayCardFromHandAction") && message.contains("VIEW")) {
						JsonObject parameter = getParameterObject(msg, 5);

						String objectId = parameter.getString("objectId");
						String playerId = getPropertyString(parameter, 2);
						int cardIndex = Integer.parseInt(getPropertyString(parameter, 3));

						DuelystMessageState state = new DuelystMessageState(playerId, MessageType.CARD_PLAY);
						state.message = new CardPlayedMessage(state.playerId, cardIndex);

						if (playerId.equals(this.playerId)) {
							ChromeUtil.getObjectProperties(webSocket, objectId, state);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(e.toString());
		}
	}

	private JsonObject getParameterObject(JsonObject jsonObject, int index) {
		return jsonObject.getJsonArray("parameters").getJsonObject(index);
	}

	private String getPropertyString(JsonObject jsonObject, int index) {
		return getPropertyObject(jsonObject, index).getString("value");
	}

	private JsonObject getPropertyObject(JsonObject jsonObject, int index) {
		return jsonObject.getJsonObject("preview").getJsonArray("properties").getJsonObject(index);
	}

	private JsonObject getResultObject(JsonObject jsonObject, String name) {
		for (JsonValue result : jsonObject.getJsonObject("result").getJsonArray("result")) {
			if (result.getValueType() == ValueType.OBJECT) {
				JsonObject resultObject = (JsonObject)result;
				if (resultObject.containsKey("name") && resultObject.getString("name").equals(name)) {
					return resultObject;
				}
			}
		}

		return null;
	}

	private void getResultObjectWs(JsonObject jsonObject, String name, DuelystMessageState state) {
		getResultObjectWs(jsonObject, name, state, false);
	}

	private void getResultObjectWs(JsonObject jsonObject, String name, DuelystMessageState state, boolean ignoreMissing) {
		JsonObject resultObject = getResultObject(jsonObject, name);
		if (resultObject == null) {
			if (ignoreMissing) {
				return;
			} else {
				throw new NoSuchElementException("Name '" + name + "' not found in results.");
			}
		}

		getResultObjectWs(resultObject, state);
	}

	private void getResultObjectWs(JsonObject jsonObject, int index, DuelystMessageState state) {
		JsonObject resultObject = jsonObject.getJsonObject("result").getJsonArray("result").getJsonObject(index);
		getResultObjectWs(resultObject, state);
	}

	private void getResultObjectWs(JsonObject jsonObject, DuelystMessageState state) {
		state.stage++;
		String objectId = jsonObject.getJsonObject("value").getString("objectId");
		ChromeUtil.getObjectProperties(webSocket, objectId, state);
	}

	private int getResultInt(JsonObject jsonObject, String name) {
		return getResultObject(jsonObject, name).getJsonObject("value").getInt("value");
	}

	private String getResultString(JsonObject jsonObject, String name) {
		return getResultObject(jsonObject, name).getJsonObject("value").getString("value");
	}

	private boolean wsMessageIsResponse(JsonObject jsonObject) {
		return jsonObject.containsKey("id");
	}

	private boolean wsMessageIsExit(JsonObject jsonObject) {
		return jsonObject.containsKey("method") && jsonObject.getString("method").equals("Inspector.detached");
	}

	private boolean wsMessageIsConsole(JsonObject jsonObject) {
		return jsonObject.containsKey("method") && jsonObject.getString("method").equals("Console.messageAdded");
	}

	private static boolean isUint(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		} else {
			for (int i = 0; i < s.length(); i++) {
				if (!Character.isDigit(s.charAt(i))) {
					return false;
				}
			}
		}

		return true;
	}

	public static BufferedImage base64ToPng(String imageString) {
		BufferedImage image = null;
		byte[] imageByte;
		try {
			BASE64Decoder decoder = new BASE64Decoder();
			imageByte = decoder.decodeBuffer(imageString);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			image = ImageIO.read(bis);
			bis.close();
		} catch (IOException e) {
			logger.error("Error converting base64 string from Chrome to an actual image: {}", e);
		}
		return image;
	}

}

/* TODO Tasks
 * 
 * Note that it may fail the first time
 * Note that changing accounts will need a restart
 *
 * Save card library and gauntlet helper to disk
 * CARD_DRAW case 2 object is null in fatigue, only in real matches?
 * 
 * Show card image on mouseover
 * Create method to process subactions instead of updating whole deck
 *
 * Card draw on damage: lionheart blessing with grasp of agony
 * Card draw on move: mogwai
 * Card draw on deathwatch: rook
 * Card draw on hailstone (coming from player 2)
 * Handle cards that steal from decks
 * Tusk boar returns on other player's end of turn step, in _cached_resolveSubActions, StartTurnAction, _subActions, PutCardInHandAction
 * Lionheart, AttackAction, _subActions, DrawCardAction
 * Overdraw, indexOfCardInHand is null
 * Void Hunter, other player's AttackAction, _subActions, other player's DieAction, _subActions, DrawCardAction (PutCardInHandAction for snow chaser?)
 * Artifacthunter is PutCardInHandAction
 * Dreamgazer, maybe get whether it was played from subactions
 * 
*/
