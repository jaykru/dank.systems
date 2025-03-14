(ns systems.dank.core
  (:require [clojure.string :as str]
            [stasis.core :as stasis]
            [markdown.core :as md]
            [hiccup.page :as hiccup]
            [clojure.java.io :as io]
            [optimus.prime :as optimus]
            [optimus.assets :as assets]
            [optimus.optimizations :as optimizations]
            [optimus.strategies :refer [serve-live-assets]]
            [optimus.export]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.content-type]))

(def source-dir "data")
(def out-dir "out")

(defn key-to-html [s]
  (str/replace s #".md" ".html"))

(defn read-and-convert! [src]
  (let [md-data  (stasis/slurp-directory src #".*\.md$")
        md-html-paths (map key-to-html (keys md-data))
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
          [:a {:href "/archive.html"} "archive"]
          [:a {:href "/outsta.html"} "outsta"]
          [:a {:href "/coolstuff.html"} "cool stuff"]
          [:a {:href "/contact.html"} "contact"]]]]]
      [:main {:role "main"}
       [:h1 (:title (:metadata page))]
       (:html page)]]]))

(defn get-pix-assets []
  (letfn [(fixup-path [asset]
            (assoc asset :path (str/replace (:path asset) " " "_")))]
    (->>
     (assets/load-assets "public" [#"pix/.*"])
     (map fixup-path))))

(defn build-outsta [target-dir]
  (let [pix-paths (map :path (get-pix-assets))
        outsta-html (hiccup/html5 {:lang "en"}
                      [:head
                       [:meta {:charset "UTF-8"}]
                       [:meta {:name "viewport"
                               :content "width=device-width, initial-scale=1.0"}]
                       [:link {:rel "stylesheet" :href "/css/gallery.css"}]
                       [:link {:rel "stylesheet" :href "css/default.css"}]]
                      [:body
                       [:div.gallery-container
                        (for [pic pix-paths]
                          [:img {:src pic :loading "lazy"}])]])]
    {"/outsta.html" {:metadata {:title "outsta"} :html outsta-html}}))

(defn format-pages [m]
  (let [html-keys (keys m)
        page-data (map apply-header-footer (vals m))]
    (zipmap html-keys page-data)))

(defn merge-website-assets! [root-dir]
  (let [raw-pages (conj (read-and-convert! source-dir)
                        (build-outsta "pix"))
        page-map (format-pages raw-pages)
        css-map (get-css source-dir)]
    (stasis/merge-page-sources {:css css-map
                                :pages page-map})))

(format-pages (build-outsta "pix"))

(defn get-pages []
  (merge-website-assets! source-dir))

(defn site []
  (->
   (stasis/serve-pages get-pages)
   (optimus/wrap
    get-pix-assets
    optimizations/none
    serve-live-assets)
   (ring.middleware.content-type/wrap-content-type)))

(def serve
  (site))

(def server
  (run-jetty serve {:port 3001 :join? false}))

;; (.start server)

;; (defn export! []
;;   (stasis/empty-directory! out-dir)
;;   (stasis/export-pages (merge-website-assets! source-dir) out-dir)
;;   (println "Website is done!"))
