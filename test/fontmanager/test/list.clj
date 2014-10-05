(ns fontmanager.test.list
  (:use clojure.test
        fontmanager.list))

(deftest test-font-list
  (testing "font list"
    (let [font-list (map :filename (fonts "resources/test"))]
      (is (= font-list ["ClearSans-Light.ttf" "OpenSans-Regular.ttf" "bundle.zip:PT_Serif-Web-Italic.ttf" "bundle.zip:SourceSansPro-Light.ttf"])))))
