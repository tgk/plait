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
  (let [new-bindings-safe (rename-underscore-bindings new-bindings)
        base-bindings-s (set (map first (partition 2 base-bindings)))
        new-binding-pairs (partition 2 new-bindings-safe)
        new-bindings-m (into {} (for [[k v] new-binding-pairs]
                                  [k v]))]
    (vec (apply concat
                (apply concat
                       (for [[n b] (partition 2 base-bindings)]
                         [n (get new-bindings-m n b)]))
                (remove (comp base-bindings-s first) new-binding-pairs)))))

;; todo
(def metas (atom []))

(defn meta-available? [x]
  (instance? clojure.lang.IMeta x))

(defn my-prewalk
  {:added "1.1"}
  [f form]
  (prn "prewalk start")
  (binding [*print-meta* true] (prn form))
  (when (meta-available? form)
    (reset! metas (conj @metas (meta form))))

  (walk/walk
   (partial my-prewalk f)
   (fn [x]
     (let [tmp-result
           (if (meta-available? x)
             (let [m      (peek @metas)
                   result (with-meta x m)]
               (reset! metas (pop @metas))
               result)
             x)]
       (prn "outer")
       (binding [*print-meta* true] (prn tmp-result))
       tmp-result))
   (f form)))

(defn plait-impl-walk
  [bindings form]
  (my-prewalk
   (fn [x]
     (if (and (seqable? x) (= 'plait (first x)))
       (let [new-bindings (merge-bindings bindings (second x))]
         (concat ['let new-bindings]
                 (map (partial plait-impl-walk new-bindings) (drop 2 x))))
       x))
   form))

(defn plait-impl-recur
  [bindings x]
  (if (and (seqable? x) (= 'plait (first x)))
       (let [new-bindings (merge-bindings bindings (second x))]
         (concat ['let new-bindings]
                 (map (partial plait-impl-recur new-bindings) (drop 2 x))))
       x))

(defmacro plait
  "Add `let` style bindings that can be redeclared at deeper levels."
  {:style/indent 1}
  [bindings & body]
  ;; This makes CIDER highlight the failing test
  ;; (plait-impl-recur [] (concat ['plait bindings] body))

  ;; This still highlights the top level form
  (plait-impl-walk [] (concat ['plait bindings] body))
  )

(comment
  (def test-form
  '(plait [a 42
           b (inc a)]
     (plait [a 10]
       (testing "replaces first binding")
       (is (= b 12)))))

  (plait-impl-recur [] test-form)
  ;; => (let
  ;;     [a 42 b (inc a)]
  ;;     (let
  ;;      [a 10 b (inc a)]
  ;;      (testing "replaces first binding")
  ;;      (is (= b 12))))

  (plait-impl-walk [] test-form)
  ;; => (let
  ;;     [a 42 b (inc a)]
  ;;     (let
  ;;      [a 10 b (inc a)]
  ;;      (testing "replaces first binding")
  ;;      (is (= b 12))))
  )
