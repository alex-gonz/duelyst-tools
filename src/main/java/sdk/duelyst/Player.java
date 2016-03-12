package sdk.duelyst;

import java.util.ArrayList;
import java.util.List;

import sdk.duelyst.console.DuelystConsole;

public 

class Player {
	public final String name;
	public final String id;
	public int generalCardId;
	public final Card[] hand = new Card[DuelystConsole.HAND_SIZE];
	public final List<Card> deck = new ArrayList<Card>();
	
	public Player(String name, String id, List<Card> deck) {
		this.name = name;
		this.id = id;
		this.deck.addAll(deck);

		generalCardId = -1;
		for (int i = 0; i < this.deck.size(); i++) {
			Card card = this.deck.get(i);
			if (card.type == CardType.GENERAL) {
				generalCardId = card.id;
				this.deck.remove(card);
				break;
			}
		}
	}
}