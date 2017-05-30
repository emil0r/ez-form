(ns ez-form.field.fileuploader
  "Field for drag'n'drop file uploads"
  (:require [ez-form.common :as ez.common]
            [ez-form.field :as ez.field]
            [ez-form.util :as ez.util]))


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
        (reset! c (take 1 files))
        (reset! c files)))))

(defn- default-show-file [file]
  [:div.file {:key (.-name file)}
   [:p "Name: " (.-name file)]
   [:p "Type: " (.-type file)]
   [:p "Size: " (.-size file)]])

(defmethod ez.field/field :fileuploader [field form-options]
  (let [id (ez.common/get-first field :id :name)
        opts (ez.field/get-opts field [:class :name] form-options)
        c (:cursor field)
        show-file (or (:show-file field) default-show-file)
        one-file? (if (= (:mode field) :multiple)
                    false
                    true)]
    (if (and (.-File js/window) (.-FileList js/window) (.-FileReader js/window))
      [:div.dragndrop (merge {:id id} opts)
       [:div.zone {:on-change (file-select-handler c one-file?)
                   :on-drag-over file-drag-hover
                   :on-drag-leave file-drag-hover
                   :on-drop (file-select-handler c one-file?)}
        "Drop here"]
       (map show-file @c)]
      [:div "fallback"])))
