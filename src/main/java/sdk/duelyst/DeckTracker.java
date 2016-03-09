package sdk.duelyst;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JWindow;

import sdk.duelyst.console.DuelystConsoleListener;
import sdk.duelyst.console.message.CardPlayedMessage;
import sdk.duelyst.console.message.DuelystMessage;
import sdk.duelyst.console.message.GameStartedMessage;

public class DeckTracker implements DuelystConsoleListener {
	private Map<String, Player> players = new HashMap<String, Player>();
	private Map<Player, DeckTrackerPanel> panels = new HashMap<Player, DeckTrackerPanel>();
	private CardPlayedMessage lastPlayed;
	
	public DeckTracker() {
		
	}

	@Override
	public void onMessage(DuelystMessage message) {
		switch (message.type)
		{
		case CANCEL:
			cancelLastPlay(message);
			break;
		case CARD_DRAW:
			break;
		case CARD_PLAY:
			break;
		case CARD_REPLACE:
			break;
		case EXIT:
			break;
		case GAME_START:
			gameStart((GameStartedMessage)message);
			break;
		case GAUNTLET_OPTIONS:
			break;
		case STARTING_HAND:
			break;
		case TURN_END:
			break;
		}
	}

	private void cancelLastPlay(DuelystMessage message) {
		if (lastPlayed != null) {
			players.get(message.playerId).hand[lastPlayed.playedIndex] = lastPlayed.card;
			lastPlayed = null;
			
			update(message.playerId);
		}
	}

	private void gameStart(GameStartedMessage message) {
		players.clear(); // TODO: destroy old panels, do this on exit
		panels.clear();
		lastPlayed = null;
		
		update();
	}
	
	private void update() {
		for (Player player : panels.keySet()) {
			panels.get(player).update(player);
		}
	}

	private void update(String playerId) {
		Player player = players.get(playerId);
		panels.get(player).update(player);
	}
}

class Player {
	public final String name;
	public final String id;
	public final Card[] hand = new Card[6];
	public final List<Card> deck = new ArrayList<Card>();
	
	public Player(String name, String id) {
		this.name = name;
		this.id = id;
	}
}

class CardComparator implements Comparator<Card> {
	@Override
    public int compare(Card card1, Card card2) {
        if (card1.manaCost == card2.manaCost) {
        	return card1.name.compareTo(card2.name);
        } else {
        	return card1.manaCost - card2.manaCost;
        }
    }
}

class DeckTrackerPanel extends JPanel {
	private static final long serialVersionUID = 452902188644703086L;
	
	private JWindow window;
	private JTextPane txtPlayer, txtHand, txtDeck;
	
	public DeckTrackerPanel() {
		super(null);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		txtPlayer = new JTextPane();
		txtHand = new JTextPane();
		txtDeck = new JTextPane();

        add(txtPlayer);
        add(txtHand);
        add(txtDeck);
        
        window = new JWindow();
        //window.setBackground(new Color(0, 0, 0, 0));
        //window.setAlwaysOnTop(true);
        //window.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", false);

        //this.setBackground(new Color(0, 0, 0, 0));
        window.add(this);
        
        window.setSize(400, 800);
        window.setLocationRelativeTo(null);
	}
	
	public void update(Player player) {
		Collections.sort(player.deck, new CardComparator());
		
		Map<Card, Integer> deckMap = new HashMap<Card, Integer>();
		for (Card card : player.deck) {
			int count = deckMap.containsKey(card) ? deckMap.get(card) : 0;
			deckMap.put(card, count + 1);
		}
		
		setPlayer(player.name);
		setHand(player.hand);
		setDeck(player.deck, deckMap);
	}
	
	private void setPlayer(String name) {
		txtPlayer.setText(name);
	}
	
	private void setHand(Card[] hand) {
		String handString = "";
		for (Card card : hand) {
			if (card != null) {
				handString += card.name;
			}
			
			handString += System.lineSeparator();
		}
		
		txtHand.setText(handString.trim());
	}
	
	private void setDeck(List<Card> deck, Map<Card, Integer> deckMap) {
		String deckString = "";
		for (Card card : deck) {
			if (card != null) {
				deckString += card.name + " x" + deckMap.get(card);
			}
			
			deckString += System.lineSeparator();
		}
		
		txtDeck.setText(deckString.trim());
	}
}