package sdk.utility;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// TODO comment saying what the fields mean
public class OcrDimensions {
  private int x1;
  private int x2;
  private int x3;
  private int y;
  private int textWidth;
  private int textHeight;
  private int screenBaseWidth;
  private int screenBaseHeight;

  public OcrDimensions(int x1, int x2, int x3, int y, int textWidth, int textHeight, int screenBaseWidth, int screenBaseHeight) {
    this.x1 = x1;
    this.x2 = x2;
    this.x3 = x3;
    this.y = y;
    this.textWidth = textWidth;
    this.textHeight = textHeight;
    this.screenBaseWidth = screenBaseWidth;
    this.screenBaseHeight = screenBaseHeight;
  }


  public int getX1() {
    return x1;
  }

  public int getX2() {
    return x2;
  }

  public int getX3() {
    return x3;
  }

  public int getY() {
    return y;
  }

  public int getTextWidth() {
    return textWidth;
  }

  public int getTextHeight() {
    return textHeight;
  }

  public int getScreenBaseWidth() {
    return screenBaseWidth;
  }

  public int getScreenBaseHeight() {
    return screenBaseHeight;
  }

  public static Optional<OcrDimensions> maybeFromString(String s) {
    String[] dimensionStrings = s.split(",");
    if (dimensionStrings.length == 8) {
      List<Integer> dimensions = Arrays.stream(dimensionStrings).map(Integer::parseInt).collect(Collectors.toList());
      return Optional.of(new OcrDimensions(
              dimensions.get(0),
              dimensions.get(1),
              dimensions.get(2),
              dimensions.get(3),
              dimensions.get(4),
              dimensions.get(5),
              dimensions.get(6),
              dimensions.get(7)
      ));
    } else {
      System.out.println("Expected ocr dimensions in config file to have length of 8. Found incorrect one:" + s);
      return Optional.empty();
    }
  }
}
