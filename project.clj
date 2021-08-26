(defproject xyz.thoren/luminary "0.7.0-SNAPSHOT"
  :description "Calculate dates based on the Bible and the 1st Book of Enoch."
  :url "https://github.com/johanthoren/luminary"
  :license {:name "LGPL-3.0"
            :url "https://choosealicense.com/licenses/lgpl-3.0"
            :comment "GNU Lesser General Public License v3.0"
            :year 2021
            :key "lgpl-3.0"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [tick "0.5.0-RC1"]
                 [xyz.thoren/equinox "1.0.0"]
                 [org.shredzone.commons/commons-suncalc "3.5"]]
  :repositories [["releases" {:url "https://repo.clojars.org"
                              :creds :gpg}]
                 ["org.shredzone.commons/commons-suncalc"
                  "https://search.maven.org/artifact/"]]
  :repl-options {:init-ns xyz.thoren.luminary}
  :release-tasks [["test"]
                  ["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
