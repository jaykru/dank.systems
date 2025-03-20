((clojure-mode . ((cider-jack-in-nrepl-middlewares . ("flow-storm.nrepl.middleware/wrap-flow-storm"
													  ("refactor-nrepl.middleware/wrap-refactor" :predicate cljr--inject-middleware-p)
													  "cider.nrepl/cider-middleware")))))
