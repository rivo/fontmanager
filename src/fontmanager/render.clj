(ns fontmanager.render
  "Renders font samples."
  (:use fontmanager.util
        fontmanager.zip
        ring.util.io)
  (:import (java.awt Color Font RenderingHints Graphics2D)
           (java.awt.geom AffineTransform)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)))

; Get font file.
(defn- font-file
  "Returns a java.io.File of the specified font or nil
  if the file could not be created."
  [font]
  (let [font-index (:index font)]
    (cond
      (nil? font) nil ; Could happen if the provided hash is unknown.
      (vector? font-index) (cond
                       (zip? (.getName (first font-index))) (zip-font-file font-index)
                       :else nil)
      :else font-index)))

; Get a java.swt.Font.
(defn font-obj
  "Returns a Java Font object given the font definition from
  the font list. The font size will be 1pt per default. Returns
  nil if it was impossible to generate the font."
  [font]
  (try
    (if-let [file (font-file font)]
      (case (font-extension (:index font)) ; Test file extension.
       "ttf" (Font/createFont Font/TRUETYPE_FONT file)
       "pfb" (Font/createFont Font/TYPE1_FONT file)
       nil))
    (catch Exception e nil)))

; Render the font sample.
(defn render
  "Renders a sample text to a piped input stream with the specified font
  from the font list."
  [font width height text]
  (piped-input-stream
   (fn [output-stream]
    (let [font-file-obj (font-obj font) ; Attempt to get the java.awt.Font. Size is 1pt.
          font-obj (if (nil? font-file-obj) ; Substitute if attempt failed.
                     (Font. "Arial" Font/PLAIN 1)
                     font-file-obj)
          final-text (if (nil? font-file-obj) ; Replace text with error message if required.
                       "No preview available"
                       text)
          image (BufferedImage. width height BufferedImage/TYPE_BYTE_GRAY) ; Make an image.
          graphics (doto (.createGraphics image) ; Initialize graphics context.
                         (.setBackground Color/WHITE)
                         (.clearRect 0 0 width height)
                         (.setFont font-obj))
          font-metrics (.getFontMetrics graphics) ; Prepare to scale up.
          bounds (.getStringBounds font-metrics final-text graphics)
          test-width (.getWidth bounds)
          test-height (.getHeight bounds)
          height-test (if (< test-width 1E-6) height (/ (* test-height width) test-width))
          scale (if (< height-test height)
                  (if (< test-width 1E-6) 1 (/ width test-width))
                  (if (< test-height 1E-6) 1 (/ height test-height)))
          scaled-font (.deriveFont font-obj (float scale))]
      (doto graphics ; Render font sample.
        (.setFont scaled-font)
        (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
        (.setColor Color/BLACK)
        (.drawString final-text 0 (.getAscent (.getFontMetrics graphics))))
      (ImageIO/write image "gif" output-stream)))))
