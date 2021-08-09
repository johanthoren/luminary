(ns xyz.thoren.luminary-test
  (:require [clojure.test :refer [deftest is testing]]
            [xyz.thoren.luminary :as l]
            [java-time :as t]))

(def arvidsjaur-latitude 65.5927979)
(def arvidsjaur-longitude 19.1622545)
(def anchorage-latitude 61.1077053)
(def anchorage-longitude -150.0006618)
(def puerto-williams-latitude -54.9362304)
(def puerto-williams-longitude -67.6132659)
(def longyearbyen-latitude 78.2253587)
(def longyearbyen-longitude 15.4878901)
(def mcmurdo-latitude -77.8401191)
(def mcmurdo-longitude 166.6445299)
(def tm-lat 31.7781161)
(def tm-lon 35.233804)

(defn- time-of-sunset
  [latitude longitude year month day]
  (#'xyz.thoren.luminary/next-start-of-day
   latitude longitude (t/with-zone
                        (t/zoned-date-time year month day 12 0)
                        "UTC")))

(deftest test-sunset-drift
  (testing "that the same minute is always reported for the sunset"
    (testing "in Sweden"
      (let [r #(str (:sunset (#'xyz.thoren.luminary/next-sunset
                              58 12 (#'xyz.thoren.luminary/make-zoned-date
                                     "Europe/Stockholm" 2021 %1 %2 %3 0))))
            t #(is (= %4 (r %1 %2 %3)))]
        (testing "on June 6"
          (doseq [h (range 0 22)]
            (t 6 6 h "2021-06-06T22:08+02:00[Europe/Stockholm]")))
        (testing "on August 8"
          (doseq [h (range 0 22)]
            (t 8 5 h "2021-08-05T21:20+02:00[Europe/Stockholm]")))
        (testing "on December 20"
          (doseq [h (range 0 16)]
            (t 12 20 h "2021-12-20T15:23+01:00[Europe/Stockholm]")))))
    (testing "in Anchorage"
      (let [r #(str (:sunset (#'xyz.thoren.luminary/next-sunset
                              anchorage-latitude anchorage-longitude
                              (#'xyz.thoren.luminary/make-zoned-date
                               "America/Anchorage" 2021 %1 %2 %3 0))))
            t #(is (= %4 (r %1 %2 %3)))]
        (testing "on May 10"
          (doseq [h (range 0 23)]
            (t 5 10 h "2021-05-10T22:29-08:00[America/Anchorage]")))
        (testing "on June 6"
          (doseq [h (range 0 24)]
            (t 6 6 h "2021-06-06T23:29-08:00[America/Anchorage]")))
        (testing "on December 20"
          (doseq [h (range 0 16)]
            (t 12 20 h "2021-12-20T15:43-09:00[America/Anchorage]")))))

    (testing "in Puerto-Williams"
      (let [r #(str (:sunset (#'xyz.thoren.luminary/next-sunset
                              puerto-williams-latitude puerto-williams-longitude
                              (#'xyz.thoren.luminary/make-zoned-date
                               "Antarctica/Palmer" 2021 %1 %2 %3 0))))
            t #(is (= %4 (r %1 %2 %3)))]
        (testing "on May 10"
          (doseq [h (range 0 18)]
            (t 5 10 h "2021-05-10T17:44-03:00[Antarctica/Palmer]")))
        (testing "on June 6"
          (doseq [h (range 0 18)]
            (t 6 6 h "2021-06-06T17:10-03:00[Antarctica/Palmer]")))
        (testing "on December 20"
          (doseq [h (range 0 23)]
            (t 12 20 h "2021-12-20T22:09-03:00[Antarctica/Palmer]")))))))

