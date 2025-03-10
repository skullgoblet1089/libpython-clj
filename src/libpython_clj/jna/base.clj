(ns libpython-clj.jna.base
  (:require [tech.jna :as jna]
            [tech.jna.base :as jna-base]
            [camel-snake-kebab.core :refer [->kebab-case]])
  (:import [com.sun.jna Pointer NativeLibrary]
           [libpython_clj.jna PyObject]))



(def ^:dynamic *python-library* "python3.6m")


(defprotocol PToPyObjectPtr
  (convertible-to-pyobject-ptr? [item])
  (->py-object-ptr [item]))


(extend-type PyObject
  PToPyObjectPtr
  (convertible-to-pyobject-ptr? [item] true)
  (->py-object-ptr [item] (.getPointer item)))


(extend-type Pointer
  PToPyObjectPtr
  (convertible-to-pyobject-ptr? [item] true)
  (->py-object-ptr [item] item))


(defn ensure-pyobj
  [item]
  (if-let [retval (->py-object-ptr item)]
    retval
    (throw (ex-info "Failed to get a pyobject pointer from object." {}))))


(defn ensure-pydict
  "The return value of this has to be a python dictionary object."
  [item]
  (ensure-pyobj item))


(defn ensure-pytuple
  "The return value of this has to be a python tuple object."
  [item]
  (ensure-pyobj item))


(defmacro def-pylib-fn
  [fn-name docstring & args]
  `(jna/def-jna-fn *python-library* ~fn-name ~docstring ~@args))


(def size-t-type (type (jna/size-t 0)))


(defn find-pylib-symbol
  [sym-name]
  (.getGlobalVariableAddress ^NativeLibrary (jna-base/load-library *python-library*)
                             sym-name))
