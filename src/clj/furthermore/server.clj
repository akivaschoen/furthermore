(ns furthermore.server
  (:require [clojure.edn :as edn :refer [read-string]]
            [clojure.java.io :as io]

            [compojure.core :refer [GET POST context defroutes]]
            [compojure.handler :refer [site]]
            [compojure.response :refer [render]]
            [compojure.route :refer [resources]]
            [environ.core :refer [env]]
            [medley.core :refer [map-keys]]
            [liberator.core :refer [defresource resource]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]

            [furthermore.contents :refer [display-contents-page]]
            [furthermore.entities :refer :all]
            [furthermore.home :refer [display-home-page]]
            [furthermore.page :as page :refer [display-static-page]]
            [furthermore.post :refer [display-post-page]]
            ;[furthermore.newsfeed :refer [get-feed]]
            [furthermore.repository :refer [initialize-db-connection]]
            [furthermore.update :refer [display-update-page]]
            [furthermore.updates :refer [display-updates-page]]
            [furthermore.utils :as utils])
  (:gen-class))

;;
;; EDN Functions
;;
(defn- params->map
  [params]
  (let [m (map-keys keyword params)]
    (if (= clojure.lang.PersistentVector (type (:authors m)))
      m
      (update m :authors vector))))

(defn records->maps
  "Returns a map with values converted to EDN-friendly natives (e.g.,
  records become generic maps)."
  [result]
  (let [result (transient (into {} result))]
      (doseq [k [:created-on :last-updated]]
        (assoc! result k (utils/joda-date->java-date (k result))))
      (doseq [k [:parent :topic]]
        (assoc! result k (into {} (k result))))
      (assoc! result :authors (map #(into {} %) (:authors result)))
      (persistent! result)))

;;
;; Blog Update Dispatch
;;
(defn- dispatch-update*
  [fn entity]
  ((comp add-post fn) entity))

(defmulti dispatch-update :kind)

(defmethod dispatch-update "post"
  [entity]
  (dispatch-update* create-post entity))

(defmethod dispatch-update "follow-up"
  [entity]
  (dispatch-update* create-follow-up entity))

(defmethod dispatch-update "topic"
  [entity]
  ((comp add-entity create-topic) entity))

;;
;; Routes & Resources
;;
(defmulti redirect :kind)

(defn- redirect*
  [entity]
  (str (utils/create-url-path entity) (utils/create-entity-url entity)))

(defmethod redirect "follow-up"
  [ctx]
  (println (:_id ctx))
  (let [post (get-follow-up (:_id ctx))]
    (redirect* (get-parent post))))

(defmethod redirect "post"
  [ctx]
  (let [post (get-post (:_id ctx))]
    (redirect* post)))

(defmethod redirect "topic"
  [ctx]
  "contents")

(defresource update-site
  [type]
  :allowed-methods [:post]
  :available-media-types ["text/html" "application/edn" "application/x-www-form-urlencoded"]
  :post! (fn [ctx] (let [form-params (params->map (get-in ctx [:request :form-params]))]
                     (dispatch-update form-params)
                     {:_id (:_id form-params)
                      :kind (:kind form-params)}))
  :post-redirect? (fn [ctx]
                    {:location (str utils/site-url (redirect ctx))}))

(defresource return-result
  [task]
  :allowed-methods [:get]
  :available-media-types ["application/edn"]
  :handle-ok (pr-str task))

(defroutes routes
  ;; API calls
  (GET "/" [] (display-home-page))
  (GET "/contents" [] (display-contents-page))
  (GET "/updates" [] (display-updates-page))
  (GET "/post/:title" [title] (display-post-page title))
  (GET "/admin/add-follow-up" [] (display-update-page :follow-up))
  (GET "/admin/add-post" [] (display-update-page :post))
  (GET "/admin/add-topic" [] (display-update-page :topic))
  (GET "/api/posts" [] (return-result (map records->maps (get-entities :posts))))
  (GET "/api/topics" [] (return-result (map records->maps (get-entities :topics))))
  (POST "/api/update/:kind" [kind] (update-site kind))
  (GET "/page/:page" [page] (page/display-static-page page))
  ;; Disabled until RSS feed is fixed (ANY "/rss.xml" [] (get-feed))
  (resources "/"))

(def app
  (do (initialize-db-connection)
      (wrap-params routes)))

;;
;; This Is Where It Will All Go Wrong for You
;;
(defn -main
  "Launches Furthermore."
  [& port]
  (let [port (Integer. (or (first port) (env :port) 5000))]
    (run-jetty (site #'app) {:port port :join? false})
    (println "Furthermore up and running on port" port)))
