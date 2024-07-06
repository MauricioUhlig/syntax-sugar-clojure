(ns clirc.probl3.proc
    (:require [clojure.core.match :refer [match]]))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; CLIRC-PROC

(declare expand-proc-return
         expand-proc-input-args
         expand-proc-args
         expand-proc-funcs
         expand-proc-body)

(defn expand-proc-tree 
  [proc]
  (match [proc]
    [(['proc funcname args & body] :seq)]
    (let [in-args (expand-proc-input-args args)]
      {:name funcname
       :in in-args
       :body-sttmts (expand-proc-body body in-args)
       :return (expand-proc-return body in-args)})
    :else nil)
  )

(defn expand-proc-return
  [body input-args]
  (letfn [(aux [acc sttmt]
               (match [sttmt]
                 [(['return func] :seq)]
                 (expand-proc-funcs nil func input-args)
                 :else acc))]
    (reduce aux nil body)))

(defn expand-proc-body
  [body input-args]
  (letfn [(aux [acc sttmt]
               (match [sttmt]
                 [(['set! var func] :seq)] 
                   (into [] (concat acc [(expand-proc-funcs var func input-args)]))
                 :else acc)
                 )]
    (reduce aux [] body)))

(defn expand-proc-input-args [args] (map #(keyword %) args))
(defn expand-proc-funcs [var func input-args] {:var var :func (first func) :args (expand-proc-args (rest func) input-args)})

(defn expand-proc-args 
  [func-args input-args] 
  (letfn [(aux [acc func-arg] 
               (cond (some #{(keyword func-arg)} input-args )
                     (concat acc [(keyword func-arg)])
                     :else (concat acc (list func-arg))))]
    (reduce aux [] func-args)))


(defn expand-proc 
  [prog]
  (let [tree (letfn [(aux [acc proc]
                          (let [proc-tree (expand-proc-tree proc)] 
                            (cond (nil? proc-tree) acc
                                  :else (concat acc [proc-tree]))))]
               (reduce aux [] prog))]
    tree))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def counter (atom -1))
(defn count-inc [] (swap! counter inc))

(declare bind-in-args
         handle-proc
         handle-proc-get-func-tree
         handle-proc-func-tree
         handle-proc-func-tree-body-sttmts
         handle-proc-func-tree-return
         bind-func-args
         create-sttmt)

(defn handle-proc
  [prog proc-tree]
  (letfn [(aux [acc sttmt]
            (match [sttmt]
              [(['set! var func] :seq)]
              (let [func-tree (handle-proc-get-func-tree (first func) proc-tree)] 
                (count-inc)
                (cond (nil? func-tree) acc
                      :else (into [] (concat acc (handle-proc-func-tree var func func-tree)))))
              :else nil))]
    (reduce aux [] prog)))

(defn handle-proc-get-func-tree
  [func-name proc-tree]
  (loop [tree proc-tree]
        (cond (empty? tree) nil 
          (= (:name (first tree)) func-name) (first tree)
              :else (recur (rest tree)))))

(defn bind-in-args
  [func-name in-args proc-args]
  (println func-name in-args proc-args)
  (cond (not= (count in-args) (count proc-args))
        (throw (ex-info "Wrong number of arguments." {:fname func-name :in-args in-args :proc-args proc-args}))
        (empty? in-args) nil
        :else (loop [acc {} in-arg' in-args proc-args' proc-args]
                (cond (empty? in-arg') acc
                      :else (recur (assoc-in acc [(first proc-args')] (first in-arg')) (rest in-arg') (rest proc-args'))))))

(defn create-var-name 
  [func var]
  (str func @counter "$" var))

(defn handle-proc-func-tree
  [var func tree] 
  (let [func-args  (rest func)
        binded-args (bind-in-args (first func) func-args (:in tree))
        body-sttmt (handle-proc-func-tree-body-sttmts binded-args tree)
        return (handle-proc-func-tree-return var tree binded-args)]
    (concat body-sttmt return))  )

(defn handle-proc-func-tree-return
  [var tree binded-args]
  `((set! ~var (~(get-in tree [:return :func]) ~@(bind-func-args (:name tree) binded-args (get-in tree [:return :args])))))
  )

(defn handle-proc-func-tree-body-sttmts
  [binded-args tree]
  (letfn [(aux [acc body-sttmt]
               (concat acc (create-sttmt (:name tree) (:var body-sttmt) (:args body-sttmt) binded-args)))]
    (reduce aux [] (:body-sttmts tree))))

(defn create-sttmt 
  [func var func-args binded-args]
  `((set! ~(create-var-name func var) (~func ~@(bind-func-args func binded-args func-args)))))

(defn bind-func-args 
  [func bind-in-args func-args]
  (letfn [(aux [acc func-arg]
               (concat acc [(cond (keyword? func-arg) (get-in bind-in-args [func-arg])
                                  :else (create-var-name func func-arg))]))]
    (reduce aux [] func-args)))
  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def proc '[(proc xor [a b]
                 (set! or1 (or a b))
                 (set! and1 (and a b))
                 (set! nand1 (not and1))
                 (return (and or1 nand1)))
            (set! (:out 0) (xor (:in 0), (:in 1)))])

(def tree (expand-proc proc))
(println (handle-proc proc tree))