package sdk.duelyst;

public class Card {
	public final int id;
	
	public final String name;
	public final CardType type;
	public final Faction faction;
	public final Rarity rarity;
	
	public final int manaCost;
	public final int attack;
	public final int health;

	public String description;

	// the counterplay image file that's downloaded for this card. Needed to recognize images in gauntlet
	public String counterplayName;
	
	public Card(int id, String name, CardType type, Faction faction, Rarity rarity, int manaCost, int attack, int health, String description, String counterplayName) {
		this.id = id;
		
		this.name = name;
		this.type = type;
		this.faction = faction;
		this.rarity = rarity;
		
		this.manaCost = manaCost;
		this.attack = attack;
		this.health = health;
		
		this.description = description;

		this.counterplayName = counterplayName;
	}

	public boolean isMinion() {
		switch (type) {
		case ARTIFACT:
		case SPELL:
		case GENERAL:
			return false;
		default:
			return true;
		}
	}

	@Override
	public String toString() {
		return "Card{" +
						"id=" + id +
						", name='" + name + '\'' +
						'}';
	}
}
