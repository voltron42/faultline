(ns faultline.commmon.validation)

(defn match-row-lengths []
  (fn [value]
    (when (coll? value)
      (let [[_ & rows] value]
        (and
          (every? coll? rows)
          (= 1 (count (set (map count rows)))))))))
