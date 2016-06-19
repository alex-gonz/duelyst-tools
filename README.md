# duelyst-tools
Deck tracker and gauntlet overlay (overlays CynosureGG's card ratings and notes when you pick cards for gauntlet).

## How to run this ##
1. Clone this repo and duelyst-utils (https://github.com/alex-gonz/duelyst-utils)
2. Install maven if you don't already have it.
3. In the directory of duelyst-utils, run "maven install" to add it to your local library.
4. In the directory of duelyst-tools, run "maven compile" to download all your dependencies.
5. Import this project with your IDE of choice so that it adds the maven dependencies to your classpath.
6. Download [a trained dataset for Tesseract](https://github.com/tesseract-ocr/tessdata) and store it in {{project root}}/tessdata to use OCR
7. Run class DuelystTools with your preferred IDE.

## Screenshots ##
http://imgur.com/a/V12hC

## Requirements ##
- Java 7 - https://www.java.com/download/
- Chrome - https://www.google.com/chrome/browser/desktop/

## Notes ##
- The first run will take some time and might error (requiring a restart). The program runs chrome on separate profile which needs to be set up.
- Changing users will require a restart, the program stores the user ID to filter messages from the game.
- This has only been tested on my computer, I'm sure there will be bugs.

## zelda6525's Gauntlet Rankings ##
Gauntlet Tier List: https://docs.google.com/document/d/1r3tX0myAjXHo-EzGmQ2v3E-P2fLCB-8lcCdKkRzeQq0/edit#

## duelyst-utils ##
https://github.com/ScottyDoesKnow/duelyst-utils

