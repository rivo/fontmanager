(ns fontmanager.zip
  "Provides access to ZIP archives."
  (:use fontmanager.util)
  (:import (java.io File FileInputStream FileOutputStream ByteArrayOutputStream)
           (java.util.zip ZipInputStream)))

; A sequence of the font files in a ZIP archive. May be empty.
(defn zip-entries
  "Returns a sequence of the font file entries in a ZIP archive where
  each entry is a vector of two items: the ZIP java.io.File and the
  ZIP-internal path of the font."
  [file]
  (try (with-open [zip-file (java.util.zip.ZipFile. file)]
    (->> zip-file
         .entries
         enumeration-seq
         (filter #(not (.isDirectory %)))
         (map #(.getName %))
         (filter #(and (font? %) (-> % java.io.File. .getName (.startsWith ".") not))) ; Exclude *nix system files.
         (map #(vector file %))
         doall))
    (catch java.util.zip.ZipException e []))) ; If the ZIP file could not be opened, return an empty vector.

; Get temporary directory with the font files from the ZIP archive.
(defn- zip-dir
  "Returns a temporary directory which contains the extracted
  font files from the provided java.io.File which is a ZIP archive.
  The file names of the fonts in the temporary directory are the
  md5 hashes for the fonts."
  [zip-file]
  (let [dir (File/createTempFile "fm_" "_zip")]
    (doto dir
      .delete ; Delete the file so we can turn it into a directory.
      .mkdir ; Create the temp directory.
      .deleteOnExit)
    (with-open [fis (FileInputStream. zip-file)
                zip (ZipInputStream. fis)]
      (loop [] ; Loop over archive contents.
        (when-let [entry (.getNextEntry zip)]
          (when (and ; Only accept font files.
                 (not (.isDirectory entry))
                 (font? (.getName entry)))
            (let [font [zip-file (.getName entry)]
                  hash (font-md5 font)
                  buffer (make-array Byte/TYPE 1024)
                  font-file (File. dir hash)] ; Write eligible archive entries to (temporary) font file.
              (.deleteOnExit font-file) ; It's just a temporary file.
              (with-open [fos (FileOutputStream. font-file)] ; Copy ZIP entry stream to file.
                (loop []
                  (let [size (.read zip buffer)]
                    (when (> size 0)
                      (.write fos buffer 0 size)
                      (recur)))))))
          (recur))))
    dir))

; Memoized version of zip-dir.
(def zip-dir-memo (memoize zip-dir))

; Get font file from ZIP archive.
(defn zip-font-file
  "Returns a temporary java.io.File of the specified font which
  corresponds to a ZIP archive."
  [font-index]
  (let [dir (zip-dir-memo (first font-index))
        font-file (File. dir (font-md5 font-index))]
    font-file))

