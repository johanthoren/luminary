(ns xyz.thoren.luminary
  (:require [java-time :as t]
            [xyz.thoren.equinox :refer [march-equinox]]
            [xyz.thoren.luminary-feast-data :as fd])
  (:import (org.shredzone.commons.suncalc SunTimes SunTimes$SunTimesBuilder
                                          MoonPhase MoonPhase$MoonPhaseBuilder)
           (java.time ZonedDateTime)))

(def jerusalem-lat
  "The Latitude of the Temple Mount in Jerusalem, Israel."
  31.7781161)

(def jerusalem-lon
  "The Longitude of the Temple Mount in Jerusalem, Israel."
  35.233804)

(def jerusalem-tz
  "A string representing the ZoneRegion of Jerusalem, Israel."
  "Asia/Jerusalem")

(def calculated-feast-days
  "A map of pre-calculated feast days covering the years 2020-2039."
  (apply merge [fd/feasts-2020
                fd/feasts-2021
                fd/feasts-2022
                fd/feasts-2023
                fd/feasts-2024
                fd/feasts-2025
                fd/feasts-2026
                fd/feasts-2027
                fd/feasts-2028
                fd/feasts-2029
                fd/feasts-2030
                fd/feasts-2031
                fd/feasts-2032
                fd/feasts-2033
                fd/feasts-2034
                fd/feasts-2035
                fd/feasts-2036
                fd/feasts-2037
                fd/feasts-2038
                fd/feasts-2039]))

(defn valid-zone-id?
  "Given a string `s`, test if it can be transformed into a valid
  java.time.ZoneRegion. Passing a java-time.ZoneRegion object will also return
  true."
  [s]
  (t/zone-id? (try (t/zone-id s) (catch Exception _e nil))))

(defn zone-it
  "Given a valid timezone `tz` (either a string or a java.time.ZoneRegion
  object), and a java.time.ZonedDateTime object `date`, convert `date` to the
  same instant in `tz`."
  [tz date]
  {:pre [(valid-zone-id? tz) (t/zoned-date-time? date)]}
  (t/with-zone-same-instant date tz))

(defn truncate-to-minutes
  [date]
  (t/truncate-to date :minutes))

(defn now
  "Return the current time using the system timezone."
  []
  (t/zoned-date-time))

(defn make-zoned-date
  "Given a string containing a valid timezone name `tz`, and at least 3
  integers, return a ZonedDateTime object. Accepts between 3 and 7 integer
  arguments representing year, month, day, hour, minute, second, nanos."
  [tz & args]
  {:pre [(valid-zone-id? tz)
         (empty? (remove int? args))
         (<= 3 (count args) 7)]}
  (t/with-zone (apply t/zoned-date-time args) tz))

(defn make-utc-date
  "Given at least 3 integers, return a ZonedDateTime object in the UTC timezone.
  Accepts between 3 and 7 arguments representing year, month, day, hour, minute,
  second, nanos."
  [& args]
  (apply make-zoned-date (cons "UTC" args)))

(defn go-back
  "Subtract `adjustment` from `date`.

  Example: (go-back (t/hours 1) (now))"
  [adjustment date]
  (t/adjust date t/minus adjustment))

(defn go-forward
  "Add `adjustment` to `date`.

  Example: (go-forward (t/hours 1) (now))"
  [adjustment date]
  (t/adjust date t/plus adjustment))

