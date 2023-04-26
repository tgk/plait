(ns plait.core
  (:require [clojure.walk :as w]
            [clojure.walk :as walk]))

(defn- safe-binding-name
  [s]
  (if (= (symbol "_") s)
    (gensym)
    s))

(defn- rename-underscore-bindings
  [bindings]
  (apply concat
         (for [[k v] (partition 2 bindings)]
           [(safe-binding-name k) v])))

(defn merge-bindings
  [base-bindings new-bindings]
  (let [base-bindings-s (set (map first (partition 2 base-bindings)))
        new-binding-pairs (partition 2 new-bindings)
        new-bindings-m (into {} (for [[k v] new-binding-pairs]
                                  [k v]))]
    (vec (apply concat
                (apply concat
                       (for [[n b] (partition 2 base-bindings)]
                         [n (get new-bindings-m n b)]))
                (remove (comp base-bindings-s first) new-binding-pairs)))))

(defn plait-impl
  [bindings form]
  (walk/prewalk
   (fn [x]
     (if (and (seqable? x) (= 'plait (first x)))
       (let [child-bindings (rename-underscore-bindings (second x))
             child-body     (drop 2 x)
             new-bindings   (merge-bindings bindings child-bindings)]
         (concat ['let new-bindings]
                 (map (partial plait-impl new-bindings) child-body)))


       x))
   form))

(defmacro plait
  "Add `let` style bindings that can be redeclared at deeper levels."
  {:style/indent 1}
  [bindings & body]
  (plait-impl [] (concat ['plait bindings] body)))
