(ns xyz.thoren.luminary
  (:require [java-time :as t]
            [xyz.thoren.equinox :refer [march-equinox]])
  (:import (org.shredzone.commons.suncalc SunTimes MoonPhase)))

(def jerusalem-lat 31.7781161)
(def jerusalem-lon 35.233804)
(def jerusalem-tz "Asia/Jerusalem")

(defn- make-utc-date
  [& args]
  (t/with-zone (apply t/zoned-date-time args) "UTC"))

(defn- date-of-march-equinox
  [year]
  (->> [:year :month :day :hour :minute :second]
       (map #(% (march-equinox year)))
       (apply make-utc-date)))

(defn- go-back
  [adjustment date]
  (t/adjust date t/minus adjustment))

(defn- go-forward
  [adjustment date]
  (t/adjust date t/plus adjustment))

(defn- tz?
  [date]
  (t/zone-id date))

(defn- calculate-sun-events
  [lat lon date]
  (let [tz (tz? date)]
    (-> (SunTimes/compute)
        (.on date)
        (.at lat lon)
        (.oneDay)
        (.timezone (str tz))
        (.execute))))

(defn- next-sunset
  [lat lon date & {:keys [adjusted] :or {adjusted false}}]
  (let [sun (calculate-sun-events lat lon date)
        always-up (.isAlwaysUp sun)
        always-down (.isAlwaysDown sun)
        sunset (.getSet sun)]
    (cond
      (or always-down always-up)
      (cond
        (< lat -65.7)
        (next-sunset -65.7 lon date :adjusted true)
        (> lat 65.7)
        (next-sunset 65.7 lon date :adjusted true)
        :else
        (throw (Exception. (str "Sun either always up or always down but"
                                " latitude is: " lat))))
      (nil? sunset) ;; This seems to happen when the sunset happened the minute
                  ;; before date.
      (next-sunset lat lon (go-forward (t/minutes 1) date)
                   :adjusted adjusted)
      :else
      {:sunset (t/truncate-to sunset :seconds)
       :adjusted-for-polar-region adjusted
       :always-down always-down
       :always-up always-up})))

(defn- next-start-of-day-full-map
  [lat lon date]
  ;; The dancing back and forth in the following function is due to the fact
  ;; that `SunTimes/compute` seems to give different answers depending on what
  ;; time of day one provides as date, even if the next time of sunset should
  ;; be the same.
  ;;
  ;; To work around this, always ask for the time of sunset 1 hour before the
  ;; originally reported sunset.
  (->> (next-sunset lat lon date)
       (:sunset)
       (go-back (t/hours 1))
       (next-sunset lat lon)))

(defn- next-start-of-day
  [lat lon date]
  (:sunset (next-start-of-day-full-map lat lon date)))

(defn- previous-start-of-day
  [lat lon date]
  (->> (next-start-of-day lat lon date)
       (go-back (t/hours 25))
       (next-start-of-day lat lon)))

(defn- noon
  [lat lon date]
  (let [tz (tz? date)
        year (t/as date :year)
        month (t/as date :month-of-year)
        day (t/as date :day-of-month)
        morning (t/with-zone (t/zoned-date-time year month day 4 0) tz)]
    (.getNoon (calculate-sun-events lat lon morning))))

(defn- boundaries-of-day
  [lat lon date]
  (let [p (previous-start-of-day lat lon date)
        n (next-start-of-day lat lon (noon lat lon (go-forward (t/hours 16) p)))]
    [p (go-back (t/seconds 1) n)]))

(defn- calculate-new-moon
  [date]
  (let [tz (tz? date)]
    (-> (MoonPhase/compute)
        (.on date)
        (.timezone (str tz))
        (.execute))))

(defn- next-new-moon
  [date]
  (-> (calculate-new-moon date)
      (.getTime)
      (t/truncate-to :seconds)))

(defn- previous-new-moon
  [date]
  (->> (next-new-moon date)
       (go-back (t/days 31))
       (next-new-moon)))

(defn zone-it
  "Given a string containing a valid timezone name `tz`, and a
  java.time.ZonedDateTime object `date`, convert `date` to the same instant in
  `tz`."
  [tz date]
  (t/with-zone-same-instant date tz))

(defn- next-start-of-month-in-israel
  [date]
  (let [zdate (zone-it jerusalem-tz date)
        lat jerusalem-lat
        lon jerusalem-lon
        previous-moon (previous-new-moon zdate)
        day-following-previous-moon (next-start-of-day lat lon previous-moon)
        next-day (next-start-of-day lat lon zdate)]
    (if (zero? (t/as (t/duration day-following-previous-moon next-day)
                     :seconds))
      next-day
      (next-start-of-day lat lon (next-new-moon zdate)))))

(defn- previous-start-of-month-in-israel
  [date]
  (->> (next-start-of-month-in-israel date)
       (go-back (t/days 32))
       (next-start-of-month-in-israel)))

(defn- year-month-day
  [date]
  (t/as date :year :month-of-year :day-of-month))

(defn- make-zoned-date
  [tz & args]
  (t/with-zone (apply t/zoned-date-time args) tz))

(defn- next-start-of-month
  [lat lon date]
  (let [tz (tz? date)
        prev-month-israel (previous-start-of-month-in-israel date)
        next-month-israel (next-start-of-month-in-israel date)
        next-day (next-start-of-day lat lon date)]
    (if (= (year-month-day prev-month-israel) (year-month-day next-day))
      next-day
      (as-> (year-month-day next-month-israel) <>
            (make-zoned-date tz (first <>) (second <>) (last <>))
            (noon lat lon <>)
            (next-start-of-day lat lon <>)))))

(defn- previous-start-of-month
  [lat lon date]
  (->> (next-start-of-month lat lon date)
       (go-back (t/days 32))
       (next-start-of-month lat lon)))

(defn- boundaries-of-month
  [lat lon date]
  (let [p (previous-start-of-month lat lon date)
        n (next-start-of-month lat lon (go-forward (t/days 2) p))]
    [p (go-back (t/seconds 1) n)]))

(defn- next-march-equinox
  [date]
  (let [tz (tz? date)
        year (Integer/parseInt (str (t/year date)))
        same-year-march-equinox (date-of-march-equinox year)]
    (if (t/before? date same-year-march-equinox)
      (zone-it tz same-year-march-equinox)
      (zone-it tz (date-of-march-equinox (inc year))))))

(defn- previous-march-equinox
  [date]
  (let [tz (tz? date)
        year (Integer/parseInt (str (t/year date)))
        same-year-march-equinox (date-of-march-equinox year)]
    (if (t/before? same-year-march-equinox date)
      (zone-it tz same-year-march-equinox)
      (zone-it tz (date-of-march-equinox (dec year))))))

(defn- next-start-of-year-in-israel
  [date]
  (let [zdate (zone-it jerusalem-tz date)
        this-year-march-equinox (date-of-march-equinox (t/as zdate :year))
        previous-month (previous-start-of-month-in-israel zdate)
        pme (previous-march-equinox zdate)
        moon-following-previous-equinox (next-new-moon pme)
        day-following-moon (next-start-of-day jerusalem-lat jerusalem-lon
                                              moon-following-previous-equinox)
        next-day (next-start-of-day jerusalem-lat jerusalem-lon zdate)
        potential-new-year (next-start-of-month-in-israel
                            (next-march-equinox zdate))]
    (cond
      (zero? (t/as (t/duration day-following-moon next-day) :seconds))
      next-day
      (t/before? previous-month this-year-march-equinox zdate)
      (next-start-of-month-in-israel zdate)
      :else
      potential-new-year)))

(defn- previous-start-of-year-in-israel
  [date]
  (let [y (t/as date :year)
        m (t/as date :month-of-year)]
    (cond
      (> m 4) (next-start-of-year-in-israel (make-zoned-date jerusalem-tz y 2 1))
      (< m 3) (next-start-of-year-in-israel (make-zoned-date jerusalem-tz (dec y) 2 1))
      :else   (->> (next-start-of-year-in-israel date)
                   (go-back (t/days 400))
                   (next-start-of-year-in-israel)))))

(defn- next-start-of-year
  [lat lon date]
  (let [tz (tz? date)
        prev-year-israel (previous-start-of-year-in-israel date)
        next-year-israel (next-start-of-year-in-israel date)
        next-day (next-start-of-day lat lon date)]
    (if (= (year-month-day prev-year-israel) (year-month-day next-day))
      next-day
      (as-> (year-month-day next-year-israel) <>
            (make-zoned-date tz (first <>) (second <>) (last <>))
            (noon lat lon <>)
            (next-start-of-day lat lon <>)))))

(defn- previous-start-of-year
  [lat lon date]
  (let [y (t/as date :year)
        m (t/as date :month-of-year)
        tz (tz? date)]
    (cond
      (> m 4) (next-start-of-year lat lon (make-zoned-date tz y 2 1))
      (< m 3) (next-start-of-year lat lon (make-zoned-date tz (dec y) 2 1))
      :else   (->> (next-start-of-year lat lon date)
                   (go-back (t/days 400))
                   (next-start-of-year lat lon)))))

(defn- boundaries-of-year
  [lat lon date]
  (let [p (previous-start-of-year lat lon date)
        n (next-start-of-year lat lon (go-forward (t/days 1) p))]
    [p (go-back (t/seconds 1) n)]))

(defn- next-start-of-week
  [lat lon date]
  (->> (range 0 8)
       (map #(->> (next-start-of-day lat lon date)
                  (go-forward (t/days %))
                  (go-back (t/hours 8))
                  (noon lat lon)
                  (next-start-of-day lat lon)))
       (dedupe)
       ;; Since the day sometimes starts after local midnight, due to DST
       (filter #(or (and (t/saturday? %) (>= (t/as % :hour-of-day) 12))
                    (and (t/sunday? %) (< (t/as % :hour-of-day) 12))))
       (first)))

