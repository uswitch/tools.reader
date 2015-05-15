(ns repl
  (:require
   [cljs.repl]
   [cljs.closure]
   [cljs.repl.node]
   [cemerick.piggieback]))

;; (cljs.closure/build "dev" {:main 'repl :output-to "out/main.js" :verbose true})

;; (cemerick.piggieback/cljs-repl :repl-env (cljs.repl.node/repl-env)
;;                                :watch "dev"
;;                                :output-dir "out")

(cljs.repl/repl (cljs.repl.node/repl-env)
                ;; :watch "dev"
                ;; :output-dir "out"
                )
