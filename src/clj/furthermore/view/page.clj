(ns furthermore.view.page
  (:require [hiccup.core :refer :all]
            [typographer.core :as typo :refer [smarten]]

            [furthermore.view.layout :as layout :refer [display-page]]
            [furthermore.entities :as entities :refer [get-page]]))

(defn display-static-page
  [page]
  (let [page (entities/get-page page)]
    (layout/display-page
     :page
     (:title page)
     (html
      [:div {:id "static"
             :class "container"}
       [:div {:class "col-xs-12 col-sm-10 col-sm-offset-1"}
        [:div {:class "title"} (typo/smarten (:title page))]
        [:div {:class "body"} (:body page)]]]))))

