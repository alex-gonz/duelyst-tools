# duelyst-tools
Deck tracker and gauntlet overlay (overlays CynosureGG's card ratings and notes when you pick cards for gauntlet).

## How to run this for local development ##
1. Clone this repo
2. Install maven if you don't already have it.
3. In the directory of duelyst-tools, run "maven compile" to download all your dependencies.
4. Import this project with your IDE of choice so that it adds the maven dependencies to your classpath.
5. Download [a trained dataset for Tesseract](https://github.com/tesseract-ocr/tessdata) and store it in {{project root}}/tessdata to use OCR
6. Run class DuelystTools with your preferred IDE.

## How to generate an executable jar ##
1. Run `mvn clean compile assembly:single` to create a jar with all the dependencies
2. Copy the aforementioned tessdata folder into the same directory as your jar

## Screenshots ##
http://imgur.com/a/V12hC

## Requirements ##
- Java 8 - https://www.java.com/download/
- Chrome - https://www.google.com/chrome/browser/desktop/

## Notes ##
- Deck tracking is currently broken. Only the gauntlet comments + ratings are available currently.
- The first run will take some time. The program downloads icons from [duelystdb](duelystdb.com) and runs chrome on separate profile which needs to be set up.
- Changing users will require a restart, the program stores the user ID to filter messages from the game.
- This has only been tested on my computer, I'm sure there will be bugs.

## zelda6525's Gauntlet Rankings ##
Gauntlet Tier List: https://docs.google.com/document/d/1r3tX0myAjXHo-EzGmQ2v3E-P2fLCB-8lcCdKkRzeQq0/edit#
