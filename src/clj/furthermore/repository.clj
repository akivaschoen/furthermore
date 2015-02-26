(ns furthermore.repository
  (:require [clj-time.local :as l]
            [environ.core :refer [env]]
            [monger.collection :as mc]
            [monger.core :refer [connect-via-uri]]
            [monger.joda-time :refer :all]))

(def ^:private db (atom nil))
(def ^:private db-queue (atom {}))

(defn add-db-queue
  [entity]
  (swap! db-queue assoc (:_id entity) entity))

(defn update-db-queue
  [entity]
  (swap! db-queue update (:_id entity) entity))

(defn get-db-queue
  [id]
  (val (find @db-queue id)))

(defn list-db-queue
  []
  @db-queue)

(defn clear-db-queue
  []
  (reset! db-queue {}))

(defn parse-entity
  [entity]
  (let [entity (as-> entity e
                   (update e :type keyword)
                   (if-not (nil? (:parent e))
                     (update-in e [:parent :type] keyword) 
                     e))
        refs (:references entity)]
    (if-not (empty? refs)
      (reduce #(update-in %1 [:references (.indexOf refs %2) :type] keyword) entity refs)
      entity)))

(defmulti save-entity :type)
(defmethod save-entity :post [entity]
  (let [entity (assoc entity :last-updated (l/local-now))]
    (mc/upsert @db "posts" {:_id (:_id entity)} entity)))
(defmethod save-entity :topic [entity]
  (let [entity (assoc entity :last-updated (l/local-now))]
    (mc/upsert @db "topics" {:_id (:_id entity)} entity)))

(defmulti read-entity :type)
(defmethod read-entity :post [request]
  (let [post (mc/find-map-by-id @db "posts" (:_id request))]
    (parse-entity post)))
(defmethod read-entity :topic [request]
  (let [topic (mc/find-map-by-id @db "topics" (:_id request))]
    (parse-entity topic)))

(defn read-all
  [coll]
  (map parse-entity (mc/find-maps @db coll)))

(defn process-db-queue
  []
  (doseq [entity (vals @db-queue)]
    (save-entity entity)))

(defn initialize-db-connection
  []
  (reset! db (:db (connect-via-uri (env :database-uri)))))