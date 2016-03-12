package sdk.duelyst;

import java.io.IOException;

import sdk.duelyst.console.DuelystConsole;
import sdk.duelyst.console.DuelystConsoleListener;
import sdk.duelyst.console.message.CardDrawnMessage;
import sdk.duelyst.console.message.CardPlayedMessage;
import sdk.duelyst.console.message.CardReplacedMessage;
import sdk.duelyst.console.message.DuelystMessage;
import sdk.duelyst.console.message.GameStartedMessage;
import sdk.duelyst.console.message.MessageType;
import sdk.duelyst.console.message.StartingHandMessage;
import sdk.duelyst.ui.DeckTrackerPanel;

public class DeckTracker implements DuelystConsoleListener {
	private DeckTrackerPanel panel;
	private Player player;
	private CardPlayedMessage lastPlayed;
	
	public DeckTracker() throws IOException {
		panel = new DeckTrackerPanel();
	}
	
	public void addListener(DuelystConsole console) {
		console.addListener(this);
	}

	@Override
	public void onMessage(DuelystMessage message) {
		if (player == null && message.type != MessageType.GAME_START) {
			return;
		} else if (!message.playerId.equals("") && player != null && !message.playerId.equals(player.id)) {
			return;
		}
		
		System.out.println(message);
		
		switch (message.type)
		{
		case GAUNTLET_OPTIONS:
		case TURN_END:
			break;
		case EXIT:
		case GAME_END:
			reset();
			break;
		case CANCEL:
			cancel(message);
			break;
		case CARD_DRAW:
			cardDraw((CardDrawnMessage)message);
			break;
		case CARD_PLAY:
			cardPlay((CardPlayedMessage)message);
			break;
		case CARD_REPLACE:
			cardReplace((CardReplacedMessage)message);
			break;
		case DECK_UPDATE:
			System.out.println(message.toString());
			break;
		case GAME_START:
			gameStart((GameStartedMessage)message);
			break;
		case STARTING_HAND:
			startingHand((StartingHandMessage)message);
			break;
		}
	}

	public void setVisible(boolean visible) {
		panel.setWindowVisible(visible);
	}

	public void setCompact(boolean compact) {
		panel.setCompact(compact);
	}

	private void cancel(DuelystMessage message) {
		if (lastPlayed != null) {
			player.hand[lastPlayed.playedIndex] = lastPlayed.card;
			lastPlayed = null;
			update();
		}
	}

	private void cardDraw(CardDrawnMessage message) {
		cardDraw(message.card);
	}

	private void cardDraw(Card card) {
		player.deck.remove(card);
		
		for (int i = 0; i < player.hand.length; i++) {
			if (player.hand[i] == null) {
				player.hand[i] = card;
				break;
			}
		}
		
		update();
	}

	private void cardPlay(CardPlayedMessage message) {
		lastPlayed = message;
		player.hand[message.playedIndex] = null;
		update();
	}

	private void cardReplace(CardReplacedMessage message) {
		player.deck.add(player.hand[message.replacedIndex]);
		player.hand[message.replacedIndex] = null;
		
		// TODO Doesn't handle the case where there are no open spots on the field to place it
		if (!message.card.name.equals("Dreamgazer")) {
			player.deck.add(message.card);
		}
		
		cardDraw(message.card);
	}

	private void gameStart(GameStartedMessage message) {
		reset();
		player = new Player(message.playerName, message.playerId, message.deck);
		update();
	}

	private void startingHand(StartingHandMessage message) {
		for (int i = 0; i < message.hand.size(); i++) {
			player.deck.remove(message.hand.get(i));
			player.hand[i] = message.hand.get(i);
		}
		
		update();
	}

	private void reset() {
		player = null;
		lastPlayed = null;
		panel.clear();
	}
	
	private void update() {
		panel.update(player);
	}
}
