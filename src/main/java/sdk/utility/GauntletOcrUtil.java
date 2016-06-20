package sdk.utility;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sdk.duelyst.DuelystLibrary;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

/**
 * Recognizes gauntlet choices via Tesseract OCR
 */
public class GauntletOcrUtil {
  private static Tesseract ocr = new Tesseract();

  private static Logger logger = LoggerFactory.getLogger(GauntletOcrUtil.class);

  /**
   * Uses Tesseract to retrieve the Gauntlet choices
   * @param img A screenshot of chrome taken by the Chrome debugging protocol
   * @return An Optional containing the String choices that Tesseract found
   */
  public static Optional<OcrGauntletChoices> getGauntletOptions(BufferedImage img) {
    Optional<OcrDimensions> maybeDimensions = getOcrDimensionsForImage(OcrConfig.getDimensions(), img);

    OcrDimensions dimensions;
    if (!maybeDimensions.isPresent()) {
      logger.error("No dimensions parsed from file. Check that it has proper syntax and restart this app.");
      return Optional.empty();
    } else {
      dimensions = maybeDimensions.get();
    }

    int imageHeight = img.getHeight();
    int imageWidth = img.getWidth();

    int top = dimensions.getY() + Integer.max(0, (imageHeight - dimensions.getScreenBaseHeight()) / 2);

    Rectangle firstRectangle = getRectangle(dimensions.getX1(), dimensions, imageWidth, top);
    Rectangle secondRectangle = getRectangle(dimensions.getX2(), dimensions, imageWidth, top);
    Rectangle thirdRectangle = getRectangle(dimensions.getX3(), dimensions, imageWidth, top);

    // TODO remove this once we know it works
    File outfile = new File("subscreenshot-of-first-option.png");
    try {
      ImageIO.write(img.getSubimage((int)firstRectangle.getX(), top, dimensions.getTextWidth(), dimensions.getTextHeight()), "png", outfile);
    } catch (IOException e) {
      logger.error(e.toString());
    }

    return getOcrNameToCard(img, firstRectangle).flatMap(first ->
            getOcrNameToCard(img, secondRectangle).flatMap(second ->
                    getOcrNameToCard(img, thirdRectangle).map(third ->
                            new OcrGauntletChoices(first, second, third)
                    )
            )
    );
  }

  /**
   * Generate Rectangle to use for OCR
   * @param xStart number of pixels between the left side of the Rectangle and the left edge of the image
   * @param dimensions Dimensions matching the current screen size
   * @param imageWidth width of the image itself
   * @param top number of pixels between the top of the Rectangle and the top edge of the image
   * @return A Rectangle that bounds the section of the image to use for OCR
   */
  private static Rectangle getRectangle(int xStart, OcrDimensions dimensions, int imageWidth, int top) {
    int firstOptionLeft = xStart + Integer.max(0, (imageWidth - dimensions.getScreenBaseWidth()) / 2);
    return new Rectangle(firstOptionLeft, top, dimensions.getTextWidth(), dimensions.getTextHeight());
  }

  /**
   * Perform OCR on an image in a rectangle with Tesseract
   * @return An OcrNameToCard if any is found
   */
  private static Optional<OcrNameToCard> getOcrNameToCard(BufferedImage img, Rectangle firstRectangle) {
    Optional<String> maybeOcrName = Optional.empty();
    try {
      maybeOcrName = Optional.of(ocr.doOCR(img, firstRectangle));
    } catch (TesseractException e) {
      logger.error("Error while doing ocr: {}", e);
    }
    return maybeOcrName.flatMap(GauntletOcrUtil::findOcrNameToCard).filter(OcrNameToCard::isValid);
  }

  /**
   * Gets Levenshtein distance between name found by OCR and all valid card names, finding the one that's closest
   * @return Nothing if the name found by OCR wouldn't be a card's name, otherwise an OcrNameToCard with
   * the closest card and the Levenshtein distance between that cards's name and the name found by OCR
   */
  private static Optional<OcrNameToCard> findOcrNameToCard(String ocrName) {
    String cleanedName = ocrName.trim();

    if (cleanedName.isEmpty() || cleanedName.length() < 3 || cleanedName.length() - cleanedName.replaceAll("[^A-z]", "").length() > 3) {
      return Optional.empty();
    } else if (DuelystLibrary.cardsByName.containsKey(cleanedName)) {
      return Optional.of(new OcrNameToCard(cleanedName, DuelystLibrary.cardsByName.get(cleanedName), 0));
    }

    return DuelystLibrary.cardsByName.entrySet().stream()
            .map(nameToCard ->
                    new OcrNameToCard(
                            cleanedName,
                            nameToCard.getValue(),
                            StringUtils.getLevenshteinDistance(cleanedName, nameToCard.getKey())
                    )
            )
            .min((o1, o2) -> Integer.compare(o1.getLevenshteinDistance(), o2.getLevenshteinDistance()));
  }

  /**
   * Calculates the largest dimension that is smaller than current screen size.
   * If none are found, returns smallest dimension (if any).
   */
  private static Optional<OcrDimensions> getOcrDimensionsForImage(Collection<OcrDimensions> dimensions, BufferedImage image) {
    int height = image.getHeight();
    int width = image.getWidth();

    Optional<OcrDimensions> largestSmallerThanImage = dimensions.stream()
            .filter(d -> d.getScreenBaseWidth() <= width && d.getScreenBaseHeight() <= height)
            .max((d1, d2) -> Integer.compare(d1.getScreenBaseWidth(), d2.getScreenBaseWidth()));

    if (largestSmallerThanImage.isPresent()) {
      return largestSmallerThanImage;
    } else {
      logger.info("Couldn't find a dimension smaller than current screen size. Using smallest configured one.");
      return dimensions.stream()
              .min((d1, d2) -> Integer.compare(d1.getScreenBaseWidth(), d2.getScreenBaseWidth()));
    }
  }

}
