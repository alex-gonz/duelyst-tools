package sdk.duelyst;

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
			cancelLastPlay(message.playerId);
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
			break;
		case GAUNTLET_OPTIONS:
			break;
		case STARTING_HAND:
			break;
		case TURN_END:
			break;
		}
	}

	private void cancelLastPlay(String playerId) {
		if (lastPlayed != null) {
			//players.get(playerId).hand[lastPlayed.]
		}
	}
}

class Player {
	public final String name;
	public final String id;
	public final Card[] hand = new Card[6];
	
	public Player(String name, String id) {
		this.name = name;
		this.id = id;
	}
	
	public void setHand() {
		
	}
}

class DeckTrackerPanel extends JPanel {
	private static final long serialVersionUID = 452902188644703086L;
	
	private JWindow window;
	private JTextPane txtHand, txtDeck;
	
	public DeckTrackerPanel() {
		super(null);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		txtHand = new JTextPane();
		txtDeck = new JTextPane();
        
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
	
	public void setHand(Card[] hand) {
		String handString = "";
		for (Card card : hand) {
			if (card != null) {
				handString += card.name;
			}
			
			handString += System.lineSeparator();
		}
		
		txtHand.setText(handString.trim());
	}
	
	public void setDeck(List<Card> deck, Map<Card, Integer> deckMap) {
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