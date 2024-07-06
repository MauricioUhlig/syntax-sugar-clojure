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
(defn expand-proc-funcs [var func input-args] {:var (first (expand-proc-args [var] input-args)) :func (first func) :args (expand-proc-args (rest func) input-args)})

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
(def counter (atom 0))
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
  ([prog]
   (handle-proc prog (expand-proc prog)))
  ([prog proc-tree]
   ;;(println proc-tree)
   (loop [sttmts prog]
     (let [exec (letfn [(aux [acc sttmt]
                          (match [sttmt]
                            [(['set! var func] :seq)]
                            (let [func-tree (handle-proc-get-func-tree (first func) proc-tree)]
                              (cond (nil? func-tree) {:sttmts (into [] (concat (:sttmts acc) [sttmt])) :has-proc? (:has-proc? acc)}
                                    :else (do
                                            (count-inc) ;; só incrementa quando existe um procedimento para abrir na arvore
                                            {:sttmts (into [] (concat (:sttmts acc) (handle-proc-func-tree var func func-tree)))
                                             :has-proc? true})))
                            :else nil))]
                  (reduce aux {:sttmts [] :has-proc? false} sttmts))]
       (cond (true? (:has-proc? exec)) (recur (:sttmts exec))
             :else (:sttmts exec))))))

(defn handle-proc-get-func-tree
  [func-name proc-tree]
  (loop [tree proc-tree]
        (cond (empty? tree) nil 
          (= (:name (first tree)) func-name) (first tree)
              :else (recur (rest tree)))))

(defn bind-in-args
  [func-name in-args proc-args]
  ;;(println "bind-in-args" func-name in-args proc-args)
  (cond (not= (count in-args) (count proc-args))
        (throw (ex-info "Wrong number of arguments." {:fname func-name :in-args in-args :proc-args proc-args}))
        (empty? in-args) nil
        :else (loop [acc {} in-arg' in-args proc-args' proc-args]
                (cond (empty? in-arg') acc
                      :else (recur (assoc-in acc [(first proc-args')] (first in-arg')) (rest in-arg') (rest proc-args'))))))

(defn create-var-name 
  [func var]
  (symbol (str func @counter "$" var)))

(defn handle-proc-func-tree
  [var func tree] 
  (let [func-args  (rest func)
        binded-args (bind-in-args (first func) func-args (:in tree))
        body-sttmt (handle-proc-func-tree-body-sttmts binded-args tree)
        return (handle-proc-func-tree-return var tree binded-args)]
    ;;(println "binded-args" (:name tree) binded-args)
    (concat body-sttmt return))  )

(defn handle-proc-func-tree-return
  [var tree binded-args]
  `((set! ~var (~(get-in tree [:return :func]) ~@(bind-func-args (:name tree) binded-args (get-in tree [:return :args])))))
  )

(defn handle-proc-func-tree-body-sttmts
  [binded-args tree]
  (letfn [(aux [acc body-sttmt]
               (concat acc (create-sttmt (:name tree) (:func body-sttmt) (:var body-sttmt) (:args body-sttmt) binded-args)))]
    (reduce aux [] (:body-sttmts tree))))

(defn create-sttmt 
  [func func-name var func-args binded-args]
  `((set! ~@(bind-func-args func binded-args [var]) (~func-name ~@(bind-func-args func binded-args func-args)))))

(defn bind-func-args 
  [func bind-in-args func-args]
  ;;(println func bind-in-args func-args)
  (letfn [(aux [acc func-arg]
               (concat acc [(cond (keyword? func-arg) (get-in bind-in-args [func-arg])
                                  (list? func-arg) func-arg
                                  :else (create-var-name func func-arg))]))]
    (reduce aux [] func-args)))
  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def proc '[(proc full-adder [a b cin cout]
                  (set! xor1 (xor a b))
                  (set! and1 (and xor1 cin))
                  (set! and2 (and a b))
                  (set! cout (or and1 and2)) 
                  (return (xor xor1 cin)))
            (proc xor [a b]
                 (set! or1 (or a b))
                 (set! and1 (and a b))
                 (set! nand1 (not and1))
                 (return (and or1 nand1)))
            
            (set! soma (full-adder (:in 0) (:in 1) (:out 0) (:out 1)))])

;;(def tree (expand-proc proc))
;;(println (handle-proc proc tree))