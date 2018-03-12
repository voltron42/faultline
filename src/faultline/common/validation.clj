(ns faultline.commmon.validation)

(defn min-count [bound]
  (fn [value]
    (try
      (<= bound (count value))
      (catch Throwable t
        false))))

(defn matches? [regex]
  (fn [value]
    (let [matches (re-matches regex value)
          matches (if (coll? matches) matches [matches])]
      (contains? (set matches) value))))

(defn named-as
  ([ns-regex name-regex]
   (fn [value]
     (try
       (and ((matches? ns-regex) (namespace value)) ((matches? name-regex) (name value)))
       (catch Throwable t
         false))))
  ([name-regex]
   (fn [value]
     (try
       (and (nil? (namespace value)) ((matches? name-regex) (name value)))
       (catch Throwable t
         false)))))

(defn one-of [my-list]
  (fn [value]
    (contains? (set my-list) value)))

(defn match-row-lengths []
  (fn [value]
    (if-not (coll? value)
      (do
        (println "not collection")
        (println (type value))
        (println value)
        false)
      (let [[_ & rows] value]
        (println "collection")
        (println (type rows))
        (println rows)
        (and
          (every? coll? rows)
          (= 1 (count (set (map count rows)))))))))
