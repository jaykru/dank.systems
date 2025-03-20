(defproject dank.systems "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://dank.systems"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [stasis "2.5.0"]
                 [markdown-clj "1.10.5"]
                 [hiccup "1.0.5"]
                 [ring "1.8.2"]
                 [optimus "2025.01.19.2"]
                 [optimus-img-transform "0.3.1"]
                 [com.github.flow-storm/flow-storm-dbg "4.2.1"]
                 [org.clojure/core.async "1.7.701"]]
  :ring {:handler systems.dank.core/server}
  :profiles {:dev {:plugins [[lein-ring "0.12.5"]]}}
  :repl-options {:init-ns systems.dank.core}
  :aliases {"build-site" ["run" "-m" "systems.dank.core/export!"]}
  :resource-paths ["resources"])
