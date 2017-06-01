(ns ez-form.field.fileuploader
  "Field for drag'n'drop file uploads"
  (:require [clojure.string :as str]
            [ez-form.common :as ez.common]
            [ez-form.field :as ez.field]
            [ez-form.i18n :refer [*t* *locale*]]
            [ez-form.util :as ez.util]
            [goog.string :as gstring]
            [goog.string.format]
            [reagent.core :as r]))


(defn- file-drag-hover [e]
  (do (.stopPropagation e)
      (.preventDefault e)
      (set! (-> e .-target .-className) (if (= (.-type e) "dragover")
                                          "zone hover"
                                          "zone"))))

(defn- file-select-handler [c one?]
  (fn [e]
    ;; cancel event and honour styling
    (file-drag-hover e)

    (let [files (ez.util/->array (or (-> e .-target .-files) (-> e .-dataTransfer .-files)))]
      (if one?
        (reset! c (first files))
        (reset! c (into (or @c []) files))))))

(def KiB 1024)
(def MiB (* 1024 1024))
(def GiB (* 1024 1024 1024))
(def TiB (* 1024 1024 1024 1024))

(defn- get-size [file]
  (let [size (.-size file)]
    (cond (>= size TiB) [(gstring/format "%.2f" (float (/ size TiB))) "TB"]
          (>= size GiB) [(gstring/format "%.2f" (float (/ size GiB))) "GB"]
          (>= size MiB) [(gstring/format "%.2f" (float (/ size MiB))) "MB"]
          (>= size KiB) [(gstring/format "%.2f" (float (/ size KiB))) "KB"]
          :else         [size "bytes"])))

(defn cb-remove-file [c file]
  (fn [e]
    (if (sequential? @c)
      (reset! c (remove #(= file %) @c))
      (reset! c nil))))

(defn- show-thumbnail [field form-options c file]
  (let [img-url (r/atom nil)]
    (fn []
      (let [[size suffix] (get-size file)
            reader (js/FileReader.)
            img (.createElement js/document "IMG")]
        (.addEventListener reader "load" (fn []
                                           (reset! img-url (.-result reader))))
        (.readAsDataURL reader file)
        [:div.preview
         (when @img-url
           [:div.image
            [:img (merge {:style (:thumbnail field)} {:src @img-url})]])
         [:div.details
          [:div.size [:span [:strong size] " " suffix]]
          [:div.name [:span (.-name file)]]]
         [:div.remove {:on-click (cb-remove-file c file)}
          (*t* *locale* ::remove-file)]]))))
(defn- show-thumbnail2 [field form-options c file]
  (let [file-name (last (str/split file #"/"))]
    [:div.preview
     [:div.image
      [:img {:style (:thumbnail field) :src file}]]
     [:div.details
      [:div.name [:span file-name]]]
     [:div.remove {:on-click (cb-remove-file c file)}
      (*t* *locale* ::remove-file)]]))
(defmulti show-file (fn [field form-options c file]
                      (cond
                        (string? file) :image
                        :else (.-type file))))
(defmethod show-file "image/jpeg" [field form-options c file]
  ^{:key (.-name file)}[show-thumbnail field form-options c file])
(defmethod show-file "image/png" [field form-options c file]
  ^{:key (.-name file)}[show-thumbnail field form-options c file])
(defmethod show-file "image/gif" [field form-options c file]
  ^{:key (.-name file)} [show-thumbnail field form-options c file])
(defmethod show-file :image [field form-options c file]
  ^{:key file} [show-thumbnail2 field form-options c file])
(defmethod show-file :default [_ _ c file]
  (let [[size suffix] (get-size file)]
    [:div.preview {:key (.-name file)}
     [:div.details
      [:div.size [:span [:strong size] " " suffix]]
      [:div.name [:span (.-name file)]]]
     [:div.remove {:on-click (cb-remove-file c file)}
      (*t* *locale* ::remove-file)]]))

(defmethod ez.field/field :fileuploader [field form-options]
  (let [id (ez.common/get-first field :id :name)
        opts (ez.field/get-opts field [:class :name] form-options)
        c (:cursor field)
        one-file? (not (:multiple field))]
    (if (and (.-File js/window) (.-FileList js/window) (.-FileReader js/window))
      [:div.fileuploader.dragndrop (merge {:id id} opts)
       [:div.zone {:on-change (file-select-handler c one-file?)
                   :on-drag-over file-drag-hover
                   :on-drag-leave file-drag-hover
                   :on-drop (file-select-handler c one-file?)}
        [:span (*t* *locale* ::drop-file-here)]
        (if one-file?
          (map #(show-file field form-options c %) [@c])
          (map #(show-file field form-options c %) @c))]]
      [:div.fileuploader
       [:input (merge {:id id
                       :type :file
                       :value (or @c "")
                       :on-change (file-select-handler c true)} opts)]
       (if one-file?
          (map #(show-file field form-options c %) [@c])
          (map #(show-file field form-options c %) @c))])))
