(ns fontmanager.list
  "Provides lists of fonts for a specific directory."
  (:use fontmanager.util
        fontmanager.zip
        clojure.java.io))

; Is it a font / an archive?
(defn- font-or-archive?
  "Evaluates whether a java.io.File or a file name is a font"
  [file-or-name]
  (let [file-name (if (instance? java.io.File file-or-name) (.getName file-or-name) file-or-name)]
    (or (font? file-name) (zip? file-name))))

; The raw font/archive files where archives are not expanded (lazy seq).
(defn raw-files
  "Returns a lazy sequence of java.io.File objects which are
  either fonts or archives."
  [font-dir]
  (->> font-dir
       file
       file-seq
       (filter #(.isFile %))
       (filter font-or-archive?)))

; A lazy sequence of all font files. Archives are expanded. May contain duplicates.
(defn- generate-font-files
  "Generates a lazy sequence of font files. If the elements are
  java.io.File objects, they point to actual font files. If they
  are vectors, they are part of an archive. The first vector element
  is the java.io.File to the archive, the remaining elements are
  the paths to the contained font file (i.e. archives may contain
  other archives)."
  ([raw-files]
   (when (not-empty raw-files)
     (lazy-cat
      (let [raw-file (first raw-files)
            raw-file-name (.getName raw-file)]
        (cond
         (font? raw-file-name) [raw-file]
         (zip? raw-file-name) (zip-entries raw-file)))
      (generate-font-files (rest raw-files))))))

; Get the file name of the font.
(defn- file-name
  "Returns the name of the font file. If font-index is a java.io.File,
  its name will be used. If it is a vector, the last string of
  the vector is used."
  [font-index]
  (let [archive? (vector? font-index)
        file (if archive? (first font-index) font-index)
        file-name (str (.getName file) (if archive? (str ":" (.getName (java.io.File. (last font-index))))))]
    file-name))

; Sequence of distinct font files. See generate-font-files for the format.
(defn fonts
  "Returns a sequence of (distinct) fonts found in the font-dir directory
  and its sub-directories. Each item of the sequence is either a java.io.File
  object of the font file or a vector. In case of a vector, the font is
  contained in a compressed archive (e.g. a ZIP file). The vector's first
  item is the java.io.File of the archive. The last item is the path to the
  compressed font file. There may be path strings inbetween if the archive
  contains other archives and so on. The returned sequence is sorted by
  the font name, which is assumed from the font's file name."
  [font-dir]
  (->> font-dir ; Start with the directory.
       raw-files ; Get the raw files, archives not expanded.
       generate-font-files ; Expand archives.
       (sort-by file-name) ; Sort by font name.
       (partition-by file-name) ; Group duplicate font names.
       (map first) ; Take first of each group.
       (map #(hash-map :index % :id (font-md5 %) :filename (file-name %))) ; Enrich with more information.
       ))

; Memoized version of fonts function.
(def fonts-memo (memoize fonts))

; The fonts as a map.
(defn font-map
  "Returns a map with hashes as keys and the fonts from fonts-memo as values,
  generated from the specified fonts directory."
  [font-dir]
  (into {} (map #(hash-map (:id %) %) (fonts-memo font-dir))))

; Memoized version of font-map function.
(def font-map-memo (memoize font-map))