(defn- previous-start-of-week
  [lat lon date]
  (->> (next-start-of-week lat lon date)
       (go-back (t/days 10))
       (next-start-of-week lat lon)))

(defn- boundaries-of-week
  [lat lon date]
  (let [p (previous-start-of-week lat lon date)
        n (next-start-of-week lat lon (go-forward (t/days 1) p))]
    [p (go-back (t/seconds 1) n)]))

(defn- start-of-days-in-week
  [lat lon date]
  (let [w (boundaries-of-week lat lon date)]
    (->> (range 1 8)
         (map #(->> (first w)
                    (go-forward (t/days %))
                    (go-back (t/hours 8))
                    (noon lat lon)
                    (next-start-of-day lat lon)))
         (dedupe)
         (filter #(t/before? % (second w)))
         (cons (first w)))))

(defn- day-of-week
  [lat lon date]
  (let [p (previous-start-of-day lat lon date)
        pj (t/as p :julian-day)]
    (->> (start-of-days-in-week lat lon date)
         (map #(t/as % :julian-day))
         (apply sorted-set)
         (keep-indexed #(when (= %2 pj) %1))
         (first)
         (inc))))

(defn- new-moons-since-start-of-year
  [lat lon date]
  (let [y (boundaries-of-year lat lon date)
        start-of-year (first y)
        end-of-year (second y)]
    (->> (range 0 14)
         (map #(next-new-moon (go-forward (t/days (* % 29)) start-of-year)))
         (dedupe)
         (filter #(t/before? % end-of-year))
         (drop-last))))

(defn- start-of-months-in-year
  [lat lon date]
  (->> (new-moons-since-start-of-year lat lon date)
       (map #(next-start-of-month lat lon (go-back (t/hours 1) %)))
       (cons (previous-start-of-year lat lon date))))

(defn- month
  [lat lon date]
  (let [p (t/as (previous-start-of-month lat lon date) :julian-day)]
    (->> (start-of-months-in-year lat lon date)
         (map #(t/as % :julian-day))
         (apply sorted-set)
         (keep-indexed #(when (= %2 p) %1))
         (first)
         (inc))))

(defn- start-of-days-in-month
  [lat lon date]
  (let [m (boundaries-of-month lat lon date)
        start-of-month (first m)
        end-of-month (second m)]
    (->> (range 1 32)
         (map #(->> start-of-month
                    (go-forward (t/days %))
                    (go-back (t/hours 2))
                    (noon lat lon)
                    (next-start-of-day lat lon)))
         (dedupe)
         (filter #(t/before? % end-of-month))
         (cons start-of-month))))

(defn- day-of-month
  [lat lon date]
  (let [p (previous-start-of-day lat lon date)
        pj (t/as p :julian-day)
        m (start-of-days-in-month lat lon date)]
    (->> (map #(t/as % :julian-day) m)
         (apply sorted-set)
         (keep-indexed #(when (= %2 pj) %1))
         (first)
         (inc))))

(def traditional-month-names
  [ "Nisan" "Iyar" "Sivan" "Tammuz" "Av" "Elul" "Tishrei" "Marcheshvan"
   "Kislev" "Tevet" "Shevat" "Adar" "Adar II"])

(def month-numbers
  (flatten ["1st" "2nd" "3rd" (map #(str % "th") (range 4 14))]))

(def day-numbers
  (flatten ["1st" "2nd" "3rd" (map #(str % "th") (range 4 21))
            "21st" "22nd" "23rd" (map #(str % "th") (range 24 31))]))

(def weekday-names
  (flatten [(map #(str % " day of the week")
                 ["1st" "2nd" "3rd" "4th" "5th" "6th"])
            "Sabbath"]))

(defn- single-day-feast
  [m]
  (assoc m :day-of-feast 1 :days-in-feast 1))

(def pesach
  (single-day-feast {:name "Passover" :hebrew-name "Pesach"}))

(defn- ha-matzot
  [day-of-feast]
  {:name "Feast of Unleavened Bread"
   :hebrew-name "Chag Ha-Matzot"
   :day-of-feast day-of-feast
   :days-in-feast 7})

(def yom-bikkurim
  (single-day-feast {:name "Feast of First Fruits" :hebrew-name "Yom Bikkurim"}))

(def shavuot
  (single-day-feast
   {:name "Feast of Weeks"
    :alternative-name "Feast of Pentecost"
    :hebrew-name "Shavuot"}))

(defn- ha-sukkot
  [day-of-feast]
  {:name "Feast of Tabernacles"
   :alternative-name "Feast of Booths"
   :hebrew-name "Chag Ha-Sukkot"
   :day-of-feast day-of-feast
   :days-in-feast 7})

(def yom-teruah
  (single-day-feast {:name "Feast of Trumpets" :hebrew-name "Yom Teruah"}))

(def yom-ha-kippurim
  (single-day-feast
   {:name "Day of Atonement"
    :hebrew-name "Yom Ha-Kippurim"
    :alternative-hebrew-name "Yom Kippur"}))

(def shemini-atzeret
  (single-day-feast
   {:name "The Last Great Day"
    :hebrew-name "Shemini Atzeret"}))

(defn- chanukah
  [day-of-feast]
  {:name "Hanukkah"
   :hebrew-name "Chanukah"
   :day-of-feast day-of-feast
   :days-in-feast 8})

(def purim
  {:name "Purim" :hebrew-name "Purim" :day-of-feast 1 :days-in-feast 2})

(def shushan-purim
  {:name "Shushan Purim"
   :hebrew-name "Shushan Purim"
   :day-of-feast 2
   :days-in-feast 2})

(defn- feast-day?
  "Given `m` (hebrew month of year), `d` (hebrew day of month), and `dow`
  (hebrew day of week), return a map containing details of any feast day on that
  day, or return `false` if there is none.

  `:days-in-prev-month` is mandatory if `m` = 10 and `d` is <= 1 and 3."
  [m d dow & {:keys [days-in-prev-month]}]
  {:pre [(or (not (and (= m 10) (<= 1 d 3)))
             (<= 29 days-in-prev-month 30))]}
  (cond
    (and (= m 1) (= d 14)) pesach
    (and (= m 1) (= d 15)) (ha-matzot 1)
    (and (= m 1) (< 15 d 22) (= dow 1)) yom-bikkurim
    (and (= m 1) (= d 16)) (ha-matzot 2)
    (and (= m 1) (= d 17)) (ha-matzot 3)
    (and (= m 1) (= d 18)) (ha-matzot 4)
    (and (= m 1) (= d 19)) (ha-matzot 5)
    (and (= m 1) (= d 20)) (ha-matzot 6)
    (and (= m 1) (= d 21)) (ha-matzot 7)
    (and (= m 3) (< 5 d 12) (= dow 1)) shavuot
    (and (= m 7) (= d 1)) yom-teruah
    (and (= m 7) (= d 10)) yom-ha-kippurim
    (and (= m 7) (= d 15)) (ha-sukkot 1)
    (and (= m 7) (= d 16)) (ha-sukkot 2)
    (and (= m 7) (= d 17)) (ha-sukkot 3)
    (and (= m 7) (= d 18)) (ha-sukkot 4)
    (and (= m 7) (= d 19)) (ha-sukkot 5)
    (and (= m 7) (= d 20)) (ha-sukkot 6)
    (and (= m 7) (= d 21)) (ha-sukkot 7)
    (and (= m 7) (= d 22)) shemini-atzeret
    (and (= m 9) (= d 25)) (chanukah 1)
    (and (= m 9) (= d 26)) (chanukah 2)
    (and (= m 9) (= d 27)) (chanukah 3)
    (and (= m 9) (= d 28)) (chanukah 4)
    (and (= m 9) (= d 29)) (chanukah 5)
    (and (= m 9) (= d 30)) (chanukah 6)
    (and (= m 10) (= d 1) (= days-in-prev-month 29)) (chanukah 6)
    (and (= m 10) (= d 1) (= days-in-prev-month 30)) (chanukah 7)
    (and (= m 10) (= d 2) (= days-in-prev-month 29)) (chanukah 7)
    (and (= m 10) (= d 2) (= days-in-prev-month 30)) (chanukah 8)
    (and (= m 10) (= d 3) (= days-in-prev-month 29)) (chanukah 8)
    (and (= m 12) (= d 14)) purim
    (and (= m 12) (= d 15)) shushan-purim
    :else false))

(defn- sabbath?
  "Given the hebrew `month-of-year`, `day-of-month`, and `day-of-week`, return
  `true` if it's a Sabbath or `false` if it's not."
  [month-of-year day-of-month day-of-week]
  (or (= 7 day-of-week)
      (and (= month-of-year 1) (= day-of-month 15))                      ; "First day of the Feast of Unleavened Bread"
      (and (= month-of-year 1) (< 15 day-of-month 22) (= day-of-week 1)) ; "Feast of First Fruits"
      (and (= month-of-year 1) (= day-of-month 21))                      ; "Last day of the Feast of Unleavened Bread"
      (and (= month-of-year 3) (< 5 day-of-month 12) (= day-of-week 1))  ; "Feast of Weeks"
      (and (= month-of-year 7) (= day-of-month 1))                       ; "Feast of Trumpets"
      (and (= month-of-year 7) (= day-of-month 10))                      ; "Day of Atonement"
      (and (= month-of-year 7) (= day-of-month 15))                      ; "First day of the Feast of Tabernacles"
      (and (= month-of-year 7) (= day-of-month 22))))                    ; "The Last Great Day"

(defn- days-between
  "Given a `start` and an `end`, calculate the duration between the events in
  days."
  [start end]
  (t/as (t/duration start end) :days))

(defn- polar-adjusted?
  [lat lon date]
  (if (<= -65 lat 65)
    false
    (->> (go-back (t/hours 2) date)
         (next-start-of-day-full-map lat lon)
         (:adjusted-for-polar-region))))

(defn- assoc-polar-status
  [lat lon m]
  (assoc m :start-adjusted-for-polar-region (polar-adjusted? lat lon (:start m))
           :end-adjusted-for-polar-region (polar-adjusted? lat lon (:end m))))

(defn- with-polar-status
  [lat lon m]
  (into (empty m) (for [[k v] m] [k (assoc-polar-status lat lon v)])))

(defn now "Return the current time." [] (t/zoned-date-time))

(defn hebrew-date
  "Return a map containing the details of a `hebrew-date` where:
  `lat` is the latitude of the location,
  `lon` is the longitude of the location, and
  `date` is a java.time.ZonedDateTime object from which the beginning of the
  hebrew year will be calculated. I.e, it will use the 'current' year of the
  `date` as the base for calculating `m` and `d`.

  If only `date` is provided `lat` and `lon` will default to the coordinates of
  the Temple Mount in Jerusalem.

  In addition, if only `lat`, and `lon` is provided `date` will default to the
  current time in the \"Asia/Jerusalem\" timezone.

  Example:
  (hebrew-date 59.3325800 18.0649000 (zone-it \"Europe/Stockholm\" (now)))

  The above will look for the current hebrew date. The coordinates are those of
  Stockholm, Sweden. Using the `zone-it` function to make sure that `now` is
  returned in the correct timezone is recommended, in this case
  \"Europe/Stockholm\".

  Caution: Make sure that `date` is using the actual timezone of the location at
  the provided coordinates. Otherwise the results may not even be produced, or
  they will be inaccurate. Calculating the timezone of a given location is out
  of scope of this library.

  See also `zone-it`, and `now`."
  ([lat lon date]
   {:pre [(and (number? lat) (<= -90 lat 90))
          (and (number? lon) (<= -180 lon 180))
          (and (or (t/zoned-date-time? date)
                   (t/local-date-time? date)
                   (t/offset-date-time date))
               (<= 1584 (t/as date :year) 2200))]}
   (let [day-boundaries (boundaries-of-day lat lon date)
         week-boundaries (boundaries-of-week lat lon date)
         month-boundaries (boundaries-of-month lat lon date)
         year-boundaries (boundaries-of-year lat lon date)
         start-of-year (first year-boundaries)
         end-of-year (last year-boundaries)
         start-of-month (first month-boundaries)
         end-of-month (last month-boundaries)
         start-of-week (first week-boundaries)
         end-of-week (last week-boundaries)
         start-of-day (first day-boundaries)
         end-of-day (last day-boundaries)
         days-in-year (days-between start-of-year end-of-year)
         months-in-year (if (<= 383 days-in-year 384) 13 12)
         month (month lat lon date)
         dom (day-of-month lat lon date)
         dow (day-of-week lat lon date)]
     {:hebrew {:month-of-year month
               :months-in-year months-in-year
               :day-of-month dom
               :days-in-month (days-between start-of-month end-of-month)
               :day-of-week dow
               :sabbath (sabbath? month dom dow)
               :feast-day (feast-day? month dom dow
                                      :days-in-prev-month
                                      (when (and (= month 10) (< dom 4))
                                        (let [b (boundaries-of-month
                                                 lat lon
                                                 (go-back (t/days 1)
                                                          start-of-month))]
                                          (days-between (first b) (second b)))))
               :names {:month-of-year (nth month-numbers (dec month))
                       :traditional-month-of-year
                       (if (and (= month 12) (= months-in-year 13))
                         "Adar I"
                         (nth traditional-month-names (dec month)))
                       :day-of-month (nth day-numbers (dec dom))
                       :day-of-week (nth weekday-names (dec dow))}}
      :time (with-polar-status lat lon
              {:year {:start start-of-year :end end-of-year}
               :month {:start start-of-month :end end-of-month}
               :week {:start start-of-week :end end-of-week}
               :day {:start start-of-day :end end-of-day}})}))
  ([lat lon]
   {:pre [(and (number? lat) (<= -90 lat 90))
          (and (number? lon) (<= -180 lon 180))]}
   (hebrew-date lat lon (now)))
  ([date]
   {:pre [(and (or (t/zoned-date-time? date)
                   (t/local-date-time? date)
                   (t/offset-date-time date))
               (<= 1584 (t/as date :year) 2100))]}
   (hebrew-date jerusalem-lat jerusalem-lon date))
  ([]
   (hebrew-date (zone-it jerusalem-tz (now)))))

(defn find-date
  "Return a map containing the details of a `hebrew-date` where:
  `lat` is the latitude of the location,
  `lon` is the longitude of the location,
  `m` is the hebrew month of year,
  `d` is the hebrew day of month that you are looking for, and
  `date` is a java.time.ZonedDateTime object from which the beginning of the
  hebrew year will be calculated. I.e, it will use the 'current' year of the
  `date` as the base for calculating `m` and `d`.

  If only `m`, `d`, and `date` are provided `lat` and `lon` will default to
  the coordinates of the Temple Mount in Jerusalem.

  In addition, if only `m`, and `d` is provided `date` will default to the
  current time in the \"Asia/Jerusalem\" timezone.

  Example:
  (find-date 59.3325800 18.0649000 1 14 (zone-it \"Europe/Stockholm\" (now)))

  The above will look for the 14th day of the 1st month in the hebrew year that
  starts in the current gregorian year (based on the system time and timezone.
  The coordinates are those of Stockholm, Sweden. Using the `zone-it` function
  to make sure that `now` is returned in the correct timezone is recommended,
  in this case \"Europe/Stockholm\".

  Caution: Make sure that `date` is using the actual timezone of the location at
  the provided coordinates. Otherwise the results may not even be produced, or
  they will be inaccurate. Calculating the timezone of a given location is out
  of scope of this library.

  See also `hebrew-date`, `zone-it`, and `now`."
  ([lat lon m d date]
   {:pre [(and (pos-int? m) (< 0 m 14))
          (and (pos-int? d) (< 0 d 31))]}
   (let [months (start-of-months-in-year lat lon date)
         start-of-month (try
                          (nth months (dec m))
                          (catch IndexOutOfBoundsException _e nil))
         days (when start-of-month
                (->> start-of-month
                     (go-forward (t/hours 1))
                     (start-of-days-in-month lat lon)))]
     (when days
       (try
         (->> (dec d)
              (nth days)
              (go-forward (t/hours 1))
              (hebrew-date lat lon))
         (catch IndexOutOfBoundsException _e nil)))))
  ([m d date]
   (find-date jerusalem-lat jerusalem-lon m d date))
  ([m d]
   (find-date m d (zone-it jerusalem-tz (now)))))

(defn find-date-in-year
  "Return a map containing the details of a `hebrew-date` where:
  `lat` is the latitude of the location
  `lon` is the longitude of the location
  `tz` is a string containing a valid TimeZone description
  `y` is the gregorian year in which the hebrew year in question starts
  `m` is the hebrew month of year, and
  `d` is the hebrew day of month that you are looking for.

  If only `tz`, `y`, `m`, and `d` are provided `lat` and `lon` will default to
  the coordinates of the Temple Mount in Jerusalem.

  In addition, if only `y`, `m`, and `d` is provided `tz` will default to
  \"Asia/Jerusalem\".

  Example:
  (find-date-in-year 59.3325800 18.0649000 \"Europe/Stockholm\" 2025 1 14)

  The above will look for the 14th day of the 1st month of the hebrew year that
  starts in the gregorian year 2025. The timezone in use will be
  \"Europe/Stockholm\" and the coordinates are those of Stockholm, Sweden.

  Caution: Make sure that `tz` is the actual timezone of the location at the
  provided coordinates. Otherwise the results may not even be produced, or they
  will be inaccurate. Calculating the timezone of a given location is out of
  scope of this library.

  See also `find-date`."
  ([lat lon tz y m d]
   {:pre [(string? tz)
          (and (pos-int? y) (<= 1584 y 2200))
          (and (pos-int? m) (< 0 m 14))
          (and (pos-int? d) (< 0 d 31))]}
   (find-date lat lon m d (make-zoned-date tz y 6 1 12)))
  ([tz y m d]
   (find-date-in-year jerusalem-lat jerusalem-lon tz y m d))
  ([y m d]
   (find-date-in-year jerusalem-tz y m d)))