(defn time-of-march-equinox
  "Given an integer `year`, return the time of the March Equinox of that year as
  a java.time.ZonedDateTime object in UTC."
  [year]
  (->> [:year :month :day :hour :minute :second]
       (map #(% (march-equinox year)))
       (apply make-utc-date)))

(defn calculate-sun-events
  "Given `lat` and `lon` for a location, and a java.time.ZonedDateTime object
  `date` in the timezone that corresponds to that location, return an
  org.shredzone.commons.suncalc.SunTimes object describing the sun events
  following `date`.

  Example:
  (calculate-sun-events 31 35 (make-zoned-date \"Asia/Jerusalem\" 2021 6 1 12))"
  [lat lon ^ZonedDateTime date]
  (let [t (str (t/zone-id date))]
    (as-> (SunTimes/compute) <>
      (.on ^SunTimes$SunTimesBuilder <> date)
      (.at ^SunTimes$SunTimesBuilder <> lat lon)
      (.oneDay ^SunTimes$SunTimesBuilder <>)
      (.timezone ^SunTimes$SunTimesBuilder <> t)
      (.execute ^SunTimes$SunTimesBuilder <>))))

(defn- bigdec-
  [a b]
  (float (- (bigdec a) (bigdec b))))

(defn- bigdec+
  [a b]
  (float (+ (bigdec a) (bigdec b))))

(defn- towards-polar-circle
  [lat]
  {:pre [(<= -90 lat 90)]}
  (cond
   (<= -65.7 lat 65.7) lat
   (<= 65.7 lat 65.8) 65.7
   (>= -65.7 lat -65.8) -65.7
   (<= 65.8 lat 67) (bigdec- lat 0.1)
   (>= -65.8 lat -67) (bigdec+ lat 0.1)
   (<= 67 lat 70) (bigdec- lat 0.3)
   (>= -67 lat 70) (bigdec+ lat 0.3)
   (<= 70 lat 75) (bigdec- lat 0.5)
   (>= -70 lat -75) (bigdec+ lat 0.5)
   (neg? lat) (inc lat)
   :else (dec lat)))

(defn next-sunset
  "Given `lat` and `lon` for a location, and a java.time.ZonedDateTime object
  `date` in the timezone that corresponds to that location, return a map
  containing the next following sunset for that date.

  If the sun is either always up or always down, the function will re-run with
  the latitude incremently adjusted to get closer to the equator until a sunset
  can be observed.

  See also `calculate-sun-events`."
  [lat lon date & {:keys [adjusted] :or {adjusted false}}]
  (let [sun ^SunTimes (calculate-sun-events lat lon date)
        always-up (.isAlwaysUp sun)
        always-down (.isAlwaysDown sun)
        sunset (.getSet sun)]
    (cond
      (or always-down always-up)
      (cond
        (< lat -65.7)
        (next-sunset (towards-polar-circle lat) lon date :adjusted true)
        (> lat 65.7)
        (next-sunset (towards-polar-circle lat) lon date :adjusted true)
        :else (throw (Exception. (str "Sun either always up or always down"
                                      " but latitude is: " lat))))
      ;; This seems to happen when the sunset occurred just before `date`.
      (nil? sunset)
      (next-sunset lat lon (go-forward (t/minutes 1) date) :adjusted adjusted)
      :else {:sunset (truncate-to-minutes sunset)
             :adjusted-for-polar-region adjusted
             :always-down always-down
             :always-up always-up
             :lat lat
             :lon lon
             :date date})))

(defn- next-start-of-day
  [lat lon date]
  (:sunset (next-sunset lat lon date)))

(defn- previous-start-of-day
  [lat lon date]
  (->> (next-start-of-day lat lon date)
       (go-back (t/hours 25))
       (next-start-of-day lat lon)))

(defn- noon
  [lat lon date]
  (let [tz (t/zone-id date)
        year (t/as date :year)
        month (t/as date :month-of-year)
        day (t/as date :day-of-month)
        morning (t/with-zone (t/zoned-date-time year month day 4 0) tz)]
    (.getNoon ^SunTimes (calculate-sun-events lat lon morning))))

(defn- boundaries-of-day
  [lat lon date]
  (let [p (previous-start-of-day lat lon date)
        n (next-start-of-day lat
                             lon
                             (noon lat lon (go-forward (t/hours 16) p)))]
    [p (go-back (t/seconds 1) n)]))

(defn calculate-new-moon
  "Given a java.time.ZonedDateTime object `date` return an
  org.shredzone.commons.suncalc.MoonPhase object describing the new moon
  following `date` using the same timezone as `date`.

  Example:
  (calculate-new-moon (make-zoned-date \"Asia/Jerusalem\" 2021 6 1 12))"
  [^ZonedDateTime date]
  (let [t (str (t/zone-id date))]
    (as-> (MoonPhase/compute) <>
      (.on ^MoonPhase$MoonPhaseBuilder <> date)
      (.timezone ^MoonPhase$MoonPhaseBuilder <> t)
      (.execute ^MoonPhase$MoonPhaseBuilder <>))))

(defn next-new-moon
  "Given a java.time.ZonedDateTime object `date` return a new
  java.time.ZonedDateTime object detailing the time of the new moon following
  `date` using the same timezone as `date`.

  See also `calculate-new-moon`.

  Example:
  (next-new-moon (make-zoned-date \"Asia/Jerusalem\" 2021 6 1 12))"
  [date]
  (truncate-to-minutes (.getTime ^MoonPhase (calculate-new-moon date))))

(defn- previous-new-moon
  [date]
  (->> (next-new-moon date)
       (go-back (t/days 31))
       (next-new-moon)))

(defn- next-start-of-month-in-israel
  [date]
  (let [zdate (zone-it jerusalem-tz date)
        lat jerusalem-lat
        lon jerusalem-lon
        previous-m (previous-new-moon zdate)
        day-following-previous-m (next-start-of-day lat lon previous-m)
        next-day (next-start-of-day lat lon zdate)]
    (if (zero? (t/as (t/duration day-following-previous-m next-day) :seconds))
      next-day
      (next-start-of-day lat lon (next-new-moon zdate)))))

(defn- previous-start-of-month-in-israel
  [date]
  (->> (next-start-of-month-in-israel date)
       (go-back (t/days 32))
       (next-start-of-month-in-israel)))

(defn- year-month-day [date] (t/as date :year :month-of-year :day-of-month))

(defn- next-start-of-month
  [lat lon date]
  (let [tz (t/zone-id date)
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
  (let [tz (t/zone-id date)
        year (Integer/parseInt (str (t/year date)))
        same-year-march-equinox (time-of-march-equinox year)]
    (if (t/before? date same-year-march-equinox)
      (zone-it tz same-year-march-equinox)
      (zone-it tz (time-of-march-equinox (inc year))))))

(defn- previous-march-equinox
  [date]
  (let [tz (t/zone-id date)
        year (Integer/parseInt (str (t/year date)))
        same-year-march-equinox (time-of-march-equinox year)]
    (if (t/before? same-year-march-equinox date)
      (zone-it tz same-year-march-equinox)
      (zone-it tz (time-of-march-equinox (dec year))))))

(defn- next-start-of-year-in-israel
  [date]
  (let [zdate (zone-it jerusalem-tz date)
        this-year-march-equinox (time-of-march-equinox (t/as zdate :year))
        previous-month (previous-start-of-month-in-israel zdate)
        pme (previous-march-equinox zdate)
        moon-following-previous-equinox (next-new-moon pme)
        day-following-moon (next-start-of-day jerusalem-lat
                                              jerusalem-lon
                                              moon-following-previous-equinox)
        next-day (next-start-of-day jerusalem-lat jerusalem-lon zdate)
        potential-new-year (next-start-of-month-in-israel (next-march-equinox
                                                            zdate))]
    (cond (zero? (t/as (t/duration day-following-moon next-day) :seconds))
          next-day
          (t/before? previous-month this-year-march-equinox zdate)
          (next-start-of-month-in-israel zdate)
          :else potential-new-year)))

(defn- previous-start-of-year-in-israel
  [date]
  (let [y (t/as date :year)
        m (t/as date :month-of-year)]
    (cond (> m 4) (next-start-of-year-in-israel
                    (make-zoned-date jerusalem-tz y 2 1))
          (< m 3) (next-start-of-year-in-israel
                    (make-zoned-date jerusalem-tz (dec y) 2 1))
          :else (->> (next-start-of-year-in-israel date)
                     (go-back (t/days 400))
                     (next-start-of-year-in-israel)))))

(defn- next-start-of-year
  [lat lon date]
  (let [tz (t/zone-id date)
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
        tz (t/zone-id date)]
    (cond (> m 4) (next-start-of-year lat lon (make-zoned-date tz y 2 1))
          (< m 3) (next-start-of-year lat lon (make-zoned-date tz (dec y) 2 1))
          :else (->> (next-start-of-year lat lon date)
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
                  (go-back (t/hours 6))
                  (next-start-of-day lat lon)))
       (dedupe)
       ;; Since the day sometimes starts after local midnight, due to DST
       (filter #(or (and (t/saturday? %) (>= (t/as % :hour-of-day) 2))
                    (and (t/sunday? %) (< (t/as % :hour-of-day) 2))))
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

(defn- hebrew-months-in-year
  [lat lon date]
  (count (start-of-months-in-year lat lon date)))

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
                    (go-back (t/hours 6))
                    (next-start-of-day lat lon)))
         (dedupe)
         (filter #(t/before? % end-of-month))
         (cons start-of-month))))

(defn- hebrew-days-in-month
  [lat lon date]
  (count (start-of-days-in-month lat lon date)))

(defn- hebrew-month-of-year
  [lat lon date]
  (let [p (t/as (previous-start-of-month lat lon date) :julian-day)]
    (->> (start-of-months-in-year lat lon date)
         (map #(t/as % :julian-day))
         (apply sorted-set)
         (keep-indexed #(when (= %2 p) %1))
         (first)
         (inc))))

(defn- hebrew-day-of-month
  [lat lon date]
  (let [p (previous-start-of-day lat lon date)
        pj (t/as p :julian-day)
        m (start-of-days-in-month lat lon date)]
    (->> (map #(t/as % :julian-day) m)
         (apply sorted-set)
         (keep-indexed #(when (= %2 pj) %1))
         (first)
         (inc))))

(defn- hebrew-day-of-week
  [lat lon date]
  (let [p (previous-start-of-day lat lon date)
        pj (t/as p :julian-day)]
    (->> (start-of-days-in-week lat lon date)
         (map #(t/as % :julian-day))
         (apply sorted-set)
         (keep-indexed #(when (= %2 pj) %1))
         (first)
         (inc))))

(def trad-month-names
  ["Nisan" "Iyar" "Sivan" "Tammuz" "Av" "Elul" "Tishrei" "Marcheshvan" "Kislev"
   "Tevet" "Shevat" "Adar" "Adar II"])

(def month-numbers
  (vec (flatten ["1st" "2nd" "3rd" (map #(str % "th") (range 4 14))])))

(def day-numbers
  (vec (flatten ["1st" "2nd" "3rd" (map #(str % "th") (range 4 21))
                 "21st" "22nd" "23rd" (map #(str % "th") (range 24 31))])))

(def weekday-names
  (conj (mapv #(str % " day of the week") ["1st" "2nd" "3rd" "4th" "5th" "6th"])
        "Sabbath"))

(defn- single-day-feast [feast] (assoc feast :day-of-feast 1, :days-in-feast 1))

(defn- multi-day-feast
  [day-of-feast days-in-feast feast]
  (assoc feast :day-of-feast day-of-feast, :days-in-feast days-in-feast))

(defn- rosh-chodesh
  "Return a string representing the 1st day of the `m` month of the year."
  [m]
  {:pre [(< 0 m 14)]}
  (single-day-feast
   {:name (str (day-numbers 0) " day of the " (month-numbers (dec m)) " month")
    :hebrew-name (str "Rosh Chodesh " (trad-month-names (dec m)))}))

(def pesach (single-day-feast {:name "Passover", :hebrew-name "Pesach"}))

(defn- ha-matzot
  [day-of-feast]
  (multi-day-feast day-of-feast 7 {:name "Feast of Unleavened Bread"
                                   :hebrew-name "Chag Ha-Matzot"}))

(def yom-bikkurim
  (single-day-feast {:name "Feast of First Fruits"
                     :hebrew-name "Yom Bikkurim"}))

(def shavuot
  (single-day-feast {:name "Feast of Weeks"
                     :alternative-name "Feast of Pentecost"
                     :hebrew-name "Shavuot"}))

(defn- ha-sukkot
  [day-of-feast]
  (multi-day-feast day-of-feast 7 {:name "Feast of Tabernacles"
                                   :alternative-name "Feast of Booths"
                                   :hebrew-name "Chag Ha-Sukkot"}))

(def yom-teruah
  (single-day-feast {:name "Feast of Trumpets", :hebrew-name "Yom Teruah"}))

(def yom-ha-kippurim
  (single-day-feast {:name "Day of Atonement"
                     :hebrew-name "Yom Ha-Kippurim"
                     :alternative-hebrew-name "Yom Kippur"}))

(def shemini-atzeret
  (single-day-feast {:name "The Last Great Day"
                     :hebrew-name "Shemini Atzeret"}))

(defn- chanukah
  [day-of-feast]
  (multi-day-feast day-of-feast 8 {:name "Hanukkah", :hebrew-name "Chanukah"}))

(def purim
  (multi-day-feast 1 2 {:name "Purim", :hebrew-name "Purim"}))

(def shushan-purim
  (multi-day-feast 2 2 {:name "Shushan Purim", :hebrew-name "Shushan Purim"}))

(defn- minor-feast-day?
  "Given `m` (hebrew month of year), `d` (hebrew day of month), return a map
  with details of any minor feast day on that day, or return false if there are
  none."
  [m d]
  (cond
    (= d 1) (rosh-chodesh m)
    :else false))

(defn- days-in-first-month
  [start-of-year]
  (->> (go-forward (t/days 2) start-of-year)
       (zone-it "Asia/Jerusalem")
       (hebrew-days-in-month jerusalem-lat jerusalem-lon)))

(defn- days-in-prev-month
  [start-of-month]
  (->> (go-back (t/days 2) start-of-month)
       (zone-it "Asia/Jerusalem")
       (hebrew-days-in-month jerusalem-lat jerusalem-lon)))

(defn- major-feast-day?
  "Given `m` (hebrew month of year), `d` (hebrew day of month), and `dow`
  (hebrew day of week), return a map with details of any major feast day on that
  day, or return false if there are none."
  [m d dow start-of-year start-of-month]
  (let [days-in-first-month
        (when (and (= m 3) (<= 5 d 12))
          (days-in-first-month start-of-year))
        days-in-prev-month
        (when (or (and (= m 3) (<= 5 d 12))
                  (and (= m 10) (< 0 d 4)))
          (days-in-prev-month start-of-month))
        two-first-months
        (when (and days-in-first-month days-in-prev-month)
          (+ days-in-first-month days-in-prev-month))]
    (cond
      (and (= m 1) (= d 14)) pesach
      (and (= m 1) (<= 15 d 21) (= dow 1)) yom-bikkurim
      (and (= m 1) (= d 15)) (ha-matzot 1)
      (and (= m 1) (= d 16)) (ha-matzot 2)
      (and (= m 1) (= d 17)) (ha-matzot 3)
      (and (= m 1) (= d 18)) (ha-matzot 4)
      (and (= m 1) (= d 19)) (ha-matzot 5)
      (and (= m 1) (= d 20)) (ha-matzot 6)
      (and (= m 1) (= d 21)) (ha-matzot 7)
      (and (= two-first-months 58) (= m 3) (<= 6 d 12) (= dow 1)) shavuot
      (and (= two-first-months 59) (= m 3) (<= 5 d 11) (= dow 1)) shavuot
      (and (= two-first-months 60) (= m 3) (<= 4 d 10) (= dow 1)) shavuot
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
      :else false)))

(defn- sabbath?
  "Given the hebrew `month-of-year`, `day-of-month`, `day-of-week`, and
  `feast-days`,  return `true` if it's a Sabbath or `false` if it's not."
  [month-of-year day-of-month day-of-week major-feast-day]
  (or (= 7 day-of-week)
      (and (= month-of-year 1) (= day-of-month 15)) ; "First day of the Feast of
                                                    ;  Unleavened Bread"
      (and (= month-of-year 1) (= day-of-month 21)) ; "Last day of the Feast of
                                                    ;  Unleavened Bread"
      (and (= month-of-year 7) (= day-of-month 1))  ; "Feast of Trumpets"
      (and (= month-of-year 7) (= day-of-month 10)) ; "Day of Atonement"
      (and (= month-of-year 7) (= day-of-month 15)) ; "First day of the Feast of
                                                    ;  Tabernacles"
      (and (= month-of-year 7) (= day-of-month 22)) ; "The Last Great Day"
      (and major-feast-day (= "Feast of First Fruits" (:name major-feast-day)))
      (and major-feast-day (= "Feast of Weeks" (:name major-feast-day)))))

(defn- hebrew-names
  [month-of-year months-in-year day-of-month day-of-week]
  {:month-of-year (month-numbers (dec month-of-year))
   :traditional-month-of-year
     (if (and (= month-of-year 12) (= months-in-year 13))
       "Adar I"
       (trad-month-names (dec month-of-year)))
   :day-of-month (day-numbers (dec day-of-month))
   :day-of-week (weekday-names (dec day-of-week))})

(defn- hebrew-date-map
  [lat lon y m date]
  (let [months-in-y (hebrew-months-in-year lat lon date)
        moy (hebrew-month-of-year lat lon date)
        dom (hebrew-day-of-month lat lon date)
        dow (hebrew-day-of-week lat lon date)
        major-feast-day (major-feast-day? moy dom dow (first y) (first m))]
    {:month-of-year moy
     :months-in-year months-in-y
     :day-of-month dom
     :days-in-month (hebrew-days-in-month lat lon date)
     :day-of-week dow
     :sabbath (sabbath? moy dom dow major-feast-day)
     :minor-feast-day (minor-feast-day? moy dom)
     :major-feast-day major-feast-day
     :names (hebrew-names moy months-in-y dom dow)}))

(defn- polar-adjusted?
  [lat lon date]
  (if (<= -65 lat 65)
    false
    (->> (go-back (t/hours 2) date)
         (next-sunset lat lon)
         (:adjusted-for-polar-region))))

(defn- assoc-polar-status
  [lat lon m]
  (assoc m
    :start-adjusted-for-polar-region (polar-adjusted? lat lon (:start m))
    :end-adjusted-for-polar-region (polar-adjusted? lat lon (:end m))))

(defn- with-polar-status
  [lat lon m]
  (into (empty m) (for [[k v] m] [k (assoc-polar-status lat lon v)])))

(defn- hebrew-time-map
  [lat lon y m w d]
  (with-polar-status lat lon {:year {:start (first y) :end (last y)}
                              :month {:start (first m) :end (last m)}
                              :week {:start (first w) :end (last w)}
                              :day {:start (first d) :end (last d)}}))

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

  See also `zone-it`, `hebrew-date-map`, `hebrew-time-map`, and `now`."
  ([lat lon date]
   {:pre [(and (number? lat) (<= -90 lat 90))
          (and (number? lon) (<= -180 lon 180))
          (t/zoned-date-time? date)
          (<= 1584 (t/as date :year) 2100)]}
   (let [y (boundaries-of-year lat lon date)
         m (boundaries-of-month lat lon date)
         w (boundaries-of-week lat lon date)
         d (boundaries-of-day lat lon date)]
     {:hebrew (hebrew-date-map lat lon y m date)
      :time (hebrew-time-map lat lon y m w d)}))
  ([lat lon] (hebrew-date lat lon (now)))
  ([date] (hebrew-date jerusalem-lat jerusalem-lon date))
  ([] (hebrew-date (zone-it jerusalem-tz (now)))))

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
   {:pre [(t/zoned-date-time? date)
          (and (pos-int? m) (< 0 m 14)) (and (pos-int? d) (< 0 d 31))]}
   (try
     (let [months (start-of-months-in-year lat lon date)
           start-of-month (nth months (dec m))
           days (when start-of-month
                  (->> start-of-month
                       (go-forward (t/hours 1))
                       (start-of-days-in-month lat lon)))]
          (when days
            (->> (dec d)
                 (nth days)
                 (go-forward (t/hours 1))
                 (hebrew-date lat lon))))
     (catch IndexOutOfBoundsException _e nil)))
  ([m d date] (find-date jerusalem-lat jerusalem-lon m d date))
  ([m d] (find-date m d (zone-it jerusalem-tz (now)))))

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
   {:pre [(string? tz) (and (pos-int? y) (<= 1584 y 2100))
          (and (pos-int? m) (< 0 m 14)) (and (pos-int? d) (< 0 d 31))]}
   (find-date lat lon m d (make-zoned-date tz y 6 1 12)))
  ([tz y m d] (find-date-in-year jerusalem-lat jerusalem-lon tz y m d))
  ([y m d] (find-date-in-year jerusalem-tz y m d)))

;; The following functions were used to calculate the feast days for
;; `calculated-feast-days`. They're terribly slow and shouldn't be used
;; for any other purpose. I'm leaving them here for future reference and if I
;; have to re-generate the dates due to a change in the calculation method or
;; future bug fix.
(def potential-feast-dates
  (->> (concat (map #(vector 1 %) (range 14 22))
               (map #(vector 3 %) (range 5 13))
               [[7 1] [7 10]]
               (map #(vector 7 %) (range 15 23))
               (map #(vector 9 %) (range 25 31))
               (map #(vector 10 %) (range 1 4))
               [[12 14] [12 15]]
               (map #(vector % 1) (range 1 14)))
       (sort)
       (dedupe)))

(defn- calculate-feast-days-in-gregorian-year
  [year]
  (->> (for [y [(dec year) year]
             p potential-feast-dates]
         (find-date-in-year y (first p) (last p)))
       (filter #(or (get-in % [:hebrew :minor-feast-day])
                    (get-in % [:hebrew :major-feast-day])))
       (pmap #(vector (t/as (get-in % [:time :day :start])
                            :year
                            :month-of-year
                            :day-of-month)
                      (get-in % [:hebrew :minor-feast-day])
                      (get-in % [:hebrew :major-feast-day])))
       (map #(remove false? %))
       (filter #(= (ffirst %) year))
       (sort-by #(second (first %)))))

(defn- map-of-feast-days-in-gregorian-year
  [year]
  (let [dates (calculate-feast-days-in-gregorian-year year)]
    (->> (for [d dates]
           {(second (first d)) {(last (first d)) (vec (rest d))}})
         (apply merge-with into)
         (hash-map year))))

(defn- iso-date
  [y m d]
  (str y "-" (format "%02d" m) "-" (format "%02d" d)))

(defn- long-feast-day-name
  [y m d n day-of-feast days-in-feast]
  (str (iso-date y m d) " "
       (if (< days-in-feast 3)
         n
         (if (= days-in-feast 8)
           (str (day-numbers (dec day-of-feast)) " day of " n)
           (str (day-numbers (dec day-of-feast)) " day of the " n)))))

(defn list-of-known-feast-days-in-gregorian-year
  "Given a gregorian `year` between 1584 and 2100, return a list of strings
  describing the feast days in that year. The dates represent the gregorian day
  on which the sunset would begin the feast day in question. Some days will have
  more than one feast day.

  Note: The years 2020-2039 are pre-calculated and defined in
  `calculated-feast-days`, which will be used for years that it contains. If the
  `year` is outside of this range there will be quite some computational time
  added."
  [year]
  (let [y (or (calculated-feast-days year)
              (map-of-feast-days-in-gregorian-year year))]
    (sort (for [m (keys y)
                d (keys (y m))
                f (get-in y [m d])]
            (let [n (:name f)
                  day-of-feast (:day-of-feast f)
                  days-in-feast (:days-in-feast f)]
              (long-feast-day-name year m d n day-of-feast days-in-feast))))))
