(ns fontmanager.handler
  "A font manager/viewer which displays local fonts.
  Starting point for the application."
  (:use clojure.java.io
        compojure.core
        fontmanager.list
        fontmanager.render)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as response]
            [clojure.data.json :as json])
  (:import (java.util.prefs Preferences)
           (java.lang System)
           (java.awt GraphicsEnvironment)))

; The user preferences for this application.
(def pref-node (.node (Preferences/userRoot) "fontmanager"))

; Define HTTP routes.
(defroutes app-routes
  (GET "/" [] (response/resource-response "index.html" {:root "public"})) ; Get started.
  (GET "/fontdir" [] (.get pref-node "font-dir" "")) ; Get last used font directory.
  (GET "/homedir" [] (System/getProperty "user.home")) ; Get user's home directory.
  (GET "/subdirs/:dir" [dir] ; Get files in sub-directory.
       (let [file-dir (file dir)
             files (.listFiles file-dir)
             directories (filter #(.isDirectory %) files)
             parent (.getParent file-dir)
             subdirs (map #(hash-map :path (.getPath %) :name (.getName %)) directories)
             subdirs-with-parent (conj subdirs {:path (if (nil? parent) dir parent) :name "[parent directory]"})]
         (json/write-str subdirs-with-parent)))
  (GET "/fonts/:dir" [dir] ; Get a list of fonts. Also set new font directory.
       (.put pref-node "font-dir" dir)
       (->> (fonts-memo dir)
            (map #(dissoc % :index))
            json/write-str))
  (GET ["/sample/:id/:width/:height/:text", ; Return a GIF preview of a font.
        :id #"[0-9a-f]{32}",
        :width #"\d+",
        :height #"\d+"]
       [id width height text]
       {:status 200
        :headers {"Content-type" "image/gif"}
        :body (render
               (get (font-map-memo (.get pref-node "font-dir" nil)) id)
               (Integer/parseInt width)
               (Integer/parseInt height)
               text)})
  (GET ["/fontname/:id", :id #"[0-9a-f]{32}"] [id] ; Get the family name of a font.
       (let [font (get (font-map-memo (.get pref-node "font-dir" nil)) id)
             obj (font-obj font)]
         (.getFamily obj)))
  (GET ["/installed"] [] ; Get a sorted array of names of fonts installed on the system.
       (let [env (GraphicsEnvironment/getLocalGraphicsEnvironment)
             fonts (.getAllFonts env)]
         (->> fonts
              (map #(.getFontName %))
              sort
              json/write-str)))
  (route/resources "/")
  (route/not-found "Not Found"))

; Start the application.
(def app
  (handler/site app-routes))
