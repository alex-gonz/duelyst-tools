package sdk.utility;

import sdk.duelyst.Card;

public class OcrNameToCard {
  private String ocrName;
  private Card card;
  private int levenshteinDistance;

  public OcrNameToCard(String ocrName, Card card, int levenshteinDistance) {
    this.ocrName = ocrName;
    this.card = card;
    this.levenshteinDistance = levenshteinDistance;
  }

  public String getOcrName() {
    return ocrName;
  }

  public Card getCard() {
    return card;
  }

  public int getLevenshteinDistance() {
    return levenshteinDistance;
  }

  /**
   * Determines if this was a reasonable guess, or we just picked something from a random set of characters.
   *
   * Eg. Name=Maw, LD=3 means that it could have been an empty string
   */
  public boolean isValid() {
    return card.name.length() / 2 > levenshteinDistance;
  }

  @Override
  public String toString() {
    return "OcrNameToCard{" +
            "ocrName='" + ocrName + '\'' +
            ", card=" + card +
            ", levenshteinDistance=" + levenshteinDistance +
            '}';
  }
}