(deftest temple-mount
  (testing "sunrise and sunset at the Temple Mount"
    (let [lat tm-lat
          lon tm-lon
          r #(%1 lat lon %2 %3 %4)
          test-string #(is (= %5 (str (r %1 %2 %3 %4))))]
      (test-string #'time-of-sunset 2021 1 1 "2021-01-01T14:46Z[UTC]")
      (test-string #'time-of-sunset 2021 4 30 "2021-04-30T16:18Z[UTC]")
      (test-string #'time-of-sunset 2021 6 22 "2021-06-22T16:48Z[UTC]")
      (test-string #'time-of-sunset 2021 7 1 "2021-07-01T16:48Z[UTC]")
      (test-string #'time-of-sunset 2021 11 11 "2021-11-11T14:42Z[UTC]")
      (test-string #'time-of-sunset 2021 12 21 "2021-12-21T14:39Z[UTC]"))))

(deftest arvidsjaur
  (testing "sunrise and sunset in Arvidsjaur, Sweden"
    (let [lat arvidsjaur-latitude
          lon arvidsjaur-longitude
          r #(%1 lat lon %2 %3 %4)
          test-string #(is (= %5 (str (r %1 %2 %3 %4))))]
      (test-string #'time-of-sunset 2021 1 1 "2021-01-01T12:33Z[UTC]")
      (test-string #'time-of-sunset 2021 4 30 "2021-04-30T19:16Z[UTC]")
      (test-string #'time-of-sunset 2021 6 22 "2021-06-22T22:19Z[UTC]")
      (test-string #'time-of-sunset 2021 7 1 "2021-07-01T21:56Z[UTC]")
      (test-string #'time-of-sunset 2021 11 11 "2021-11-11T13:42Z[UTC]")
      (test-string #'time-of-sunset 2021 12 21 "2021-12-21T12:16Z[UTC]"))))

(deftest longyearbyen
  (testing "sunrise and sunset in Longyearbyen, Svalbard"
    (let [lat longyearbyen-latitude
          lon longyearbyen-longitude
          r #(%1 lat lon %2 %3 %4)
          test-string #(is (= %5 (str (r %1 %2 %3 %4))))]
      ;; The dates where one would expect no sunset are automatically adjusted
      ;; to be calculated at the latitude 65.7.
      (test-string #'time-of-sunset 2021 1 1 "2021-01-01T12:45Z[UTC]")
      (test-string #'time-of-sunset 2021 3 1 "2021-03-01T14:58Z[UTC]")
      (test-string #'time-of-sunset 2021 4 30 "2021-04-30T19:32Z[UTC]")
      (test-string #'time-of-sunset 2021 6 22 "2021-06-22T22:46Z[UTC]")
      (test-string #'time-of-sunset 2021 7 1 "2021-07-01T22:16Z[UTC]")
      (test-string #'time-of-sunset 2021 10 10 "2021-10-10T14:43Z[UTC]")
      (test-string #'time-of-sunset 2021 11 11 "2021-11-11T13:55Z[UTC]")
      (test-string #'time-of-sunset 2021 12 21 "2021-12-21T12:28Z[UTC]"))))

(deftest puerto-williams
  (testing "sunrise and sunset in Puerto Williams, Chile"
    (let [lat puerto-williams-latitude
          lon puerto-williams-longitude
          r #(%1 lat lon %2 %3 %4)
          test-string #(is (= %5 (str (r %1 %2 %3 %4))))]
      (test-string #'time-of-sunset 2021 1 1 "2021-01-02T01:10Z[UTC]")
      (test-string #'time-of-sunset 2021 4 30 "2021-04-30T21:04Z[UTC]")
      (test-string #'time-of-sunset 2021 6 22 "2021-06-22T20:08Z[UTC]")
      (test-string #'time-of-sunset 2021 7 1 "2021-07-01T20:13Z[UTC]")
      (test-string #'time-of-sunset 2021 11 11 "2021-11-12T00:09Z[UTC]")
      (test-string #'time-of-sunset 2021 12 21 "2021-12-22T01:09Z[UTC]"))))

(deftest mcmurdo
  (testing "sunrise and sunset at McMurdo station, Antarctica"
    (let [lat mcmurdo-latitude
          lon mcmurdo-longitude
          r #(%1 lat lon %2 %3 %4)
          test-string #(is (= %5 (str (r %1 %2 %3 %4))))]
      ;; The dates where one would expect no sunset are automatically adjusted
      ;; to be calculated at the latitude -65.7.
      (test-string #'time-of-sunset 2021 1 1 "2021-01-01T12:07Z[UTC]")
      (test-string #'time-of-sunset 2021 3 1 "2021-03-02T09:46Z[UTC]")
      (test-string #'time-of-sunset 2021 4 30 "2021-05-01T04:33Z[UTC]")
      (test-string #'time-of-sunset 2021 6 22 "2021-06-23T02:27Z[UTC]")
      (test-string #'time-of-sunset 2021 7 1 "2021-07-02T02:39Z[UTC]")
      (test-string #'time-of-sunset 2021 10 10 "2021-10-11T09:23Z[UTC]")
      (test-string #'time-of-sunset 2021 11 11 "2021-11-12T09:51Z[UTC]")
      (test-string #'time-of-sunset 2021 12 21 "2021-12-21T12:42Z[UTC]"))))

(deftest new-moons
  (testing "time of new moons 2021"
    (let [t #(is (= %2 (str (#'xyz.thoren.luminary/next-new-moon
                             (#'xyz.thoren.luminary/make-utc-date 2021 %1 1)))))]
      (t 1 "2021-01-13T05:00Z[UTC]")
      (t 2 "2021-02-11T19:07Z[UTC]")
      (t 3 "2021-03-13T10:24Z[UTC]")
      (t 4 "2021-04-12T02:31Z[UTC]")
      (t 5 "2021-05-11T19:00Z[UTC]")
      (t 6 "2021-06-10T10:56Z[UTC]")
      (t 7 "2021-07-10T01:22Z[UTC]")
      (t 8 "2021-08-08T13:53Z[UTC]")
      (t 9 "2021-09-07T00:54Z[UTC]")
      (t 10 "2021-10-06T11:10Z[UTC]")
      (t 11 "2021-11-04T21:20Z[UTC]")
      (t 12 "2021-12-04T07:46Z[UTC]"))))

(deftest next-new-year-in-israel
  (testing "the time of the previous new year in Israel"
    (let [t #(is (= %2
                    (str (#'xyz.thoren.luminary/next-start-of-year-in-israel
                          (#'xyz.thoren.luminary/make-zoned-date "Asia/Jerusalem"
                                        %1 (+ 5 (rand-int 8)) 1 1)))))]
      (t 2018 "2019-04-05T19:01+03:00[Asia/Jerusalem]")
      (t 2019 "2020-03-24T17:53+02:00[Asia/Jerusalem]")
      (t 2020 "2021-04-12T19:06+03:00[Asia/Jerusalem]")
      (t 2021 "2022-04-01T18:58+03:00[Asia/Jerusalem]")
      (t 2022 "2023-03-22T17:51+02:00[Asia/Jerusalem]")
      (t 2023 "2024-04-09T19:04+03:00[Asia/Jerusalem]")
      (t 2024 "2025-03-29T18:56+03:00[Asia/Jerusalem]")
      (t 2025 "2026-04-17T19:09+03:00[Asia/Jerusalem]")
      (t 2026 "2027-04-07T19:02+03:00[Asia/Jerusalem]")
      (t 2027 "2028-03-26T18:54+03:00[Asia/Jerusalem]"))))

(deftest previous-new-year-in-israel
  (testing "the time of the previous new year in Israel"
    (let [t #(is (= %2
                    (str (#'xyz.thoren.luminary/previous-start-of-year-in-israel
                          (#'xyz.thoren.luminary/make-zoned-date "Asia/Jerusalem"
                                        %1 (+ 5 (rand-int 8)) 22 12)))))]
      (t 2019 "2019-04-05T19:01+03:00[Asia/Jerusalem]")
      (t 2020 "2020-03-24T17:53+02:00[Asia/Jerusalem]")
      (t 2021 "2021-04-12T19:06+03:00[Asia/Jerusalem]")
      (t 2022 "2022-04-01T18:58+03:00[Asia/Jerusalem]")
      (t 2023 "2023-03-22T17:51+02:00[Asia/Jerusalem]")
      (t 2024 "2024-04-09T19:04+03:00[Asia/Jerusalem]")
      (t 2025 "2025-03-29T18:56+03:00[Asia/Jerusalem]")
      (t 2026 "2026-04-17T19:09+03:00[Asia/Jerusalem]")
      (t 2027 "2027-04-07T19:02+03:00[Asia/Jerusalem]")
      (t 2028 "2028-03-26T18:54+03:00[Asia/Jerusalem]"))))

(deftest temple-mount-previous-start-of-month
  (testing "that the correct start of month is calculated"
    (let [lat tm-lat
          lon tm-lon
          t #(is (= %6
                    (str (#'xyz.thoren.luminary/previous-start-of-month
                          lat lon (t/with-zone
                                    (t/zoned-date-time %1 %2 %3 %4 %5 0 0 0)
                                    "Asia/Jerusalem")))))]
      (t 2021 7 10 1 20 "2021-06-10T19:44+03:00[Asia/Jerusalem]")
      (t 2021 7 10 19 50 "2021-07-10T19:47+03:00[Asia/Jerusalem]")
      (t 2021 12 4 6 35 "2021-11-05T16:46+02:00[Asia/Jerusalem]")
      (t 2021 12 4 7 47 "2021-11-05T16:46+02:00[Asia/Jerusalem]"))))

(deftest temple-mount-next-start-of-year
  (testing "that the correct start of month is calculated"
    (let [t #(is (= %6
                    (str (#'xyz.thoren.luminary/next-start-of-year
                          tm-lat tm-lon
                          (#'xyz.thoren.luminary/make-zoned-date
                           "Asia/Jerusalem" %1 %2 %3 %4 %5)))))]
      (t 2021 3 20 12 0 "2021-04-12T19:06+03:00[Asia/Jerusalem]"))))

(deftest arvidsjaur-previous-start-of-month
  (testing "that the correct start of month is calculated"
    (let [lat arvidsjaur-latitude
          lon arvidsjaur-longitude
          t #(is (= %6
                    (str (#'xyz.thoren.luminary/previous-start-of-month
                          lat lon (t/with-zone
                                    (t/zoned-date-time %1 %2 %3 %4 %5 0 0 0)
                                    "Europe/Stockholm")))))]
      (t 2021 7 10 1 20 "2021-06-10T23:54+02:00[Europe/Stockholm]")
      (t 2021 7 10 19 50 "2021-06-10T23:54+02:00[Europe/Stockholm]")
      (t 2021 12 4 6 35 "2021-11-05T15:02+01:00[Europe/Stockholm]")
      (t 2021 12 4 7 47 "2021-11-05T15:02+01:00[Europe/Stockholm]")
      (t 2021 12 4 23 0 "2021-12-04T13:33+01:00[Europe/Stockholm]"))))

(deftest puerto-williams-previous-start-of-month
  (testing "that the correct start of month is calculated"
    (let [lat puerto-williams-latitude
          lon puerto-williams-longitude
          t #(is (= %6
                    (str (#'xyz.thoren.luminary/previous-start-of-month
                          lat lon (#'xyz.thoren.luminary/make-zoned-date
                                    "Antarctica/Palmer" %1 %2 %3 %4 %5 0 0 0)))))]
      (t 2021 7 10 1 20 "2021-06-10T17:08-03:00[Antarctica/Palmer]")
      (t 2021 7 10 19 50 "2021-07-10T17:22-03:00[Antarctica/Palmer]")
      (t 2021 12 4 6 35 "2021-11-05T20:57-03:00[Antarctica/Palmer]")
      (t 2021 12 4 7 47 "2021-11-05T20:57-03:00[Antarctica/Palmer]")
      (t 2021 12 4 23 0 "2021-12-04T21:52-03:00[Antarctica/Palmer]"))))

(deftest anchorage-previous-start-of-month
  (testing "that the correct start of month is calculated"
    (let [lat anchorage-latitude
          lon anchorage-longitude
          t #(is (= %6
                    (str (#'xyz.thoren.luminary/previous-start-of-month
                          lat lon (#'xyz.thoren.luminary/make-zoned-date
                                    "America/Anchorage" %1 %2 %3 %4 %5 0 0 0)))))]
      (t 2021 7 10 1 20 "2021-06-10T23:34-08:00[America/Anchorage]")
      (t 2021 7 10 23 50 "2021-07-10T23:26-08:00[America/Anchorage]")
      (t 2021 12 4 6 35 "2021-11-05T17:46-08:00[America/Anchorage]")
      (t 2021 12 4 7 47 "2021-11-05T17:46-08:00[America/Anchorage]")
      (t 2021 12 4 23 0 "2021-12-04T15:48-09:00[America/Anchorage]"))))

(deftest days-in-month-at-temple-mount
  (testing "the properties of the list of days of a given month"
    (let [t #(#'xyz.thoren.luminary/start-of-days-in-month
              tm-lat tm-lon
              (apply #'xyz.thoren.luminary/make-utc-date %&))]
      (doseq [y (range 2021 2023)
              m (range 1 13)]
        (let [r (t y m 1 12 0)]
          (is (apply distinct? (map #(t/as % :month-of-year :day-of-month) r)))))
      (is (some #(= "2021-10-24T14:57Z[UTC]" %)
                (map str (#'xyz.thoren.luminary/start-of-days-in-month
                          tm-lat tm-lon
                          (#'xyz.thoren.luminary/make-utc-date 2021 10 27 23)))))
      (is (some #(= "2021-11-03T14:48Z[UTC]" %)
                (map str (#'xyz.thoren.luminary/start-of-days-in-month
                          tm-lat tm-lon
                          (#'xyz.thoren.luminary/make-utc-date 2021 10 27 23))))))))

(deftest next-start-of-week-at-temple-mount
  (testing "that the correct start of week is calculated"
    (let [r #(#'xyz.thoren.luminary/next-start-of-week
              tm-lat tm-lon
              (apply #'xyz.thoren.luminary/make-zoned-date "Asia/Jerusalem" %&))
          t #(is (= %6 (str (r %1 %2 %3 %4 %5))))]
      (t 2021 4 5 16 4 "2021-04-10T19:04+03:00[Asia/Jerusalem]"))))

(deftest compare-next-start-of
  (let [date (#'xyz.thoren.luminary/make-zoned-date "Asia/Jerusalem" 2021 7 10 19 50)]
    (testing "that the next start of year is the same regardless of function"
      (is (= (#'xyz.thoren.luminary/next-start-of-year-in-israel date)
             (#'xyz.thoren.luminary/next-start-of-year tm-lat tm-lon date))))
    (testing "that the next start of month is the same regardless of function"
      (is (= (#'xyz.thoren.luminary/next-start-of-month-in-israel date)
             (#'xyz.thoren.luminary/next-start-of-month tm-lat tm-lon date))))))

(deftest compare-previous-start-of
  (let [date (#'xyz.thoren.luminary/make-zoned-date "Asia/Jerusalem" 2021 7 10 19 50)]
    (testing "that the previous start of year is the same regardless of function"
      (is (= (#'xyz.thoren.luminary/previous-start-of-year-in-israel date)
             (#'xyz.thoren.luminary/previous-start-of-year tm-lat tm-lon date))))
    (testing "that the previous start of month is the same regardless of function"
      (is (= (#'xyz.thoren.luminary/previous-start-of-month-in-israel date)
             (#'xyz.thoren.luminary/previous-start-of-month tm-lat tm-lon date))))))

(deftest months-of-year-2020
  (testing "that the correct amount of months are calculated for 2020"
    (is (= 13 (count (#'xyz.thoren.luminary/start-of-months-in-year
                      tm-lat tm-lon (#'xyz.thoren.luminary/make-zoned-date
                                     "Asia/Jerusalem" 2020 5 1 1)))))))

(deftest find-the-correct-date
  (testing "that the correct start of day is returned"
    (let [r #(l/find-date %1 %2 (#'xyz.thoren.luminary/make-zoned-date
                                 "Asia/Jerusalem" %3 %4 %5 %6))
          t #(let [h (r %1 %2 %3 %4 %5 %6)]
               (is (= %7 (str (get-in h [:time :day :start]))))
               (is (= %1 (get-in h [:hebrew :month-of-year])))
               (is (= %2 (get-in h [:hebrew :day-of-month]))))]
      (testing "for the first day of the year"
        (t 1 1 2021 5 1 12 "2021-04-12T19:06+03:00[Asia/Jerusalem]"))
      (testing "for the second day of the year"
        (t 1 2 2021 5 1 12 "2021-04-13T19:06+03:00[Asia/Jerusalem]"))
      (testing "for the third day of the 12th month"
        (t 12 3 2021 5 1 12 "2022-03-05T17:39+02:00[Asia/Jerusalem]")))))

(deftest find-the-correct-date-in-year-2021
  (testing "that the correct start of day is returned"
    (let [r #(l/find-date-in-year %1 %2 %3)
          t #(let [h (r %1 %2 %3)]
               (is (= %4 (str (get-in h [:time :day :start]))))
               (is (= %2 (get-in h [:hebrew :month-of-year])))
               (is (= %3 (get-in h [:hebrew :day-of-month]))))]
      (testing "for the first day of the year"
        (t 2021 1 1 "2021-04-12T19:06+03:00[Asia/Jerusalem]"))
      (testing "for the second day of the year"
        (t 2021 1 2 "2021-04-13T19:06+03:00[Asia/Jerusalem]"))
      (testing "for the third day of the fourth month"
        (t 2021 4 3 "2021-07-12T19:46+03:00[Asia/Jerusalem]"))
      (testing "for the third day of the 12th month"
        (t 2021 12 3 "2022-03-05T17:39+02:00[Asia/Jerusalem]")))))

(deftest find-the-correct-date-in-year-2031
  (testing "that the correct start of day is returned"
    (let [r #(l/find-date-in-year %1 %2 %3)
          t #(let [h (r %1 %2 %3)]
               (is (= %4 (str (get-in h [:time :day :start]))))
               (is (= %2 (get-in h [:hebrew :month-of-year])))
               (is (= %3 (get-in h [:hebrew :day-of-month]))))]
      (testing "for the first day of the year"
        (t 2031 1 1 "2031-03-23T17:52+02:00[Asia/Jerusalem]"))
      (testing "for the second day of the year"
        (t 2031 1 2 "2031-03-24T17:53+02:00[Asia/Jerusalem]"))
      (testing "for the third day of the fourth month"
        (t 2031 4 3 "2031-06-22T19:47+03:00[Asia/Jerusalem]"))
      (testing "for the third day of the 12th month"
        (t 2031 12 3 "2032-02-13T17:23+02:00[Asia/Jerusalem]")))))

(deftest try-finding-a-bad-date-in-year
  (testing "that nothing is returned"
    (let [r #(l/find-date-in-year %1 %2 %3)
          t #(let [h (r %1 %2 %3)]
               (is (= %4 (str (get-in h [:time :day :start]))))
               (is (nil? (get-in h [:hebrew :month-of-year])))
               (is (nil? (get-in h [:hebrew :day-of-month]))))]
      (testing "for the first day of the 13th (non-existing) month"
        (t 2021 13 1 ""))
      (testing "for the 30th (non-existing) day of the 12th month"
        (t 2021 12 30 "")))))

(deftest hebrew-dates-in-puerto-williams
  (testing "that some select days are correctly calculated and reported"
    (let [lat puerto-williams-latitude
          lon puerto-williams-longitude
          r #(l/hebrew-date lat lon
                              (apply #'xyz.thoren.luminary/make-zoned-date
                                     "Antarctica/Palmer" %&))]
      (let [h (r 2021 4 12 12 0)]
        (is (= 13 (get-in h [:hebrew :month-of-year])))
        (is (= 30 (get-in h [:hebrew :day-of-month])))
        (is (= 2 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 4 12 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 1 (get-in h [:hebrew :day-of-month])))
        (is (= "Rosh Chodesh Nisan" (get-in h [:hebrew :minor-feast-day :hebrew-name])))
        (is (= 3 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 4 26 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 3 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Unleavened Bread" (get-in h [:hebrew :major-feast-day :name])))
        (is (= 1 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (= "Nisan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 5 1 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 20 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of First Fruits" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Nisan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 6 19 23 0)]
        (is (= 3 (get-in h [:hebrew :month-of-year])))
        (is (= 10 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Weeks" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Sivan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 6 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 1 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Trumpets" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 15 23 30)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 10 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Day of Atonement" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 20 23 30)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Tabernacles" (get-in h [:hebrew :major-feast-day :name])))
        (is (= 1 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 27 23 50)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 22 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "The Last Great Day" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 12 5 23 59)]
        (is (= 9 (get-in h [:hebrew :month-of-year])))
        (is (= 2 (get-in h [:hebrew :day-of-month])))
        (is (= 2 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath])))
        (is (false? (get-in h [:hebrew :major-feast-day]))))
      (let [h (r 2022 1 4 23 0)]
        (is (= 10 (get-in h [:hebrew :month-of-year])))
        (is (= 2 (get-in h [:hebrew :day-of-month])))
        (is (= 4 (get-in h [:hebrew :day-of-week])))
        (is (= "Hanukkah" (get-in h [:hebrew :major-feast-day :name])))
        (is (= 8 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 2 25 23 0)]
        (is (= 12 (get-in h [:hebrew :month-of-year])))
        (is (= 14 (get-in h [:hebrew :day-of-month])))
        (is (= 6 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath])))
        (is (= "Purim" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Adar I" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 2 26 23 0)]
        (is (= 12 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Shushan Purim" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Adar I" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 3 26 23 0)]
        (is (= 13 (get-in h [:hebrew :month-of-year])))
        (is (= 14 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (not= "Purim" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Adar II" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2030 6 9 3 0)]
        (is (= 3 (get-in h [:hebrew :month-of-year])))
        (is (= 8 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Weeks" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Sivan" (get-in h [:hebrew :names :traditional-month-of-year])))))))

(deftest hebrew-dates-in-arvidsjaur
  (testing "that some select days are correctly calculated and reported"
    (let [r (fn [& args]
              (l/hebrew-date arvidsjaur-latitude arvidsjaur-longitude
                               (apply #'xyz.thoren.luminary/make-zoned-date
                                      (flatten ["Europe/Stockholm" args]))))]
      (let [h (r 2021 4 12 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 1 (get-in h [:hebrew :day-of-month])))
        (is (= "Rosh Chodesh Nisan" (get-in h [:hebrew :minor-feast-day :hebrew-name])))
        (is (= 3 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 4 26 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 3 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Unleavened Bread" (get-in h [:hebrew :major-feast-day :name])))
        (is (= 1 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (= "Nisan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 5 1 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 20 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of First Fruits" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Nisan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 6 19 23 59)]
        (is (= 3 (get-in h [:hebrew :month-of-year])))
        (is (= 9 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (not= "Feast of Weeks" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Sivan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 6 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 1 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Trumpets" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 15 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 10 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Day of Atonement" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 20 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Tabernacles" (get-in h [:hebrew :major-feast-day :name])))
        (is (= 1 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 27 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 22 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "The Last Great Day" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 12 4 23 0)]
        (is (= 9 (get-in h [:hebrew :month-of-year])))
        (is (= 1 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (= "Rosh Chodesh Kislev" (get-in h [:hebrew :minor-feast-day :hebrew-name])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 11 1 12 0)] ; The day after end of DST.
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 26 (get-in h [:hebrew :day-of-month])))
        (is (= 2 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2022 1 4 23 0)]
        (is (= 10 (get-in h [:hebrew :month-of-year])))
        (is (= 2 (get-in h [:hebrew :day-of-month])))
        (is (= 4 (get-in h [:hebrew :day-of-week])))
        (is (= "Chanukah" (get-in h [:hebrew :major-feast-day :hebrew-name])))
        (is (= 8 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 2 25 23 0)]
        (is (= 12 (get-in h [:hebrew :month-of-year])))
        (is (= 14 (get-in h [:hebrew :day-of-month])))
        (is (= 6 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath])))
        (is (= "Purim" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Adar I" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 2 26 23 0)]
        (is (= 12 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Shushan Purim" (get-in h [:hebrew :major-feast-day :hebrew-name])))
        (is (= "Adar I" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 3 26 23 0)]
        (is (= 13 (get-in h [:hebrew :month-of-year])))
        (is (= 14 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (not= "Purim" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Adar II" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2030 6 9 3 0)]
        (is (= 3 (get-in h [:hebrew :month-of-year])))
        (is (= 8 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Weeks" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Sivan" (get-in h [:hebrew :names :traditional-month-of-year])))))))

(deftest hebrew-dates-at-temple-mount
  (testing "that some select days are correctly calculated and reported"
    (let [r (fn [& args]
              (l/hebrew-date tm-lat tm-lon
                               (apply #'xyz.thoren.luminary/make-zoned-date
                                      (flatten ["Asia/Jerusalem" args]))))]
      (let [h (r 2021 4 12 12 0)]
        (is (= 13 (get-in h [:hebrew :month-of-year])))
        (is (= 30 (get-in h [:hebrew :day-of-month])))
        (is (= 2 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 4 12 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 1 (get-in h [:hebrew :day-of-month])))
        (is (= "Rosh Chodesh Nisan" (get-in h [:hebrew :minor-feast-day :hebrew-name])))
        (is (= 3 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 4 26 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 3 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Unleavened Bread" (get-in h [:hebrew :major-feast-day :name])))
        (is (= 1 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (= "Nisan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 5 1 23 0)]
        (is (= 1 (get-in h [:hebrew :month-of-year])))
        (is (= 20 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of First Fruits" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Nisan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 6 19 23 0)]
        (is (= 3 (get-in h [:hebrew :month-of-year])))
        (is (= 10 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Weeks" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Sivan" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 6 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 1 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Trumpets" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 15 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 10 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Day of Atonement" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 20 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Tabernacles" (get-in h [:hebrew :major-feast-day :name])))
        (is (= 1 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 10 27 23 0)]
        (is (= 7 (get-in h [:hebrew :month-of-year])))
        (is (= 22 (get-in h [:hebrew :day-of-month])))
        (is (= 5 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "The Last Great Day" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Tishrei" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 12 4 23 0)]
        (is (= 9 (get-in h [:hebrew :month-of-year])))
        (is (= 1 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (= "Rosh Chodesh Kislev" (get-in h [:hebrew :minor-feast-day :hebrew-name])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2022 1 4 23 0)]
        (is (= 10 (get-in h [:hebrew :month-of-year])))
        (is (= 2 (get-in h [:hebrew :day-of-month])))
        (is (= 4 (get-in h [:hebrew :day-of-week])))
        (is (= "Hanukkah" (get-in h [:hebrew :major-feast-day :name])))
        (is (= 8 (get-in h [:hebrew :major-feast-day :day-of-feast])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 2 25 23 0)]
        (is (= 12 (get-in h [:hebrew :month-of-year])))
        (is (= 14 (get-in h [:hebrew :day-of-month])))
        (is (= 6 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath])))
        (is (= "Purim" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Adar I" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 2 26 23 0)]
        (is (= 12 (get-in h [:hebrew :month-of-year])))
        (is (= 15 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Shushan Purim" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Adar I" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2021 3 26 23 0)]
        (is (= 13 (get-in h [:hebrew :month-of-year])))
        (is (= 14 (get-in h [:hebrew :day-of-month])))
        (is (= 7 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (not= "Purim" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Adar II" (get-in h [:hebrew :names :traditional-month-of-year]))))
      (let [h (r 2030 6 9 3 0)]
        (is (= 3 (get-in h [:hebrew :month-of-year])))
        (is (= 8 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Weeks" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Sivan" (get-in h [:hebrew :names :traditional-month-of-year])))))))

(deftest hebrew-dates-at-longyearbyen
  (testing "that some select days are correctly calculated and reported"
    (let [r (fn [& args]
              (l/hebrew-date longyearbyen-latitude longyearbyen-longitude
                               (apply #'xyz.thoren.luminary/make-zoned-date
                                      (flatten ["Europe/Oslo" args]))))]
      (let [h (r 2021 4 12 12 0)]
        (is (= 13 (get-in h [:hebrew :month-of-year])))
        (is (= 30 (get-in h [:hebrew :day-of-month])))
        (is (= 2 (get-in h [:hebrew :day-of-week])))
        (is (false? (get-in h [:hebrew :sabbath]))))
      (let [h (r 2021 6 20 2 0)]
        (is (= 3 (get-in h [:hebrew :month-of-year])))
        (is (= 10 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Weeks" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Sivan" (get-in h [:hebrew :names :traditional-month-of-year])))
        (is (true? (get-in h [:time :day :start-adjusted-for-polar-region])))
        (is (true? (get-in h [:time :day :end-adjusted-for-polar-region])))
        (is (true? (get-in h [:time :week :start-adjusted-for-polar-region])))
        (is (true? (get-in h [:time :week :end-adjusted-for-polar-region])))
        (is (true? (get-in h [:time :month :start-adjusted-for-polar-region])))
        (is (true? (get-in h [:time :month :end-adjusted-for-polar-region])))
        (is (false? (get-in h [:time :year :start-adjusted-for-polar-region])))
        (is (false? (get-in h [:time :year :end-adjusted-for-polar-region]))))
      (let [h (r 2030 6 9 3 0)]
        (is (= 3 (get-in h [:hebrew :month-of-year])))
        (is (= 8 (get-in h [:hebrew :day-of-month])))
        (is (= 1 (get-in h [:hebrew :day-of-week])))
        (is (true? (get-in h [:hebrew :sabbath])))
        (is (= "Feast of Weeks" (get-in h [:hebrew :major-feast-day :name])))
        (is (= "Sivan" (get-in h [:hebrew :names :traditional-month-of-year])))))))

(deftest test-known-feast-days
  (let [r #(l/list-of-known-feast-days-in-gregorian-year %)
        t #(is (= %2 (r %1)))]
    (testing "testing the known feast days"
      (testing "in 2021"
        (t 2021 '("2021-01-13 First day of the 11th month"
                  "2021-02-12 First day of the 12th month"
                  "2021-02-25 Purim"
                  "2021-02-26 Shushan Purim"
                  "2021-03-13 First day of the 13th month"
                  "2021-04-12 First day of the year"
                  "2021-04-25 Passover"
                  "2021-04-26 First day of the Feast of Unleavened Bread"
                  "2021-04-27 Second day of the Feast of Unleavened Bread"
                  "2021-04-28 Third day of the Feast of Unleavened Bread"
                  "2021-04-29 Fourth day of the Feast of Unleavened Bread"
                  "2021-04-30 Fifth day of the Feast of Unleavened Bread"
                  "2021-05-01 Feast of First Fruits"
                  "2021-05-02 Last day of the Feast of Unleavened Bread"
                  "2021-05-12 First day of the 2nd month"
                  "2021-06-10 First day of the 3rd month"
                  "2021-06-19 Feast of Weeks"
                  "2021-07-10 First day of the 4th month"
                  "2021-08-08 First day of the 5th month"
                  "2021-09-07 First day of the 6th month"
                  "2021-10-06 Feast of Trumpets"
                  "2021-10-06 First day of the 7th month"
                  "2021-10-15 Day of Atonement"
                  "2021-10-20 First day of the Feast of Tabernacles"
                  "2021-10-21 Second day of the Feast of Tabernacles"
                  "2021-10-22 Third day of the Feast of Tabernacles"
                  "2021-10-23 Fourth day of the Feast of Tabernacles"
                  "2021-10-24 Fifth day of the Feast of Tabernacles"
                  "2021-10-25 Sixth day of the Feast of Tabernacles"
                  "2021-10-26 Last day of the Feast of Tabernacles"
                  "2021-10-27 The Last Great Day"
                  "2021-11-05 First day of the 8th month"
                  "2021-12-04 First day of the 9th month"
                  "2021-12-28 First day of Hanukkah"
                  "2021-12-29 Second day of Hanukkah"
                  "2021-12-30 Third day of Hanukkah"
                  "2021-12-31 Fourth day of Hanukkah")))
      (testing "in 2039"
        (t 2039 '("2039-01-24 First day of the 11th month"
                  "2039-02-23 First day of the 12th month"
                  "2039-03-08 Purim"
                  "2039-03-09 Shushan Purim"
                  "2039-03-25 First day of the year"
                  "2039-04-07 Passover"
                  "2039-04-08 First day of the Feast of Unleavened Bread"
                  "2039-04-09 Feast of First Fruits"
                  "2039-04-10 Third day of the Feast of Unleavened Bread"
                  "2039-04-11 Fourth day of the Feast of Unleavened Bread"
                  "2039-04-12 Fifth day of the Feast of Unleavened Bread"
                  "2039-04-13 Sixth day of the Feast of Unleavened Bread"
                  "2039-04-14 Last day of the Feast of Unleavened Bread"
                  "2039-04-23 First day of the 2nd month"
                  "2039-05-23 First day of the 3rd month"
                  "2039-05-28 Feast of Weeks"
                  "2039-06-22 First day of the 4th month"
                  "2039-07-21 First day of the 5th month"
                  "2039-08-20 First day of the 6th month"
                  "2039-09-18 Feast of Trumpets"
                  "2039-09-18 First day of the 7th month"
                  "2039-09-27 Day of Atonement"
                  "2039-10-02 First day of the Feast of Tabernacles"
                  "2039-10-03 Second day of the Feast of Tabernacles"
                  "2039-10-04 Third day of the Feast of Tabernacles"
                  "2039-10-05 Fourth day of the Feast of Tabernacles"
                  "2039-10-06 Fifth day of the Feast of Tabernacles"
                  "2039-10-07 Sixth day of the Feast of Tabernacles"
                  "2039-10-08 Last day of the Feast of Tabernacles"
                  "2039-10-09 The Last Great Day"
                  "2039-10-18 First day of the 8th month"
                  "2039-11-16 First day of the 9th month"
                  "2039-12-10 First day of Hanukkah"
                  "2039-12-11 Second day of Hanukkah"
                  "2039-12-12 Third day of Hanukkah"
                  "2039-12-13 Fourth day of Hanukkah"
                  "2039-12-14 Fifth day of Hanukkah"
                  "2039-12-15 Sixth day of Hanukkah"
                  "2039-12-16 First day of the 10th month"
                  "2039-12-16 Sixth day of Hanukkah"
                  "2039-12-17 Seventh day of Hanukkah"
                  "2039-12-18 Last day of Hanukkah"))))))
