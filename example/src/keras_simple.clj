(ns keras-simple.core
  "https://machinelearningmastery.com/tutorial-first-neural-network-python-keras/"
  (:require [libpython-clj.python
             :refer [import-module
                     get-item
                     get-attr
                     python-type
                     call-attr
                     call-attr-kw
                     att-type-map
                     ->py-dict]
             :as py]
            [clojure.pprint :as pp]))


;;Uncomment this line to load a different version of your python shared library:


;;(alter-var-root #'libpython-clj.jna.base/*python-library* (constantly "python3.7m"))


(py/initialize!)


(defonce np (import-module "numpy"))
(defonce builtins (import-module "builtins"))
(defonce keras (import-module "keras"))
(defonce keras-models (import-module "keras.models"))
(defonce keras-layers (import-module "keras.layers"))
(defonce c-types (import-module "ctypes"))

(defn slice
  ([]
   (call-attr builtins "slice" nil))
  ([start]
   (call-attr builtins "slice" start))
  ([start stop]
   (call-attr builtins "slice" start stop))
  ([start stop incr]
   (call-attr builtins "slice" start stop incr)))


(defonce initial-data (call-attr-kw np "loadtxt"
                                    ["pima-indians-diabetes.data.csv"]
                                    {"delimiter" ","}))


(def features (get-item initial-data [(slice) (slice 0 8)]))

(def labels (get-item initial-data [(slice) (slice 8 9)]))

(defn dense-layer
  [output-size & {:as kwords}]
  (call-attr-kw keras-layers "Dense" [output-size] kwords))


(defn sequential-model
  []
  (call-attr keras-models "Sequential"))


(defn add-layer!
  [model layer]
  (call-attr model "add" layer)
  model)

(defn compile-model!
  [model & {:as kw-args}]
  (call-attr-kw model "compile" []
                kw-args)
  model)


(def model (-> (sequential-model)
                   (add-layer! (dense-layer 12 "input_dim" 8 "activation" "relu"))
                   (add-layer! (dense-layer 8 "activation" "relu"))
                   (add-layer! (dense-layer 1 "activation" "sigmoid"))
                   (compile-model! "loss" "binary_crossentropy"
                                   "optimizer" "adam"
                                   "metrics" (py/->py-list ["accuracy"]))))

;;model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])

(defn fit-model
  [model features labels & {:as kw-args}]
  (call-attr-kw model "fit"
                [features labels]
                kw-args)
  model)


(def fitted-model (fit-model model features labels
                             "epochs" 150
                             "batch_size" 10))


(defn eval-model
  [model features lables]
  (let [model-names (->> (get-attr model "metrics_names")
                         (mapv keyword))]
    (->> (call-attr model "evaluate" features labels)
         (map vector model-names)
         (into {}))))


(def scores (eval-model fitted-model features labels))


(pp/pprint scores)
