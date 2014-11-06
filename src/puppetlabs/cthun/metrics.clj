(ns puppetlabs.cthun.metrics
  (:require [clojure.tools.logging :as log]
            [clojure.java.jmx :as jmx]
            [clj-time.core :as time]
            [metrics.gauges :refer [gauge]]
            [metrics.counters :as counters]
            [metrics.meters :as meters]
            [metrics.timers :refer [timer]]
            [cheshire.core :as cheshire]))

(def metrics (atom {}))
(def start-time (time/now))

(def total-messages-in (counters/counter ["puppetlabs.cthun" "global" "total-messages-in"]))
(def total-messages-out (counters/counter ["puppetlabs.cthun" "global" "total-messages-out"]))
(def active-connections (counters/counter ["puppetlabs.cthun" "global" "active-connections"]))
(def rate-messages-in (meters/meter ["puppetlabs.cthun" "global" "rate-messages-in"] "messages received"))
(def rate-messages-out (meters/meter ["puppetlabs.cthun" "global" "rate-messages-out"] "messages sent"))
(def time-in-on-connect (timer ["puppetlabs.cthun" "handlers" "time-in-on-connect"]))
(def time-in-on-text (timer ["puppetlabs.cthun" "handlers" "time-in-on-text"]))
(def time-in-on-close (timer ["puppetlabs.cthun" "handlers" "time-in-on-close"]))
(def time-in-message-queueing (timer ["puppetlabs.cthun" "global" "time-in-message-queueing"]))

(defn- get-cthun-metrics
  "Returns cthun specific metrics as a map"
  []
  {:total-messages-in (counters/value total-messages-in)
   :total-messages-out (counters/value total-messages-out)
   :active-connections (counters/value active-connections)})

(defn- get-memory-metrics
  "Returns memory related metrics as a map"
  []
  (dissoc (jmx/mbean "java.lang:type=Memory") :ObjectName))

(defn- get-thread-metrics
  "Returns thread related metrics as a map"
  []
  (apply dissoc (jmx/mbean "java.lang:type=Threading") [:ObjectName :AllThreadIds]))

; TODO(ploubser): Flesh this out
(defn get-metrics-string
  "Returns some clean jmx metrics as a json string"
  []
  (cheshire/generate-string (-> (assoc {} :memory (get-memory-metrics))
                                (assoc :threads (get-thread-metrics))
                                (assoc :cthun (get-cthun-metrics)))
                            {:pretty true}))


(defn enable-cthun-metrics
  "Defines a set of jmx beans. Kick it on startup"
  []
  (swap! metrics assoc :active-connections active-connections)
  (swap! metrics assoc :total-messages-in total-messages-in)
  (swap! metrics assoc :total-messages-out total-messages-out)
  (swap! metrics assoc :rate-messages-in rate-messages-in)
  (swap! metrics assoc :rate-messages-out rate-messages-out)
  (swap! metrics assoc :time-in-on-connect time-in-on-connect)
  (swap! metrics assoc :time-in-on-text time-in-on-text)
  (swap! metrics assoc :time-in-message-queueing time-in-message-queueing)
  (swap! metrics assoc :time-in-on-close time-in-on-close))
