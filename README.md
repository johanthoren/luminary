# luminary

A Clojure library designed to provide date calculations based on the Bible and
the 1st Book of Enoch.

> And God said, Let there be **lights** in the firmament of the heaven to divide
> the day from the night; and let them be for signs, and for seasons, and for
> days, and years:
>
> And let them be for **lights** in the firmament of the heaven to give light
> upon the earth: and it was so.

[Genesis
1:14-15](https://www.biblegateway.com/passage/?search=Genesis+1:14-15&version=KJV)

## Usage

[![Clojars
Project](https://img.shields.io/clojars/v/xyz.thoren/luminary.svg)](https://clojars.org/xyz.thoren/luminary)

Note that Luminary is still in active development and things are changing with
every release. Consider it unstable until a 1.0.0 release has been tagged.

Require:

``` clojure
(:require [xyz.thoren.luminary :as l])
```

### Documentation

See [the full API documentation](https://cljdoc.org/d/xyz.thoren/luminary).

### Examples:

`date` is the *main* function that constructs a map containing details about a
day according to biblical timekeeping. The most simple use is without any
arguments, which will give you the current biblical day in Jerusalem, Israel.
However, you can provide a range of arguments to fully customize when and where
to calculate the day.
``` clojure
(l/date)
;; => 
{:hebrew
 {:days-in-month 30,
  :day-of-week 3,
  :month-of-year 5,
  :minor-feast-day false,
  :day-of-month 9,
  :major-feast-day false,
  :sabbath false,
  :names
  {:month-of-year "5th",
   :traditional-month-of-year "Av",
   :day-of-month "9th",
   :day-of-week "3rd day of the week"},
  :months-in-year 12},
 :time
 {:year
  {:start #time/zoned-date-time "2021-04-12T19:06+03:00[Asia/Jerusalem]",
   :end #time/zoned-date-time "2022-04-01T18:57:59+03:00[Asia/Jerusalem]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false},
  :month
  {:start #time/zoned-date-time "2021-08-08T19:29+03:00[Asia/Jerusalem]",
   :end #time/zoned-date-time "2021-09-07T18:54:59+03:00[Asia/Jerusalem]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false},
  :week
  {:start #time/zoned-date-time "2021-08-14T19:23+03:00[Asia/Jerusalem]",
   :end #time/zoned-date-time "2021-08-21T19:15:59+03:00[Asia/Jerusalem]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false},
  :day
  {:start #time/zoned-date-time "2021-08-16T19:21+03:00[Asia/Jerusalem]",
   :end #time/zoned-date-time "2021-08-17T19:19:59+03:00[Asia/Jerusalem]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false}}}
```

`lookup-date` allows you to do a reverse lookup of a date in the biblical year.
It too can be provided with a range of arguments which it passes on to `date`.
``` clojure
(l/lookup-date 5 11)
;; => 
{:hebrew
 {:days-in-month 30,
  :day-of-week 5,
  :month-of-year 5,
  :minor-feast-day false,
  :day-of-month 11,
  :major-feast-day false,
  :sabbath false,
  :names
  {:month-of-year "5th",
   :traditional-month-of-year "Av",
   :day-of-month "11th",
   :day-of-week "5th day of the week"},
  :months-in-year 12},
 :time
 {:year
  {:start #time/zoned-date-time "2021-04-12T19:06+03:00[Asia/Jerusalem]",
   :end #time/zoned-date-time "2022-04-01T18:57:59+03:00[Asia/Jerusalem]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false},
  :month
  {:start #time/zoned-date-time "2021-08-08T19:29+03:00[Asia/Jerusalem]",
   :end #time/zoned-date-time "2021-09-07T18:54:59+03:00[Asia/Jerusalem]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false},
  :week
  {:start #time/zoned-date-time "2021-08-14T19:23+03:00[Asia/Jerusalem]",
   :end #time/zoned-date-time "2021-08-21T19:15:59+03:00[Asia/Jerusalem]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false},
  :day
  {:start #time/zoned-date-time "2021-08-18T19:19+03:00[Asia/Jerusalem]",
   :end #time/zoned-date-time "2021-08-19T19:17:59+03:00[Asia/Jerusalem]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false}}}
```

`lookup-date-in-year` is similar to `lookup-date` but allows you to provide a
gregorian year, in which the hebrew year would have started that you are trying
to find a `date` in.
``` clojure
(l/lookup-date-in-year 78.2253587 15.4878901 "Europe/Oslo" 2025 1 14)
;; => 
{:hebrew
 {:days-in-month 30,
  :day-of-week 7,
  :month-of-year 1,
  :minor-feast-day false,
  :day-of-month 14,
  :major-feast-day
  {:name "Passover", :hebrew-name "Pesach", :day-of-feast 1, :days-in-feast 1},
  :sabbath true,
  :names
  {:month-of-year "1st",
   :traditional-month-of-year "Nisan",
   :day-of-month "14th",
   :day-of-week "Sabbath"},
  :months-in-year 13},
 :time
 {:year
  {:start #time/zoned-date-time "2025-03-29T19:32+01:00[Europe/Oslo]",
   :end #time/zoned-date-time "2026-04-18T00:13:59+02:00[Europe/Oslo]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false},
  :month
  {:start #time/zoned-date-time "2025-03-29T19:32+01:00[Europe/Oslo]",
   :end #time/zoned-date-time "2025-04-28T23:56:59+02:00[Europe/Oslo]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region true},
  :week
  {:start #time/zoned-date-time "2025-04-05T21:31+02:00[Europe/Oslo]",
   :end #time/zoned-date-time "2025-04-12T22:43:59+02:00[Europe/Oslo]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false},
  :day
  {:start #time/zoned-date-time "2025-04-11T22:32+02:00[Europe/Oslo]",
   :end #time/zoned-date-time "2025-04-12T22:43:59+02:00[Europe/Oslo]",
   :start-adjusted-for-polar-region false,
   :end-adjusted-for-polar-region false}}}
```

`list-of-feast-days-in-year` will list the dates on which 
holidays start at the sunset:
``` clojure
(l/list-of-feast-days-in-year 2021)
;; => 
("2021-01-13 1st day of the 11th month"
 "2021-02-12 1st day of the 12th month"
 "2021-02-25 Purim"
 "2021-02-26 Shushan Purim"
 "2021-03-13 1st day of the 13th month"
 "2021-04-12 1st day of the 1st month"
 "2021-04-25 Passover"
 "2021-04-26 1st day of the Feast of Unleavened Bread"
 "2021-04-27 2nd day of the Feast of Unleavened Bread"
 "2021-04-28 3rd day of the Feast of Unleavened Bread"
 "2021-04-29 4th day of the Feast of Unleavened Bread"
 "2021-04-30 5th day of the Feast of Unleavened Bread"
 "2021-05-01 Feast of First Fruits"
 "2021-05-02 7th day of the Feast of Unleavened Bread"
 "2021-05-12 1st day of the 2nd month"
 "2021-06-10 1st day of the 3rd month"
 "2021-06-19 Feast of Weeks"
 "2021-07-10 1st day of the 4th month"
 "2021-08-08 1st day of the 5th month"
 "2021-09-07 1st day of the 6th month"
 "2021-10-06 1st day of the 7th month"
 "2021-10-06 Feast of Trumpets"
 "2021-10-15 Day of Atonement"
 "2021-10-20 1st day of the Feast of Tabernacles"
 "2021-10-21 2nd day of the Feast of Tabernacles"
 "2021-10-22 3rd day of the Feast of Tabernacles"
 "2021-10-23 4th day of the Feast of Tabernacles"
 "2021-10-24 5th day of the Feast of Tabernacles"
 "2021-10-25 6th day of the Feast of Tabernacles"
 "2021-10-26 7th day of the Feast of Tabernacles"
 "2021-10-27 The Last Great Day"
 "2021-11-05 1st day of the 8th month"
 "2021-12-04 1st day of the 9th month"
 "2021-12-28 1st day of Hanukkah"
 "2021-12-29 2nd day of Hanukkah"
 "2021-12-30 3rd day of Hanukkah"
 "2021-12-31 4th day of Hanukkah")
```

## Background

The Bible instructs believers to keep several Feasts, or *Moedim*. There are
several methods for determining what date it is according to the model described
in the Bible. The most common would be [the Rabbinic
Calendar](https://en.wikipedia.org/wiki/Hebrew_calendar) which is a mathematical
calendar based on a lunar month and a solar year, but there are others, such as
[the Karaite
Calendar](https://en.wikipedia.org/wiki/Hebrew_calendar#Karaite_calendar), which
is also has a lunar month and a solar year, but where observation of the new
moon is the basis for a 'new month' and the status of the Barley in Israel is
the basis for a 'new year'.

This library is based both on The Bible and the 1st Book of Enoch and follows
the following conventions:

- A new year will start at the first sunset after the first lunar conjunction
  following the March Equinox.
- A new month will start at the first sunset following lunar conjunction.
- A new day starts at the sunset.
- The week starts on the evening of what is commonly known as Saturday.
- The Sabbath falls on the last day of the 7 day week.
- For locations outside of Israel, the new year and the new month will fall on
the same Gregorian date as they did in Israel. For example, if the next month
will start on the eve of March 3rd in Israel, the same will be true for
locations where the sunset will come before the lunar conjunction. This is so
that everyone will keep the feast days together which would be impossible if one
would not base the new year and the new month on the one in Israel.
- For locations where the sun would not rise or set on a particular date, the
latitude is adjusted to 65.7/-65.7 for that particular day, making sure that
there is always a sunset to start the new day. This is how I imagine keeping the
day count if moving to such a location. For days when there is a sunset at the
actual coordinates, that sunset will be used.
- The location used to calculate the timing of the new year and the new month is
the Temple Mount in Jerusalem.
- The feast of First Fruits falls on the day following the weekly Sabbath during
the Feast of Unleavened Bread.
- On leap years Purim falls on Adar I following the Karaite convention.

## Contribution

Contributions are welcome. Especially those related to performance improvements
and bug fixes. Please don't open an issue or a pull request to 'fix' the
interpretation of Scripture leading to this calendar system. There are many
calendar systems out there and I absolutely respect that no one can know with
100% assurance how to calculate these matters until Messiah comes back.

## Acknowledgements

- Sun and Moon calculations are provided by [shred's
  commons-suncalc](https://github.com/shred/commons-suncalc).
- Equinox calculations are provided by
  [xyz.thoren.equinox](https://github.com/johanthoren/equinox).
- All time operations are utilizing [tick.core](https://github.com/juxt/tick).

## License

Copyright &copy; 2021 Johan Thorén

This project is licensed under the 
[GNU Lesser General Public License v3.0][license].

[license]: https://choosealicense.com/licenses/lgpl-3.0
