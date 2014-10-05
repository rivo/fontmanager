(ns fontmanager.test.handler
  (:use clojure.test
        ring.mock.request
        fontmanager.handler))

(deftest test-app
  (testing "index route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))))

  (testing "fontdir route"
    (let [response (app (request :get "/fontdir"))]
      (is (= (:status response) 200))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
