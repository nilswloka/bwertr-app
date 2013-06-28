(ns ^:shared bwertr-app.behavior
    (:require [clojure.string :as string]
              [io.pedestal.app.messages :as msg]
              [io.pedestal.app :as app]))

(defn set-value-transform [old-value message]
  (:value message))

(defn init-transform [old-value message]
  (:ratings message))

(defn own-rating-transform [old-value message]
  (js/parseInt (:rating message)))

(defn add-rating-derive [old-value {new-model :new-model}]
  (conj old-value (:own-rating new-model)))

(defn percentage-matching [filter-fn items]
  (let [number-of-items (count items)
        number-of-matching-items (count (filter filter-fn items))]
    (* 100 (/ number-of-matching-items number-of-items))))

(def promoters
  (partial percentage-matching #(> % 8)))

(def detractors
  (partial percentage-matching #(< % 7)))

(defn net-promoter-score-derive [old-value {{ratings :ratings} :new-model}]
  (let [promoters-percentage (promoters ratings)
        detractors-percentage (detractors ratings)]
    (- promoters-percentage detractors-percentage)))

(defn own-rating-emit [{message :message}]
  (condp = (msg/type message)
    :init [[:node-create [] :map]
           [:node-create [:own-rating] :map]
           [:value [:own-rating] nil nil]
           [:transform-enable [:own-rating] :rate [{msg/type :rate msg/topic [:own-rating] (msg/param :rating) {}}]]]
    :rate [[:transform-disable [:own-rating] :rate]]
    []))

(def bwertr-app
  {:version 2
   :transform [[:init [:ratings] init-transform]
               [:rate [:own-rating] own-rating-transform]]
   :derive #{{:in #{[:own-rating]} :out [:ratings] :fn add-rating-derive}
             {:in #{[:ratings]} :out [:net-promoter-score] :fn net-promoter-score-derive}}
   :emit [{:in #{[:ratings]} :fn own-rating-emit}
          {:in #{[:*]} :fn (app/default-emitter nil) :mode :always}]})
