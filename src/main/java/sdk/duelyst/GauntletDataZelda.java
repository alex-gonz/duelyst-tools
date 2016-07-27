package sdk.duelyst;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// TODO handle faction-specific neutral minion ratings
public class GauntletDataZelda {
  public static final String ZELDA_GUIDE_URL = "https://docs.google.com/document/d/1r3tX0myAjXHo-EzGmQ2v3E-P2fLCB-8lcCdKkRzeQq0";
  public static final String ZELDA_GUIDE_TEXT_URL = "https://docs.google.com/document/export?format=txt&id=1r3tX0myAjXHo-EzGmQ2v3E-P2fLCB-8lcCdKkRzeQq0";
  public static final String SCISSORBLADES_RATINGS_ZIP_HTML_URL = "https://docs.google.com/spreadsheets/d/1KnK4HWiMi-F_EotwtBtF3wKh3J2kt357NwBv2Ua4HSY/export?format=zip&id=1KnK4HWiMi-F_EotwtBtF3wKh3J2kt357NwBv2Ua4HSY";

  private static final Logger logger = LoggerFactory.getLogger(GauntletDataZelda.class);

  public static Map<Faction, Map<Integer, Collection<Rating>>> load(Collection<Card> cards) throws IOException {
    URL descriptionsUrl;
    URL ratingsUrl;
    try {
      descriptionsUrl = new URL(ZELDA_GUIDE_TEXT_URL);
      ratingsUrl = new URL(SCISSORBLADES_RATINGS_ZIP_HTML_URL);
    } catch (MalformedURLException e) {
      logger.error("Got error trying to parse string as url", e);
      throw e;
    }

    Map<String, Card> nameToCard = generateCardNameMap(cards);
    Collection<Card> generalCards = cards.stream()
      .filter(card -> card.type == CardType.GENERAL)
      .map(card -> card)
      .collect(Collectors.toList());
    generalCards.add(new Card(1, "Neutral Minions", CardType.GENERAL, Faction.NEUTRAL, Rarity.BASIC, 0, 0, 0, "Fake neutral general card"));

    Scanner s = new Scanner(descriptionsUrl.openStream());
    Faction maybeCurrentFaction = null;
    String firstGeneral = null;
    String secondGeneral = null;

    Map<Faction, Map<Integer, Collection<Rating>>> factionToCardIdToRatings = new HashMap<>();
    while (s.hasNextLine()) {
      String line = s.nextLine();

      for (Card general : generalCards) {
        if (line.toLowerCase().startsWith(general.name.toLowerCase()) && !line.toLowerCase().startsWith(general.name.toLowerCase() + ":")) {
          logger.info("Found general: {} on line: {}", general.name, line);
          maybeCurrentFaction = general.faction;
          factionToCardIdToRatings.put(general.faction, new HashMap<>());

          if (line.toLowerCase().startsWith(general.name.toLowerCase() + " / ")) {
            String[] generalNames = line.toLowerCase().split(" / ");
            firstGeneral = generalNames[0];
            secondGeneral = generalNames[1];
          } else {
            firstGeneral = general.name.toLowerCase();
            secondGeneral = null;
          }
          break;
        }
      }

      if (maybeCurrentFaction != null && !line.startsWith("*")) {
        int openBracketIndex = line.indexOf('(');
        int closeBracketIndex = line.indexOf(')');

        if (openBracketIndex != -1 && closeBracketIndex != -1) {
          Optional<String> maybeGeneralPrefix = Optional.of(line.indexOf(':'))  // Other general lines separate name with a colon
            .filter(idx -> idx != -1 && idx < openBracketIndex)
            .map(idx -> line.substring(0, idx));
          String maybeCardName = line.substring(maybeGeneralPrefix.map(String::length).orElse(0), openBracketIndex); // Eg. "Aspect of the Drake "
          Card maybeCard = nameToCard.get(cleanCardName(maybeCardName));
          if (maybeCard != null) {
            try {
              int score = Integer.parseInt("0" + line.substring(openBracketIndex, closeBracketIndex).replaceAll("[^0-9]", ""));
              String note = maybeGeneralPrefix.map(p -> p + ": ").orElse("") + line.substring(closeBracketIndex + 4);

              Map<Integer, Collection<Rating>> currentFactionMap = factionToCardIdToRatings.get(maybeCurrentFaction);
              Optional<Collection<Rating>> existingRatings = Optional.ofNullable(currentFactionMap.get(maybeCard.id));
              Collection<Rating> ratingsList = existingRatings.orElse(new ArrayList<>());

              String generalName;
              if (ratingsList.isEmpty()) {
                generalName = firstGeneral;
              } else {
                generalName = secondGeneral;
              }

              ratingsList.add(new Rating(score, note, generalName));
              currentFactionMap.put(maybeCard.id, ratingsList);
            } catch (NumberFormatException e) {
              String scoreString = line.substring(openBracketIndex, closeBracketIndex);
              logger.error("Couldn't parse expected rating as int: " + scoreString, e);
              throw e;
            }
          } else {
            logger.error("Line wasn't matched but had brackets: \"{}\"", line);
          }
        }
      }
    }

    // add neutral cards to all
    Optional<Map<Integer, Collection<Rating>>> maybeNeutralIdToRatings = Optional.ofNullable(factionToCardIdToRatings.get(Faction.NEUTRAL));
    maybeNeutralIdToRatings.ifPresent(neutralMap -> {
      factionToCardIdToRatings.entrySet().stream().filter(f -> f.getKey() != Faction.NEUTRAL).forEach(playerFactionToMap -> {
        playerFactionToMap.getValue().putAll(neutralMap);
      });
    });
    if (!maybeNeutralIdToRatings.isPresent()) {
      RuntimeException e = new RuntimeException("Couldn't find neutral card descriptions");
      logger.error("Error parsing zelda's guide", e);
      throw e;
    }


    ZipInputStream zis = new ZipInputStream(ratingsUrl.openStream());
    ZipEntry ze;
    while ((ze = zis.getNextEntry()) != null) {
      String fileName = ze.getName();
      if (ze.isDirectory() || !fileName.endsWith(".html")) {
        continue;
      }

      Document d = Jsoup.parse(zis, null, "example.com");
      Elements tableSearchResult = d.select("table > tbody");
      if (tableSearchResult.size() != 1) {
        if (tableSearchResult.isEmpty()) {
          logger.error("Couldn't find table in ratings zip file: {} Exiting...", fileName);
        } else {
          logger.error("Found multiple tables in ratings zip file: {} Exiting...", fileName);
        }
        throw new RuntimeException("Error parsing table in file:" + fileName);
      }

      Elements rows = tableSearchResult.get(0).children();
      if (rows.isEmpty()) {
        logger.error("Parsed table was empty in zipped file: {}", fileName);
        throw new RuntimeException("Error parsing table in file:" + fileName);
      }
      String[] factionAndGeneral = rows.get(0).child(1).text().split(" - ");
      if (factionAndGeneral.length < 2) {
        logger.info("Skipping file: {} because value of faction and general cell was \"{}\"", fileName, rows.get(0).child(1).text());
        continue;
      }
      String faction = factionAndGeneral[0];
      String generalString = factionAndGeneral[1];
      for (int i = 3; i < rows.size(); i++) {
        Elements tableCells = rows.get(i).children();
        String name = tableCells.get(1).text();
        int score;
        try {
          score = Integer.parseInt(tableCells.get(6).text());
        } catch (NumberFormatException e) {
          logger.error("Error parsing rating: " + tableCells.get(7).text() + " with card name:" + name, e);
          throw e;
        }

        String cleanName = cleanCardName(name);
        Optional<Collection<Rating>> maybeRatings = Optional.ofNullable(nameToCard.get(cleanName)).flatMap(card ->
          Optional.ofNullable(factionToCardIdToRatings.get(Faction.valueOf(faction.toUpperCase()))).flatMap(cardIdToRatings ->
            Optional.ofNullable(cardIdToRatings.get(card.id))));
        maybeRatings.ifPresent(ratings -> {
          if (ratings.size() == 1) {
            Rating head = ratings.iterator().next();
            if (head.general.startsWith("neutral") || head.general.contains(generalString)) {
              ratings.remove(head);
              ratings.add(head.setGeneral(generalString));
            } else {
              ratings.add(head.setGeneral(generalString));
            }
          } else if (ratings.size() == 2) {
            Optional<Rating> sameGeneralRating = ratings.stream().filter(rating -> rating.general.contains(generalString)).findFirst();
            sameGeneralRating.ifPresent(rating -> {
              ratings.remove(rating);
              ratings.add(rating.setGeneral(generalString));
            });
          }
        });

        if (!maybeRatings.isPresent()) {
          logger.warn(
            "Couldn't find card for faction {} with general name {} with card name {} with existing ratings {}",
            faction,
            generalString,
            name,
            factionToCardIdToRatings
          );
        }
      }
    }

    return factionToCardIdToRatings;
  }

  private static String cleanCardName(String cardName) {
    return cardName.trim().toLowerCase()
      .replaceAll("[^A-z0-9]", "")
      .replace("etheral", "ethereal")
      .replace("obelisk", "obelysk")
      .replace("maelstorm", "maelstrom")
      .replace("artic", "arctic")
      .replace("judgment", "judgement")
      .replace("harvestor", "harvester")
      .replace("draguar", "draugar")
      .replace("sorceror", "sorcerer");
  }

  private static Map<String, Card> generateCardNameMap(Collection<Card> cards) {
    return cards.stream().collect(Collectors.toMap(card -> cleanCardName(card.name), Function.identity()));
  }
}
