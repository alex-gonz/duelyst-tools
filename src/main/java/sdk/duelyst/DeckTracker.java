package sdk.duelyst;

import java.io.IOException;

import sdk.duelyst.console.DuelystConsole;
import sdk.duelyst.console.DuelystConsoleListener;
import sdk.duelyst.console.message.DeckUpdateMessage;
import sdk.duelyst.console.message.DuelystMessage;
import sdk.duelyst.console.message.GameStartedMessage;
import sdk.duelyst.console.message.MessageType;
import sdk.duelyst.ui.DeckTrackerPanel;

public class DeckTracker implements DuelystConsoleListener {
	private DeckTrackerPanel panel;
	private Player player;
	
	public DeckTracker() throws IOException {
		panel = new DeckTrackerPanel();
	}
	
	public void addListener(DuelystConsole console) {
		console.addListener(this);
	}

	public void setVisible(boolean visible) {
		panel.setWindowVisible(visible);
	}

	public void setCompact(boolean compact) {
		panel.setCompact(compact);
	}

	@Override
	public void onMessage(DuelystMessage message) {
		if (player == null && message.type != MessageType.GAME_START) {
			return;
		} else if (!message.playerId.equals("") && player != null && !message.playerId.equals(player.id)) {
			return;
		}
		
		switch (message.type)
		{
		case GAME_START:
			gameStart((GameStartedMessage)message);
			break;
		case DECK_UPDATE:
			deckUpdate((DeckUpdateMessage)message);
			break;
		case EXIT:
		case GAME_END:
			reset();
			break;
		default:
			break;
		}
	}

	private void gameStart(GameStartedMessage message) {
		reset();
		player = new Player(message.playerName, message.playerId, message.deck);
		update();
	}

	private void deckUpdate(DeckUpdateMessage message) {
		player.updateDeck(message.deck);
		update();
	}

	private void reset() {
		player = null;
		panel.clear();
	}
	
	private void update() {
		panel.update(player);
	}
}
