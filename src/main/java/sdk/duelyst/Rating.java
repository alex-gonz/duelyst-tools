package sdk.duelyst;

public class Rating {
	public final int rating;
	public final String notes;
	public final String general;
	
	public Rating(int rating, String notes, String general) {
		this.rating = rating;
		this.notes = notes;
		this.general = general;
	}

	public Rating setGeneral(String general) {
		return new Rating(rating, notes, general);
	}
}
