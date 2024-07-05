(ns clirc.probl4.probl4 
  (:require [clojure.string :as string]))


(defn code2rep [code]
  (let [inputs (atom [])
        workspace (atom [])
        outputs (atom [])
        parse (fn [line]
                (let [foo (.substring line 0 (.indexOf line "="))
                      bar (.substring line (+ (.indexOf line "(") 1) (.indexOf line ","))
                      blah (.substring line (+ (.indexOf line ",") 1) (.indexOf line ")"))]
                  [(string/trim foo)
                   (string/trim bar)
                   (string/trim blah)]))
        addvar (fn [var]
                 (cond
                   (= (first var) \X) (when-not (some #(= % var) @inputs)
                                        (swap! inputs conj var))
                   (= (first var) \Y) (when-not (some #(= % var) @outputs)
                                        (swap! outputs conj var))
                   :else (when-not (some #(= % var) @workspace)
                           (swap! workspace conj var))))
        variables (atom [])]
    (doseq [line (string/split-lines code)]
      (doseq [var (parse line)]
        (addvar var)))

    (reset! variables (concat (sort @inputs) @workspace (sort @outputs)))
    (println @variables)

    (let [L (atom [])]
      (doseq [line (string/split-lines code)]
        (let [[foo bar blah] (parse line)]
          (swap! L conj [(.indexOf @variables foo)
                         (.indexOf @variables bar)
                         (.indexOf @variables blah)])))
      [(count @inputs) (count @outputs) @L])))

