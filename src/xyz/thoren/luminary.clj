(ns xyz.thoren.luminary
  (:require
   [tick.core :as tick]
   [xyz.thoren.equinox :refer [march-equinox]]
   [xyz.thoren.luminary-feast-data :as fd])
  (:import
   (org.shredzone.commons.suncalc SunTimes
                                  SunTimes$SunTimesBuilder
                                  MoonPhase
                                  MoonPhase$MoonPhaseBuilder)
   (java.time ZonedDateTime)))

(def jerusalem-lat
  "The Latitude of the Temple Mount in Jerusalem, Israel."
  31.7781161)

(def jerusalem-lon
  "The Longitude of the Temple Mount in Jerusalem, Israel."
  35.233804)

(def jerusalem-zone
  "A string representing the ZoneRegion of Jerusalem, Israel."
  "Asia/Jerusalem")

(def feast-days
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

(defn valid-zone?
  "Given a string `s`, test if it can be transformed into a valid ZoneRegion.
  Passing a ZoneRegion object will also return true."
  [s]
  (tick/zone? (try (tick/zone s) (catch Exception _e nil))))

(defn in-zone
  "Given a valid timezone `z` (either a string or a ZoneRegion object),
  and a ZonedDateTime object `t`, convert `t` to the same instant in `z`."
  [z t]
  {:pre [(valid-zone? z) (tick/zoned-date-time? t)]}
  (tick/in t z))

(defn truncate-to-minutes
  [t]
  (let [z (tick/zone t)]
    (in-zone z (tick/zoned-date-time (tick/truncate (tick/instant t) :minutes)))))

(defn now
  "Return the current time using the system timezone."
  []
  (tick/zoned-date-time))

(defn zdt
  "Given a string containing a valid timezone name `z`, and at least 3 integers,
  return a ZonedDateTime object. Accepts between 3 and 7 integer arguments
  representing year, month, day, hour, minute, second, nanos."
  [z & more]
  {:pre [(valid-zone? z)
         (empty? (remove int? more))
         (<= 3 (count more) 7)]}
  (let [d-args (take 3 more)
        t-args (drop 3 more)
        d (apply tick/new-date d-args)
        t (apply tick/new-time (if (>= (count t-args) 2) t-args [0 0]))]
    (tick/in (tick/at d t) z)))

(defn utc-zdt
  "Given at least 3 integers, return a ZonedDateTime object in the UTC timezone.
  Accepts between 3 and 7 arguments representing year, month, day, hour, minute,
  second, nanos."
  [& more]
  (apply zdt (cons "UTC" more)))

(defn go-back
  "Subtract `n` `unit` from `t`.

  Example: (go-back 5 :hours (now))"
  [n unit t]
  (tick/<< t (tick/new-duration n unit)))

(defn go-forward
  "Add `n` `unit` to `t`.

  Example: (go-forward 5 :hours (now))"
  [n unit t]
  (tick/>> t (tick/new-duration n unit)))

(defn sun-events
  "Given `lat` and `lon` for a location, and a ZonedDateTime object `t` in the
  timezone that corresponds to that location, return an
  org.shredzone.commons.suncalc.SunTimes object describing the sun events
  following `t`.

  Example:
  (sun-events 31 35 (zdt \"Asia/Jerusalem\" 2021 6 1 12))"
  [lat lon ^ZonedDateTime t]
  (let [z (str (tick/zone t))]
    (as-> (SunTimes/compute) <>
      (.on ^SunTimes$SunTimesBuilder <> t)
      (.at ^SunTimes$SunTimesBuilder <> lat lon)
      (.oneDay ^SunTimes$SunTimesBuilder <>)
      (.timezone ^SunTimes$SunTimesBuilder <> z)
      (.execute ^SunTimes$SunTimesBuilder <>))))

(defn- bigdec-
  [x y]
  (float (- (bigdec x) (bigdec y))))

(defn- bigdec+
  [x y]
  (float (+ (bigdec x) (bigdec y))))

(defn- nudge-lat
  "Nudge `lat` closer toward the nearest polar circle."
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
  "Given `lat` and `lon` for a location, and a ZonedDateTime object `t` in the
  timezone that corresponds to that location, return a map containing the next
  sunset following `t`.

  If the sun is either always up or always down, the function will re-run with
  the latitude incremently adjusted to get closer to the equator until a sunset
  can be observed.

  See also `sun-events`."
  [lat lon t & {:keys [adjusted] :or {adjusted false}}]
  (let [sun ^SunTimes (sun-events lat lon t)
        always-up (.isAlwaysUp sun)
        always-down (.isAlwaysDown sun)
        sunset (.getSet sun)]
    (cond
      (or always-down always-up)
      (cond
        (< lat -65.7)
        (next-sunset (nudge-lat lat) lon t :adjusted true)
        (> lat 65.7)
        (next-sunset (nudge-lat lat) lon t :adjusted true)
        :else (throw (Exception. (str "Sun either always up or always down"
                                      " but latitude is: " lat))))
      ;; This seems to happen when the sunset occurred just before `t`.
      (nil? sunset)
      (next-sunset lat lon (go-forward 1 :minutes t) :adjusted adjusted)
      :else {:sunset (truncate-to-minutes sunset)
             :adjusted-for-polar-region adjusted
             :always-down always-down
             :always-up always-up
             :lat lat
             :lon lon
             :time t})))

(defn- next-start-of-day
  [lat lon t]
  (:sunset (next-sunset lat lon t)))

(defn- previous-start-of-day
  [lat lon t]
  (->> (next-start-of-day lat lon t)
       (go-back 25 :hours)
       (next-start-of-day lat lon)))

(defn- next-noon
  [lat lon t]
  (let [year (tick/int (tick/year t))
        month (tick/int (tick/month t))
        day (tick/day-of-month t)
        morning (zdt (tick/zone t) year month day 4 0)]
    (.getNoon ^SunTimes (sun-events lat lon morning))))

(defn- boundaries-of-day
  [lat lon t]
  (let [p (previous-start-of-day lat lon t)
        n (next-start-of-day lat
                             lon
                             (next-noon lat lon (go-forward 16 :hours p)))]
    [p (go-back 1 :seconds n)]))

(defn new-moon
  "Given a ZonedDateTime object `t` return an
  org.shredzone.commons.suncalc.MoonPhase object describing the new moon
  following `t` using the same timezone as `t`.

  Example:
  (new-moon (zdt \"Asia/Jerusalem\" 2021 6 1 12))"
  [^ZonedDateTime t]
  (let [z (str (tick/zone t))]
    (as-> (MoonPhase/compute) <>
      (.on ^MoonPhase$MoonPhaseBuilder <> t)
      (.timezone ^MoonPhase$MoonPhaseBuilder <> z)
      (.execute ^MoonPhase$MoonPhaseBuilder <>))))

(defn next-new-moon
  "Given a ZonedDateTime object `t` return a new ZonedDateTime object detailing
  the time of the new moon following `t` using the same timezone as `t`.

  See also `new-moon`.

  Example:
  (next-new-moon (zdt \"Asia/Jerusalem\" 2021 6 1 12))"
  [t]
  (truncate-to-minutes (.getTime ^MoonPhase (new-moon t))))

(defn- previous-new-moon
  [t]
  (->> (next-new-moon t)
       (go-back 31 :days)
       (next-new-moon)))

(defn- next-start-of-month-in-israel
  [t]
  (let [zdate (in-zone jerusalem-zone t)
        lat jerusalem-lat
        lon jerusalem-lon
        previous-m (previous-new-moon zdate)
        day-following-previous-m (next-start-of-day lat lon previous-m)
        next-day (next-start-of-day lat lon zdate)]
    (if (zero? (tick/seconds (tick/between day-following-previous-m next-day)))
      next-day
      (next-start-of-day lat lon (next-new-moon zdate)))))

(defn- previous-start-of-month-in-israel
  [t]
  (->> (next-start-of-month-in-israel t)
       (go-back 32 :days)
       (next-start-of-month-in-israel)))

(defn- year-month-day
  [t]
  [(tick/int (tick/year t))
   (tick/int (tick/month t))
   (tick/day-of-month t)])

(defn- next-start-of-month
  [lat lon t]
  (let [prev-month-israel (previous-start-of-month-in-israel t)
        next-month-israel (next-start-of-month-in-israel t)
        next-day (next-start-of-day lat lon t)]
    (if (= (year-month-day prev-month-israel) (year-month-day next-day))
      next-day
      (as-> (year-month-day next-month-israel) <>
        (zdt (tick/zone t) (first <>) (second <>) (last <>))
        (next-noon lat lon <>)
        (next-start-of-day lat lon <>)))))

(defn- previous-start-of-month
  [lat lon t]
  (->> (next-start-of-month lat lon t)
       (go-back 32 :days)
       (next-start-of-month lat lon)))

(defn- boundaries-of-month
  [lat lon t]
  (let [p (previous-start-of-month lat lon t)
        n (next-start-of-month lat lon (go-forward 2 :days p))]
    [p (go-back 1 :seconds n)]))

(defn march-equinox-of
  "Given the year `y`, return the time of the March Equinox of that year as a
  java.time.ZonedDateTime object in UTC."
  [y]
  (->> [:year :month :day :hour :minute :second]
       (map #(% (march-equinox y)))
       (apply utc-zdt)))

(defn- next-march-equinox
  [t]
  (let [z (tick/zone t)
        y (tick/int (tick/year t))
        same-year-march-equinox (march-equinox-of y)]
    (if (tick/< t same-year-march-equinox)
      (in-zone z same-year-march-equinox)
      (in-zone z (march-equinox-of (inc y))))))

(defn- previous-march-equinox
  [t]
  (->> (next-march-equinox t)
       (go-back 366 :days)
       (next-march-equinox)))

(defn- next-start-of-year-in-israel
  [t]
  (let [zdate (in-zone jerusalem-zone t)
        this-year-march-equinox (march-equinox-of (tick/int (tick/year zdate)))
        previous-month (previous-start-of-month-in-israel zdate)
        pme (previous-march-equinox zdate)
        moon-following-previous-equinox (next-new-moon pme)
        day-following-moon (next-start-of-day jerusalem-lat
                                              jerusalem-lon
                                              moon-following-previous-equinox)
        next-day (next-start-of-day jerusalem-lat jerusalem-lon zdate)
        potential-new-year (next-start-of-month-in-israel (next-march-equinox
                                                            zdate))]
    (cond (zero? (tick/seconds (tick/between day-following-moon next-day)))
          next-day
          (tick/< previous-month this-year-march-equinox zdate)
          (next-start-of-month-in-israel zdate)
          :else potential-new-year)))

(defn- previous-start-of-year-in-israel
  [t]
  (let [y (tick/int (tick/year t))
        m (tick/int (tick/month t))]
    (cond (> m 4) (next-start-of-year-in-israel
                    (zdt jerusalem-zone y 2 1))
          (< m 3) (next-start-of-year-in-israel
                    (zdt jerusalem-zone (dec y) 2 1))
          :else (->> (next-start-of-year-in-israel t)
                     (go-back 400 :days)
                     (next-start-of-year-in-israel)))))

(defn- next-start-of-year
  [lat lon t]
  (let [prev-year-israel (previous-start-of-year-in-israel t)
        next-year-israel (next-start-of-year-in-israel t)
        next-day (next-start-of-day lat lon t)]
    (if (= (year-month-day prev-year-israel) (year-month-day next-day))
      next-day
      (as-> (year-month-day next-year-israel) <>
        (zdt (tick/zone t) (first <>) (second <>) (last <>))
        (next-noon lat lon <>)
        (next-start-of-day lat lon <>)))))

(defn- previous-start-of-year
  [lat lon t]
  (let [y (tick/int (tick/year t))
        m (tick/int (tick/month t))
        z (tick/zone t)]
    (cond (> m 4) (next-start-of-year lat lon (zdt z y 2 1))
          (< m 3) (next-start-of-year lat lon (zdt z (dec y) 2 1))
          :else (->> (next-start-of-year lat lon t)
                     (go-back 400 :days)
                     (next-start-of-year lat lon)))))

(defn- boundaries-of-year
  [lat lon t]
  (let [p (previous-start-of-year lat lon t)
        n (next-start-of-year lat lon (go-forward 1 :days p))]
    [p (go-back 1 :seconds n)]))

(defn- saturday?
  [t]
  (= 6 (tick/int (tick/day-of-week t))))

(defn- sunday?
  [t]
  (= 7 (tick/int (tick/day-of-week t))))

(defn- next-start-of-week
  [lat lon t]
  (->> (range 0 8)
       (map #(->> (next-start-of-day lat lon t)
                  (go-forward % :days)
                  (go-back 8 :hours)
                  (next-noon lat lon)
                  (go-back 6 :hours)
                  (next-start-of-day lat lon)))
       (distinct)
       ;; Since the day sometimes starts after local midnight, due to DST
       (filter #(or (and (saturday? %) (>= (tick/hour %) 2))
                    (and (sunday? %) (< (tick/hour %) 2))))
       (first)))

(defn- previous-start-of-week
  [lat lon t]
  (->> (next-start-of-week lat lon t)
       (go-back 10 :days)
       (next-start-of-week lat lon)))

(defn- boundaries-of-week
  [lat lon t]
  (let [p (previous-start-of-week lat lon t)
        n (next-start-of-week lat lon (go-forward 1 :days p))]
    [p (go-back 1 :seconds n)]))

(defn- start-of-days-in-week
  [lat lon t]
  (let [w (boundaries-of-week lat lon t)]
    (->> (range 1 8)
         (map #(->> (first w)
                    (go-forward % :days)
                    (go-back 8 :hours)
                    (next-noon lat lon)
                    (next-start-of-day lat lon)))
         (distinct)
         (filter #(tick/< % (second w)))
         (cons (first w)))))

(defn- new-moons-since-start-of-year
  [lat lon t]
  (let [y (boundaries-of-year lat lon t)
        start-of-year (first y)
        end-of-year (second y)]
    (->> (range 0 14)
         (map #(next-new-moon (go-forward (* % 29) :days start-of-year)))
         (distinct)
         (filter #(tick/< % end-of-year))
         (drop-last))))

(defn- start-of-months-in-year
  [lat lon t]
  (->> (new-moons-since-start-of-year lat lon t)
       (map #(next-start-of-month lat lon (go-back 1 :hours %)))
       (cons (previous-start-of-year lat lon t))))

(defn- hebrew-months-in-year
  [lat lon t]
  (count (start-of-months-in-year lat lon t)))

(defn- start-of-days-in-month
  [lat lon t]
  (let [m (boundaries-of-month lat lon t)
        start-of-month (first m)
        end-of-month (second m)]
    (->> (range 1 32)
         (map #(->> start-of-month
                    (go-forward % :days)
                    (go-back 2 :hours)
                    (next-noon lat lon)
                    (go-back 2 :hours)
                    (next-start-of-day lat lon)))
         (distinct)
         (filter #(tick/< % end-of-month))
         (cons start-of-month))))

(defn- hebrew-days-in-month
  [lat lon t]
  (count (start-of-days-in-month lat lon t)))

(defn- hebrew-month-of-year
  [lat lon t]
  (let [p (tick/date (previous-start-of-month lat lon t))]
    (->> (start-of-months-in-year lat lon t)
         (map tick/date)
         (apply sorted-set)
         (keep-indexed #(when (= %2 p) %1))
         (first)
         (inc))))

(defn- hebrew-day-of-month
  [lat lon t]
  (let [p (tick/date (previous-start-of-day lat lon t))
        m (start-of-days-in-month lat lon t)]
    (->> (map #(tick/date %) m)
         (apply sorted-set)
         (keep-indexed #(when (= %2 p) %1))
         (first)
         (inc))))

(defn- hebrew-day-of-week
  [lat lon t]
  (let [p (tick/date (previous-start-of-day lat lon t))]
    (->> (start-of-days-in-week lat lon t)
         (map #(tick/date %))
         (apply sorted-set)
         (keep-indexed #(when (= %2 p) %1))
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

(defn- single-day-feast [m] (assoc m :day-of-feast 1, :days-in-feast 1))

(defn- multi-day-feast
  [day-of-feast days-in-feast m]
  (assoc m :day-of-feast day-of-feast, :days-in-feast days-in-feast))

(defn- rosh-chodesh
  "Return a string representing the 1st day of the `n` month of the year."
  [n]
  {:pre [(< 0 n 14)]}
  (single-day-feast
   {:name (str (day-numbers 0) " day of the " (month-numbers (dec n)) " month")
    :hebrew-name (str "Rosh Chodesh " (trad-month-names (dec n)))}))

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

(defn- minor-feast-day
  "Given the hebrew `month-of-year` and the hebrew `day-of-month`, return a map
  with details of any minor feast day on that day, or return false if there are
  none."
  [month-of-year day-of-month]
  (cond
    (= day-of-month 1) (rosh-chodesh month-of-year)
    :else false))

(defn- days-in-first-month
  [start-of-year]
  (->> (go-forward 2 :days start-of-year)
       (in-zone "Asia/Jerusalem")
       (hebrew-days-in-month jerusalem-lat jerusalem-lon)))

(defn- days-in-prev-month
  [start-of-month]
  (->> (go-back 2 :days start-of-month)
       (in-zone "Asia/Jerusalem")
       (hebrew-days-in-month jerusalem-lat jerusalem-lon)))

(defn- major-feast-day
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

(defn- hebrew-date
  [lat lon y m t]
  (let [months-in-y (hebrew-months-in-year lat lon t)
        moy (hebrew-month-of-year lat lon t)
        dom (hebrew-day-of-month lat lon t)
        dow (hebrew-day-of-week lat lon t)
        major-feast-day (major-feast-day moy dom dow (first y) (first m))]
    {:month-of-year moy
     :months-in-year months-in-y
     :day-of-month dom
     :days-in-month (hebrew-days-in-month lat lon t)
     :day-of-week dow
     :sabbath (sabbath? moy dom dow major-feast-day)
     :minor-feast-day (minor-feast-day moy dom)
     :major-feast-day major-feast-day
     :names (hebrew-names moy months-in-y dom dow)}))

(defn- polar-adjusted?
  [lat lon t]
  (if (<= -65 lat 65)
    false
    (->> (go-back 2 :hours t)
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

(defn- time-date
  [lat lon year month week day]
  (with-polar-status lat lon {:year {:start (first year) :end (last year)}
                              :month {:start (first month) :end (last month)}
                              :week {:start (first week) :end (last week)}
                              :day {:start (first day) :end (last day)}}))

(defn date
  "Return a map containing the details of a hebrew `date` where:
  `lat` is the latitude of the location,
  `lon` is the longitude of the location, and
  `t` is a ZonedDateTime object representing the time for which the `date` will
  be calculated.

  If only `t` is provided `lat` and `lon` will default to the coordinates of
  the Temple Mount in Jerusalem.

  In addition, if only `lat`, and `lon` are provided `t` will default to the
  current time using the system timezone.

  Example:
  (date 59.3325800 18.0649000 (in-zone \"Europe/Stockholm\" (now)))

  The above will look for the current hebrew date. The coordinates are those of
  Stockholm, Sweden. Using the `in-zone` function to make sure that `now` is
  returned in the correct timezone is recommended, in this case
  \"Europe/Stockholm\".

  Caution: Make sure that `t` is using the actual timezone of the location at
  the provided coordinates. Otherwise the results may not even be produced, or
  they will be inaccurate. Calculating the timezone of a given location is out
  of scope of this library.

  See also `in-zone`, `hebrew-date`, `time-date`, and `now`."
  ([] (date (in-zone jerusalem-zone (now))))
  ([t] (date jerusalem-lat jerusalem-lon t))
  ([lat lon] (date lat lon (now)))
  ([lat lon t]
   {:pre [(and (number? lat) (<= -90 lat 90))
          (and (number? lon) (<= -180 lon 180))
          (tick/zoned-date-time? t)
          (<= 1584 (tick/int (tick/year t)) 2100)]}
   (let [y (boundaries-of-year lat lon t)
         m (boundaries-of-month lat lon t)
         w (boundaries-of-week lat lon t)
         d (boundaries-of-day lat lon t)]
     {:hebrew (hebrew-date lat lon y m t)
      :time (time-date lat lon y m w d)})))

(defn lookup-date
  "Return a map containing the details of a `date` where:
  `lat` is the latitude of the location,
  `lon` is the longitude of the location,
  `m` is the hebrew month of year,
  `d` is the hebrew day of month that you are looking for, and
  `t` is a ZonedDateTime object from which the beginning of the hebrew year will
  be calculated. I.e, it will use the 'current' year of the `t` as the base for
  calculating `m` and `d`.

  If only `m`, `d`, and `t` are provided `lat` and `lon` will default to
  the coordinates of the Temple Mount in Jerusalem.

  In addition, if only `m`, and `d` is provided `t` will default to the
  current time in the \"Asia/Jerusalem\" timezone.

  Example:
  (lookup-date 59.3325800 18.0649000 1 14 (in-zone \"Europe/Stockholm\" (now)))

  The above will look for the 14th day of the 1st month in the hebrew year that
  starts in the current gregorian year (based on the system time and timezone.
  The coordinates are those of Stockholm, Sweden. Using the `in-zone` function
  to make sure that `now` is returned in the correct timezone is recommended,
  in this case \"Europe/Stockholm\".

  Caution: Make sure that `t` is using the actual timezone of the location at
  the provided coordinates. Otherwise the results may not even be produced, or
  they will be inaccurate. Calculating the timezone of a given location is out
  of scope of this library.

  See also `date`, `in-zone`, and `now`."
  ([m d] (lookup-date m d (in-zone jerusalem-zone (now))))
  ([m d t] (lookup-date jerusalem-lat jerusalem-lon m d t))
  ([lat lon m d t]
   {:pre [(tick/zoned-date-time? t)
          (and (pos-int? m) (< 0 m 14)) (and (pos-int? d) (< 0 d 31))]}
   (try
     (let [months (start-of-months-in-year lat lon t)
           start-of-month (nth months (dec m))
           days (when start-of-month
                  (->> start-of-month
                       (go-forward 1 :hours)
                       (start-of-days-in-month lat lon)))]
          (when days
            (->> (dec d)
                 (nth days)
                 (go-forward 1 :hours)
                 (date lat lon))))
     (catch IndexOutOfBoundsException _e nil))))

(defn lookup-date-in-year
  "Return a map containing the details of a `date` where:
  `lat` is the latitude of the location
  `lon` is the longitude of the location
  `z` is a string containing a valid TimeZone description
  `y` is the gregorian year in which the hebrew year in question starts
  `m` is the hebrew month of year, and
  `d` is the hebrew day of month that you are looking for.

  If only `z`, `y`, `m`, and `d` are provided `lat` and `lon` will default to
  the coordinates of the Temple Mount in Jerusalem.

  In addition, if only `y`, `m`, and `d` is provided `z` will default to
  \"Asia/Jerusalem\".

  Example:
  (lookup-date-in-year 59.3325800 18.0649000 \"Europe/Stockholm\" 2025 1 14)

  The above will look for the 14th day of the 1st month of the hebrew year that
  starts in the gregorian year 2025. The timezone in use will be
  \"Europe/Stockholm\" and the coordinates are those of Stockholm, Sweden.

  Caution: Make sure that `z` is the actual timezone of the location at the
  provided coordinates. Otherwise the results may not even be produced, or they
  will be inaccurate. Calculating the timezone of a given location is out of
  scope of this library.

  See also `lookup-date`."
  ([y m d] (lookup-date-in-year jerusalem-zone y m d))
  ([z y m d] (lookup-date-in-year jerusalem-lat jerusalem-lon z y m d))
  ([lat lon z y m d]
   {:pre [(valid-zone? z) (and (pos-int? y) (<= 1584 y 2100))
          (and (pos-int? m) (< 0 m 14)) (and (pos-int? d) (< 0 d 31))]}
   (lookup-date lat lon m d (zdt z y 6 1 12))))

;; The following functions were used to calculate the feast days for
;; `feast-days`. They're terribly slow and shouldn't be used for any other
;; purpose. I'm leaving them here for future reference and if I have to
;; re-generate the dates due to a change in the calculation method or future bug
;; fix.
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

(defn- feast-days-in-year
  [year]
  (->> (for [y [(dec year) year]
             p potential-feast-dates]
         (lookup-date-in-year y (first p) (last p)))
       (filter #(or (get-in % [:hebrew :minor-feast-day])
                    (get-in % [:hebrew :major-feast-day])))
       (pmap #(let [s (get-in % [:time :day :start])]
                (vector [(tick/int (tick/year s))
                         (tick/int (tick/month s))
                         (tick/day-of-month s)]
                        (get-in % [:hebrew :minor-feast-day])
                        (get-in % [:hebrew :major-feast-day]))))
       (map #(remove false? %))
       (filter #(= (ffirst %) year))
       (sort-by #(second (first %)))))

(defn- map-of-feast-days-in-year
  [year]
  (let [dates (feast-days-in-year year)]
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

(defn list-of-feast-days-in-year
  "Given a gregorian `year` between 1584 and 2100, return a list of strings
  describing the feast days in that year. The dates represent the gregorian day
  on which the sunset would begin the feast day in question. Some days will have
  more than one feast day.

  Note: The years 2020-2039 are pre-calculated and defined in
  `feast-days`, which will be used for years that it contains. If the
  `year` is outside of this range there will be quite some computational time
  added."
  [year]
  (let [y (or (feast-days year)
              (map-of-feast-days-in-year year))]
    (sort (for [m (keys y)
                d (keys (y m))
                f (get-in y [m d])]
            (let [n (:name f)
                  day-of-feast (:day-of-feast f)
                  days-in-feast (:days-in-feast f)]
              (long-feast-day-name year m d n day-of-feast days-in-feast))))))
