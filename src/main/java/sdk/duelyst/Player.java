package sdk.duelyst;

import java.util.ArrayList;
import java.util.List;

public 

class Player {
	public final String name;
	public final String id;
	public int generalCardId;
	public final List<Card> deck = new ArrayList<Card>();
	
	public Player(String name, String id, List<Card> deck) {
		this.name = name;
		this.id = id;
		
		updateDeck(deck);
	}

	public void updateDeck(List<Card> updatedDeck) {
		deck.clear();
		deck.addAll(updatedDeck);

		generalCardId = -1;
		for (int i = 0; i < this.deck.size(); i++) {
			Card card = this.deck.get(i);
			if (card.type == CardType.GENERAL) {
				generalCardId = card.id;
				deck.remove(card);
				break;
			}
		}
	}
}