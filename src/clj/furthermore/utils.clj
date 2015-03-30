(ns furthermore.utils
  (require [clojure.string :as str]
           [clj-time.coerce :as c]
           [clj-time.local :as l]
           [environ.core :refer [env]]))

(def site-url (if (env :dev)
                "http://localhost:3000/"
                "http://furthermore-alpha.herokuapp.com/"))

(defn convert-to-java-date
  [joda-date]
  (c/to-date joda-date))

(defn create-url-name
  [entity]
  (-> (or (:title entity)
          "Untitled")
      (str/replace #"[\.,-\/#!\?$%\^&\*\'\";:{}=\-_`~()]" "")
      (str/replace #" " "-")
      str/lower-case))

(defn create-url-path
  [entity]
  (case (:type entity)
    :post "post/"
    :static "/"
    :topic "topic/"
    ""))

(defn create-url-date
  [entity]
  (let [title (create-url-name entity)
        date (l/format-local-time (:created-on entity) :date)]
   (str date "-" title)))

(defn get-excerpt
  [text]
  (str (subs text 0 49) "…"))
