(ns libpython-clj.python.protocols
  "Protocols to help generalize the python bindings.  There is a clear distinction
  made between building a bridging type and thus allowing communication between the
  two systems and building a complete copy of the datastructure of one system in
  another.  Generic objects must only bridge but if we know more about the object
  (like it implements java.util.Map) then we can implement either a bridge or a
  copy.

  Atomic objects:
  * numbers
  * booleans
  * strings-convertible things like strings, keywords, symbols

  Standard containers:
  * list
  * map
  * set
  * tuple (persistent vector of length less than 8)")


(defprotocol PPythonType
  (get-python-type [item]
    "Return a keyword that describes the python datatype of this object."))


(defn python-type
  [item]
  (if item
    (get-python-type item)
    :none-type))


(defprotocol PCopyToPython
  (->python [item options]
    "Copy this item into a python representation.  Must never return nil.
Items may fallback to as-python if copying is untenable."))


(defprotocol PBridgeToPython
  (as-python [item options]
    "Aside from atom types, this means the object represented by a zero copy python
    mirror.  May return nil.  This convertible to pointers get converted
    to numpy implementations that share the backing store."))



(defprotocol PCopyToJVM
  (->jvm [item options]
    "Copy the python object into the jvm leaving no references.  This not copying
are converted into a {:type :pyobject-address} pairs."))


(defprotocol PBridgeToJVM
  (as-jvm [item options]
    "Return a pyobject implementation that wraps the python object."))


(extend-type Object
  PBridgeToPython
  (as-python [item options] nil)
  PCopyToJVM
  (->jvm [item options] item)
  PBridgeToJVM
  (as-jvm [item options] item))


(extend-type Object
  PBridgeToJVM
  (as-jvm [item options] item))


(defprotocol PPyObject
  (dir [item]
    "Get sorted list of all attribute names.")
  (has-attr? [item item-name])
  (get-attr [item item-name])
  (set-attr! [item item-name item-value])
  (callable? [item])
  (has-item? [item item-name])
  (get-item [item item-name])
  (set-item! [item item-name item-value]))


(defprotocol PPyAttMap
  (att-type-map [item]
    "Get hashmap of att name to keyword datatype."))


(extend-type Object
  PPyAttMap
  (att-type-map [item]
()    (->> (dir item)
        (map (juxt identity (comp python-type
                                  (partial get-attr item))))
        (into (sorted-map)))))


(defprotocol PyCall
  (do-call-fn [callable arglist kw-arg-map]))


(defn call
  [callable & args]
  (do-call-fn callable args nil))


(defn call-kw
  [callable arglist kw-args]
  (do-call-fn callable arglist kw-args))

(defn call-attr
  "Call an object attribute"
  [item att-name & args]
  (-> (get-attr item att-name)
      (do-call-fn args nil)))


(defn call-attr-kw
  [item att-name arglist kw-map]
  (-> (get-attr item att-name)
      (do-call-fn arglist kw-map)))


(defprotocol PPyObjLength
  "Call the __len__ attribute."
  (len [item]))


(extend-type Object
  PPyObjLength
  (len [item]
    (-> (call-attr item "__len__")
        (->jvm {}))))


(defprotocol PPyObjectBridgeToMap
  (as-map [item]
    "Return a Map implementation using __getitem__, __setitem__.  Note that it may be
incomplete especially if the object has no 'keys' attribute."))


(defprotocol PPyObjectBridgeToList
  (as-list [item]
    "Return a List implementation using __getitem__, __setitem__."))


(defprotocol PJvmToNumpyBridge
  (as-numpy [item options]
    "Never copy data, operation returns nil of would require copy."))


(defprotocol PJvmToNumpy
  (->numpy [item options]
    "Never return nil.  Copy or otherwise, numpy or bust."))


(defprotocol PPyObjectBridgeToTensor
  (as-tensor [item]
    "Return a tech.v2.tensor object from the item that shares the data backing store."))


(defmulti pyobject->jvm
  (fn [pyobj]
    (python-type pyobj)))


(defmulti pyobject-as-jvm
  (fn [pyobj]
    (python-type pyobj)))

(defmulti python-obj-iterator
  "Given a python object, produce an iterator.  Python is fairly confused about
  what an iterator is or does, so some things require iteritems and some things
  require calling __iter__."
  (fn [pyobj interpreter]
    (python-type pyobj)))
