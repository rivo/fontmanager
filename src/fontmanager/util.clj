(ns fontmanager.util
  "Some utilities used by the other packages.")

; The file extensions that are fonts.
(def ^{:private true} font-exts [".ttf" ".pfb"]) ; Still missing: woff, otf.

; Is it a font?
(defn font?
  "Evaluates whether a file name is a font"
  [file-name]
  (some #(.endsWith file-name %) font-exts))

; Is it a ZIP file?
(defn zip?
  "Evaluates whether a file name is a ZIP file"
  [file-name]
  (.endsWith file-name ".zip"))

; MD5.
(defn md5
  "Returns an MD5 hash of a string."
  [string]
  (let [digest (java.security.MessageDigest/getInstance "MD5")]
    (format "%032x" (BigInteger. 1 (.digest digest (.getBytes string "UTF-8"))))))

; MD5 of a font file.
(defn- font-md5-gen
  "Returns an MD5 hash of a font file (as stored in the font list)."
  [font-index]
  (if (vector? font-index)
    (md5 (apply str (map #(if (instance? java.io.File %) (.getName %) (.toString %)) font-index)))
    (md5 (.getName font-index))))

; Memoized version of font-md5-gen.
(def font-md5 (memoize font-md5-gen))

; Returns the extension of a font.
(defn font-extension
  "Returns the extension of a font."
  [font-index]
  (let [file-name (if (vector? font-index)
                    (last font-index)
                    (.getName font-index))]
    (re-find #"\w+$" file-name)))
