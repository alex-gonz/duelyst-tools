package sdk.utility;

import sdk.duelyst.console.message.GauntletOptionsMessage;

/**
 * Holds the results of Tesseract OCR and allows any cleanup
 */
public class OcrGauntletChoices {
  private OcrNameToCard first;
  private OcrNameToCard second;
  private OcrNameToCard third;

  public OcrGauntletChoices(OcrNameToCard first, OcrNameToCard second, OcrNameToCard third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public GauntletOptionsMessage toGauntletOptionsMessage() {
    return new GauntletOptionsMessage(first.getCard(), second.getCard(), third.getCard());
  }
}
