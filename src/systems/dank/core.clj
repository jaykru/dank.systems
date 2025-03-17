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
            [ring.middleware.content-type]
            [optimus-img-transform.core :refer [transform-images]]
            [ring.util.response :as response]
            [clojure.spec.alpha :as s])
  (:import [javax.imageio ImageIO]
           [java.awt Image]
           [java.awt.image BufferedImage]
           [java.io ByteArrayOutputStream]
           [java.awt RenderingHints]))

;; handy snippet for debugging
;; (import '[javax.swing ImageIcon JLabel JFrame])
;; (defn display-image-object [image-object]
;;   (let [icon (ImageIcon. image-object)
;;         label (JLabel. icon)]
;;     (doto (JFrame.)
;;       (.add label)
;;       (.pack)
;;       (.setVisible true))))

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

;; collect resources for outstagram
(s/def ::resource-asset-seq (s/* (s/keys :req-un [::path ::resource]
                                         :opt-un [::last-modified]))) ; TODO: make more precise
(s/fdef get-pix-resources
  :ret ::resource-asset-seq)

(defn get-pix-resources []
  (let [fixup-path (fn [asset]
                     ;; spaces are a pain
                     (assoc asset :path (str/replace (:path asset) " " "_")))
        base-assets (assets/load-assets "public" [#"pix/.*"])]
    (->>
     base-assets
     (map fixup-path))))

(assert (s/valid? ::resource-asset-seq
                  (get-pix-resources)))

;; convert resource to bufimgs (for later resizing)
(s/def ::bufimg-asset-seq ::resource-asset-seq) ; TODO: make more precise
(s/fdef assets->img-assets
  :args ::resource-asset-seq
  :ret ::bufimg-asset-seq)
(defn assets->img-assets [assets]
  (->> assets
       (map #(update % :resource ImageIO/read))))

;; (assert (s/valid? ::bufimg-asset-seq
;;                  (assets->img-assets (get-pix-resources))))))

;; resize all the bufimgs
(def outsta-preferred-h 500)
(def outsta-preferred-w 500)
(defn resize-img
  ([img w h]
   (let [new-img (BufferedImage. w h BufferedImage/TYPE_INT_ARGB)
         g (.createGraphics new-img)]
      (doto g
        (.setRenderingHint RenderingHints/KEY_INTERPOLATION RenderingHints/VALUE_INTERPOLATION_BICUBIC)
        (.drawImage img 0 0 w h nil)
        (.dispose))
      new-img))
  ([img]
   (resize-img img outsta-preferred-w outsta-preferred-h)))

(defn resize-img-assets [assets]
  (->> assets
       (map (fn [asset]
              (update asset :resource resize-img)))))

(defn buffered-image->byte-array [buffered-image format]
  (let [output-stream (ByteArrayOutputStream.)]
    (ImageIO/write buffered-image format output-stream)
    (.toByteArray output-stream)))
(defn awt-image->byte-array
  ([awt-image format]
   (let [buffered-image (BufferedImage. (.getWidth awt-image nil) (.getHeight awt-image nil) BufferedImage/TYPE_INT_ARGB)
         graphics (.createGraphics buffered-image)]
     (try
       (.drawImage graphics awt-image 0 0 nil)
       (buffered-image->byte-array buffered-image format)
       (finally
         (.dispose graphics)))))
  ([awt-image] (awt-image->byte-array awt-image "png")))

(defn image-assets->byte-array-assets [assets]
  (->> assets
       (map #(update % :resource awt-image->byte-array))))

(def byte-array-outsta-assets
  (-> (get-pix-resources)
      (assets->img-assets)
      (resize-img-assets)
      (image-assets->byte-array-assets)))

;; byte-array-outsta-assets

(defn byte-array-img-response [asset]
  (let [img-array (:resource asset)]
    (-> (response/response img-array)
        (assoc :status 200)
        (assoc-in [:headers "Content-Type"] "image/png"))))

;; (byte-array-img-response (first byte-array-outsta-assets))

(defn wrap-img-assets [img-assets handler]
  (fn [request]
    (let [response (handler request)
          is-404 (= (:status response) 404)
          req-path (fn [] (:uri request))
          fixed-response
          (when is-404
            (let [path (req-path)]
              (when (str/includes? path "/pix/")
                (let [asset (first (filter #(= (:path %) path) img-assets))]
                  (when asset
                    (byte-array-img-response asset))))))]
      (or fixed-response response))))

;; (defn always-404 [req]
;;   {:status 404})
;; ((wrap-img-assets byte-array-outsta-assets always-404) {:uri (:path (first byte-array-outsta-assets))})

(defn build-outsta [target-dir]
  (let [pix-paths (map :path byte-array-outsta-assets)
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

(defn merge-website-assets! [source-dir]
  (let [raw-pages (conj (read-and-convert! source-dir)
                        (build-outsta "pix"))
        page-map (format-pages raw-pages)
        css-map (get-css source-dir)]
    (stasis/merge-page-sources {:css css-map
                                :pages page-map})))

(defn get-pages []
  (merge-website-assets! source-dir))

(defn site []
  (->>
   (stasis/serve-pages get-pages)
   (wrap-img-assets byte-array-outsta-assets)))
   ;; (ring.middleware.content-type/wrap-content-type)

(def serve
  (site))

(def server
  (run-jetty serve {:port 3005 :join? false}))

(.stop server)

;; (defn export! []
;;   (stasis/empty-directory! out-dir)
;;   (stasis/export-pages (merge-website-assets! source-dir) out-dir)
;;   (println "Website is done!"))
