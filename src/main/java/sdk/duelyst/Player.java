package sdk.duelyst;

import java.util.ArrayList;
import java.util.List;

public 

class Player {
	public final String id;
	public final List<Card> deck = new ArrayList<Card>();
	
	public String name = "";
	public int generalId = -1;
	
	public Player(String id, String name, List<Card> deck) {
		this.id = id;
		this.name = name;
		
		updateDeck(deck);
	}
	
	public Player(String id, int generalId) {
		this.id = id;
		this.generalId = generalId;
	}

	public void updateDeck(List<Card> updatedDeck) {
		deck.clear();
		deck.addAll(updatedDeck);

		for (int i = 0; i < this.deck.size(); i++) {
			Card card = this.deck.get(i);
			if (card.type == CardType.GENERAL) {
				generalId = card.id;
				deck.remove(card);
				break;
			}
		}
	}
}