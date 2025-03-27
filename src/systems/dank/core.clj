(ns systems.dank.core
  (:require [clojure.string :as str]
            [stasis.core :as stasis]
            [markdown.core :as md]
            [hiccup.page :as hiccup]
            [ring.util.response :as response]
            [ring.adapter.jetty :refer [run-jetty]]
            [systems.dank.outsta :as outsta]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [pantomime.mime :refer [mime-type-of]]))

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
  (if (not (:metadata page))
    ;; pages that don't fit the general structure just get passed through as raw html
    page
    (hiccup/html5 {:lang "en"}
      [:head
       [:meta {:charset "UTF-8"}]
       [:meta {:name "viewport"
               :content "width=device-width, initial-scale=1.0"}]
       [:title "jay kruer"]
       [:link {:rel "stylesheet" :href "/css/default.css"}]
       [:link {:rel "preconnect" :href "https://fonts.gstatic.com" :crossorigin true}]
       [:link {:rel "preconnect" :href "https://fonts.googleapis.com"}]
       [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css2?family=Montserrat:ital,wght@0,100..900;1,100..900&display=swap"}]
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
            [:a {:href "/contact.html"} "contact"]
            [:a {:href "/resume.html"} "resume"]]]]]
        [:main {:role "main"}
         [:h1 (:title (:metadata page))]
         (:html page)]]]
       [:footer
        [:img {:src "/public/clj_alien_rainbow.webp" :width 80 :height 54}]
        [:img {:src "/public/hosted_on_my_pc.webp" :width 80 :height 54}]
        [:img {:src "/public/powered_by_void.webp" :width 80 :height 54}]
        [:img {:src "/public/ai_buddy.webp" :width 80 :height 54}]])))

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

(def get-pages
  (merge-website-assets! source-dir))

;; TODO: move to util
(defn wrap-404 [wrapper handler]
  (fn [request]
    (let [response (handler request)
          is-404 (= (:status response) 404)
          is-root (= (:path request) "/")
          wrapper-response (wrapper request)]
      (if (and is-404
               (not is-root))
        wrapper-response
        response))))

(defn resource->bytes [resource]
  (let [in (io/input-stream resource)
        out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defn drop-leading [path]
  (let [matches (re-find #"^/+(.*)" path)]
    (if matches
      (second matches)
      path)))

(defn ensure-public [path]
  (let [without-leading (drop-leading path)]
    (if (re-find #"^public" without-leading)
      without-leading
      (str "public/" without-leading))))

(defn serve-public-resource [request]
  (if (not= (:uri request) "/")
    (let [resource (io/resource (ensure-public (:uri request)))
          resource-bytes (and resource
                              (resource->bytes resource))]
      (if resource-bytes (-> (response/response resource-bytes)
                             (assoc :status 200)
                             (assoc-in [:headers "Content-Type"]
                                       (mime-type-of resource-bytes)))
          {:status 404 :content "404"}))
    {:status 404 :content "404"}))

(defn site []
  (->>
   (stasis/serve-pages get-pages)
   (outsta/wrap)
   (wrap-404 serve-public-resource)
   ))

(def serve
  (site))
