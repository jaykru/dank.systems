(ns systems.dank.outsta
  (:require [clojure.string :as str]
            [hiccup.page :as hiccup]
            [ring.util.response :as response]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io])
  (:import [javax.imageio ImageIO]
           [java.awt.image BufferedImage]
           [java.io ByteArrayOutputStream ByteArrayInputStream]
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

(def img-cache (atom {}))
(defonce preferred-dims-state (atom {:h 1000 :w 1000}))

(defn set-preferred-dims! [h w]
  (when (or (not= h (:h @preferred-dims-state))
            (not= w (:w @preferred-dims-state)))
    (reset! img-cache {})
    (reset! preferred-dims-state {:h h :w w})))

(def outsta-preferred-h 
  (do 
    (add-watch preferred-dims-state :h-watcher 
      (fn [_ _ old-state new-state]
        (when (not= (:h old-state) (:h new-state))
          (reset! img-cache {}))))
    (:h @preferred-dims-state)))

(def outsta-preferred-w 
  (do 
    (add-watch preferred-dims-state :w-watcher 
      (fn [_ _ old-state new-state]
        (when (not= (:w old-state) (:w new-state))
          (reset! img-cache {}))))
    (:w @preferred-dims-state)))

(set-preferred-dims! 500 500)

;; collect resources for outstagram
(s/def ::resource-asset-seq (s/* (s/keys :req-un [::path ::resource]
                                         :opt-un [::last-modified]))) ; TODO: make more precise
(s/fdef get-pix-resources
  :ret ::resource-asset-seq)

(defn path->resource [path]
  (io/resource (str "public/" path)))

(defn path->resource-asset [path]
  (let [resource (path->resource path)]
    (when resource
      {:path path :resource resource})))

;; (assert (s/valid? ::resource-asset-seq
;;                   (get-pix-resources)))

;; convert resource to bufimgs (for later resizing)
(s/def ::bufimg-asset-seq ::resource-asset-seq) ; TODO: make more precise
(s/fdef assets->img-assets
  :args ::resource-asset-seq
  :ret ::bufimg-asset-seq)

(defn resource-asset->img-asset [asset]
  (update asset :resource ImageIO/read))

;; (assert (s/valid? ::bufimg-asset-seq
;;                  (assets->img-assets (get-pix-resources))))))

(defn resize-img
  ([img w h]
   (let [new-img (BufferedImage. w h BufferedImage/TYPE_INT_ARGB)
         g (.createGraphics new-img)]
      (doto g
        (.setRenderingHint RenderingHints/KEY_INTERPOLATION RenderingHints/VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        (.drawImage img 0 0 w h nil)
        (.dispose))
      new-img))
  ([img]
   (resize-img img outsta-preferred-w outsta-preferred-h)))

(defn resize-img-asset [asset]
  (-> asset (update :resource resize-img)))
  
(defn buffered-img->bytes [buffered-img format]
  (let [output-stream (ByteArrayOutputStream.)]
    (ImageIO/write buffered-img format output-stream)
    (.toByteArray output-stream)))
(defn awt-img->bytes
  ([awt-img format]
   (let [buffered-img (BufferedImage. (.getWidth awt-img nil) (.getHeight awt-img nil) BufferedImage/TYPE_INT_ARGB)
         graphics (.createGraphics buffered-img)]
     (try
       (.drawImage graphics awt-img 0 0 nil)
       (buffered-img->bytes buffered-img format)
       (finally
         (.dispose graphics)))))
  ([awt-img] (awt-img->bytes awt-img "png")))

(defn bytes->img [bytes]
  (ImageIO/read (ByteArrayInputStream. bytes)))

(defn img-asset->img-bytes-asset [asset]
  (update asset :resource awt-img->bytes))

(defn img-bytes-asset->response [asset]
  (let [img-bytes (:resource asset)]
    (-> (response/response img-bytes)
        (assoc :status 200)
        (assoc-in [:headers "Content-Type"] "image/png"))))


(defn decode-path [path]
  (-> path
      (java.net.URLDecoder/decode "UTF-8")
      (str/replace #"\+" " ")))

(defn path->img-bytes-response 
  ([path]
   (-> path
       (decode-path)
       (path->resource-asset)
       (resource-asset->img-asset)
       (resize-img-asset)
       (img-asset->img-bytes-asset)
       (img-bytes-asset->response)))
  ([path img-cache]
   (let [cached-response (@img-cache (decode-path path))]
     (if cached-response
       cached-response
       (let [response (path->img-bytes-response path)]
         (swap! img-cache assoc (decode-path path) response)
         response)))))

;; (byte-array-img-response (first byte-array-outsta-assets))
(defn list-resources-relative [path]
  (let [dir (io/file (io/resource path))
        dir-name (.getName dir)]
    (->> (file-seq dir)
         (filter #(.isFile %))
         (map #(.getPath %))
         (map #(str/replace % (re-pattern (str ".*" dir-name "/")) (str dir-name "/"))))))

(defn build-page []
  (let [pix-paths (list-resources-relative "public/pix")
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

(defn wrap [handler]
  (fn [request]
    (let [response (handler request)
          is-404 (= (:status response) 404)
          req-path (:uri request)
          fixed-response
          (when is-404
            (let [path req-path]
              (when (str/includes? path "/pix/")
                (path->img-bytes-response path img-cache))))]
      (or fixed-response response))))

(defn prefill-cache []
  (let [pix-paths (take 1000 (list-resources-relative "public/pix"))]
    (doall (pmap #(path->img-bytes-response % img-cache) pix-paths))))
