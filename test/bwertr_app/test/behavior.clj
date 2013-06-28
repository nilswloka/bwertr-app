(ns bwertr-app.test.behavior
  (:require [io.pedestal.app :as app]
            [io.pedestal.app.protocols :as p]
            [io.pedestal.app.tree :as tree]
            [io.pedestal.app.messages :as msg]
            [io.pedestal.app.render :as render]
            [io.pedestal.app.util.test :as test])
  (:use clojure.test
        bwertr-app.behavior
        [io.pedestal.app.query :only [q]]))

;; Test a transform function

(deftest init-transform-sets-ratings
  (is (= (init-transform {} {msg/type :init msg/topic [:ratings] :ratings [1 2 3]}) [1 2 3])))

;; Build an application, send a message to a transform and check the transform
;; state

(deftest no-ratings-when-starting-app
  (let [app (app/build bwertr-app)]
    (app/begin app)
    (is (vector?
         (test/run-sync! app [{msg/type :init msg/topic [:ratings] :ratings []}])))
    (is (= (-> app :state deref :data-model :ratings) []))))

;; Use io.pedestal.app.query to query the current application model

(deftest test-query-ui
  (let [app (app/build bwertr-app)
        app-model (render/consume-app-model app (constantly nil))]
    (app/begin app)
    (is (test/run-sync! app [{msg/topic [:ratings] msg/type :init :ratings []}]))
    (is (= (q '[:find ?v
                :where
                [?n :t/path [:own-rating]]
                [?n :t/transforms ?v]]
              @app-model)
           [["x"]]))))
