package sdk.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OcrConfig {
  private static Collection<OcrDimensions> cachedDimensions = null;

  private static final Properties cachedProperties = new Properties();
  private static final String FILE_NAME = "ocr.properties";

  private static final Logger logger = LoggerFactory.getLogger(OcrConfig.class);

  public static Collection<OcrDimensions> getDimensions() {
    if (cachedDimensions == null) {
      cachedDimensions = Arrays.stream(loadProperties().getProperty("ocr_dimensions").split("    "))
              .map(OcrDimensions::maybeFromString)
              .flatMap(maybeDimension -> maybeDimension.map(Stream::of).orElse(Stream.empty())) // unwrap and filter empty
              .collect(Collectors.toList());

    }

    return cachedDimensions;
  }

  private static File getPropFile() throws URISyntaxException {
    return new File(System.getProperty("user.dir"), FILE_NAME);
  }

  private static Properties loadProperties() {
    if (cachedProperties.isEmpty()) {
      try (FileInputStream stream = new FileInputStream(getPropFile())) {
        cachedProperties.load(stream);
      } catch (IOException | URISyntaxException e) {
        logger.error("Error trying to load properties file: {} with stack trace {}", FILE_NAME, e.getStackTrace());
      }
    }

    return cachedProperties;
  }
}
