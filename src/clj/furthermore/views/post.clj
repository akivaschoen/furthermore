(ns furthermore.views.post
  (:require [hiccup.core :refer :all]
            [markdown.core :refer [md-to-html-string]]
            [typographer.core :refer [smarten]]

            [furthermore.entities.follows :as follows]
            [furthermore.entities.posts :as posts]
            [furthermore.views.common :as common]
            [furthermore.views.util :as vutil]))

(def build (comp (partial common/entry :post)
                 (partial vutil/prepare-text md-to-html-string)
                 (partial vutil/prepare-text smarten)))

(defn content
  [post]
  [:div.container
   [:div#banner.page-header
    (build post)
    (when-let [fs (filter #(= (:kind %) :follow) (:refs post))]
      (html
       [:div.glyphicon.glyphicon-triangle-bottom.arrow]
       [:div.follows
        (for [follow (sort-by :created-on (map #(follows/get :_id (:_id %)) fs))]
          (build follow))]))]])
