(ns protean.transformations.curly
  "Uses output from the analysis transformations to generate a curl
   command structure."
  (:require [clojure.string :as stg]
            [clojure.set :as st]
            [clojure.data.xml :as xml]
            [ring.util.codec :as cod]
            [cheshire.core :as jsn]
            [protean.transformations.analysis :as txan])
  (:import java.net.InetAddress))

;; =============================================================================
;; Helper functions
;; =============================================================================

(defn curly-method-> [entry payload]
  (if (= (:method entry) :get)
    payload
    (str payload " -X " (stg/upper-case (name (:method entry))))))

(defn curly-headers-> [entry payload]
  (let [hstr (map #(str " -H '" (key %) ": " (val %) "'") (:headers entry))]
    (str payload (apply str hstr))))

(defn curly-form-> [entry payload]
  (if-let [f (:form-keys entry)]
    (str payload " --data '" (stg/join "&" (map #(str (key %) "=" (val %)) f))
      "'")
    payload))

(defn curly-body-> [entry payload]
  (if-let [b (:body-keys entry)]
    (str payload " -H 'Content-type: application/json'" " --data '"
      (jsn/generate-string b) "'")
    payload))

(defn curly-uri-> [entry payload]
  (str payload " '" (:uri entry)))

(defn curly-req-params-> [entry payload]
  (if-let [rp (:req-params entry)]
    (str payload "?" (stg/join "&" (map #(str (key %) "="
      (cod/form-encode (val %))) rp)) "'")
    (str payload "'")))

(defn curly-postprocess-> [entry payload]
  (stg/replace payload "*" "1"))

(defn curly-> [entry]
  (->> "curl -v"
       (curly-method-> entry)
       (curly-headers-> entry)
       (curly-form-> entry)
       (curly-body-> entry)
       (curly-uri-> entry)
       (curly-req-params-> entry)
       (curly-postprocess-> entry)))


;; =============================================================================
;; Transformation functions
;; =============================================================================

(defn curly-analysis-> [project proj-payload host port]
  (let [paths (:paths proj-payload)]
    (let [analysed (txan/analysis-> project proj-payload host port)]
      (map #(curly-> %) analysed))))
