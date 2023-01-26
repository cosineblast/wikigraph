(defproject wiki-graph "0.1.0-SNAPSHOT"
  :description "FIXME: write description"

  :url "http://example.com/FIXME"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.jsoup/jsoup "1.15.3"]
                 [org.clojure/core.async "1.6.673"]
                 [http-kit "2.3.0"]
                 [ring "1.9.6"]
                 [compojure "1.7.0"]
                 [org.clojure/data.json "2.4.0"]
                 [com.taoensso/carmine "3.2.0"]]


  :main ^:skip-aot wiki-graph.core

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}

  :jvm-opts ["-Djdk.attach.allowAttachSelf"])
