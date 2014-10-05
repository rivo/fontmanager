# Font Manager

Font Manager is a browser based font utility which allows you to preview the fonts
installed on your computer as well as preview the fonts contained in a directory,
including fonts packaged in ZIP files. It currently scans for TTF (TrueType) and
PFB (Adobe PostScript Type 1) files.

![Font Manager Screenshot](screenshot.jpg?raw=true "Font Manager Screenshot")

## Downloading and Running the JAR File

Download the precompiled JAR file from the "Releases" section on Github.

Then run the application:

    java -jar fontmanager.jar

Open a browser window with the address [http://localhost:3000](http://localhost:3000).

## Running via Source Code

To run the Font Manager from the Clojure source:

    lein ring server
