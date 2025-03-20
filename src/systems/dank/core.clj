(ns systems.dank.core
  (:require [clojure.string :as str]
            [stasis.core :as stasis]
            [markdown.core :as md]
            [hiccup.page :as hiccup]
            [ring.adapter.jetty :refer [run-jetty]]
            [systems.dank.outsta :as outsta]
            [clojure.core.async :as async]))

(def source-dir "data")

(defn path-to-html-path [path]
  (str/replace path #".md$" ".html"))

(defn read-and-convert! [src]
  (let [md-data  (stasis/slurp-directory src #".*\.md$")
        md-html-paths (map path-to-html-path (keys md-data))
        md-html-content (map md/md-to-html-string-with-meta (vals md-data))
        html-data (stasis/slurp-directory src #".*\.html$")]
    (merge (zipmap md-html-paths md-html-content)
           html-data)))

(defn get-css [src]
  (stasis/slurp-directory src #".*\.css$"))

(defn apply-header-footer [page]
  (hiccup/html5 {:lang "en"}
                [:head
                 [:meta {:charset "UTF-8"}]
                 [:meta {:name "viewport"
                         :content "width=device-width, initial-scale=1.0"}]
                 [:title "jay kruer"]
                 [:link {:rel "stylesheet" :href "/css/default.css"}]
                 [:meta {:http-equiv "x-ua-compatible" :content "ie=edge"}]]
                [:body
                 [:div#main-container
                  [:header
                   [:div#outer-nav
                    [:nav
                     [:div#inner-nav
                      [:div#logo [:b#jaykruer [:em "jay kruer"]]]
                      [:a {:href "/index.html"} "home"]
                      [:a {:href "/about.html"} "about"]
                      [:a {:href "/outsta.html"} "outsta"]
                      [:a {:href "/coolstuff.html"} "cool stuff"]
                      [:a {:href "/contact.html"} "contact"]]]]]
                  [:main {:role "main"}
                   [:h1 (:title (:metadata page))]
                   (:html page)]]]))

(defn format-pages [m]
  (let [html-keys (keys m)
        page-data (map apply-header-footer (vals m))]
    (zipmap html-keys page-data)))

(defn merge-website-assets! [source-dir]
  (let [raw-pages (conj (read-and-convert! source-dir)
                        (outsta/build-page))
        page-map (format-pages raw-pages)
        css-map (get-css source-dir)]
    (stasis/merge-page-sources {:css css-map
                                :pages page-map})))

(defn get-pages []
  (merge-website-assets! source-dir))

(defn site []
  (->>
   (stasis/serve-pages get-pages)
   (outsta/wrap)))

;; (let [c (async/thread (outsta/prefill-cache))]
;;   (async/<!! c))

(def serve
  (site))

(def server
  (run-jetty serve {:port 3010 :join? false}))
