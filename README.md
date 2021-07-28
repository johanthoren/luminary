# luminary

A Clojure library designed to provide date calculations based on the Bible and
the 1st Book of Enoch.

## Usage

[![Clojars Project](https://img.shields.io/clojars/v/xyz.thoren/luminary.svg)](https://clojars.org/xyz.thoren/luminary)

Require:

``` clojure
(:require [xyz.thoren.luminary :as l])
```

Examples:

``` clojure
(l/find-date 5 11)
;; => 
{:hebrew
 {:month-of-year 5,
  :months-in-year 12,
  :day-of-month 11,
  :days-in-month 29,
  :day-of-week 5,
  :sabbath false,
  :feast-day false,
  :names
  {:month-of-year "5th",
   :traditional-month-of-year "Av",
   :day-of-month "11th",
   :day-of-week "5th day of the week"}},
 :time
 {:year
  {:start #object[java.time.ZonedDateTime 0x209606cf "2021-04-12T19:06:09+03:00[Asia/Jerusalem]"],
   :end #object[java.time.ZonedDateTime 0x2af1c9f5 "2022-04-01T18:58:34+03:00[Asia/Jerusalem]"]},
  :month
  {:start #object[java.time.ZonedDateTime 0x703e54e5 "2021-08-08T19:29:36+03:00[Asia/Jerusalem]"],
   :end #object[java.time.ZonedDateTime 0x29d71d73 "2021-09-07T18:55:23+03:00[Asia/Jerusalem]"]},
  :week
  {:start #object[java.time.ZonedDateTime 0x71a9fb30 "2021-08-14T19:23:43+03:00[Asia/Jerusalem]"],
   :end #object[java.time.ZonedDateTime 0x3fdfeb4b "2021-08-21T19:16:08+03:00[Asia/Jerusalem]"]},
  :day
  {:start #object[java.time.ZonedDateTime 0x6fa2c8b0 "2021-08-18T19:19:29+03:00[Asia/Jerusalem]"],
   :end #object[java.time.ZonedDateTime 0x4b5afa8b "2021-08-19T19:18:22+03:00[Asia/Jerusalem]"],
   :adjusted-for-polar-region false}}}
```

``` clojure
(l/find-date-in-year 59.3325800 18.0649000 "Europe/Stockholm" 2025 1 14)
;; => 
{:hebrew
 {:month-of-year 1,
  :months-in-year 13,
  :day-of-month 14,
  :days-in-month 30,
  :day-of-week 7,
  :sabbath true,
  :feast-day
  {:name "Passover", :hebrew-name "Pesach", :day-of-feast 1, :days-in-feast 1},
  :names
  {:month-of-year "1st",
   :traditional-month-of-year "Nisan",
   :day-of-month "14th",
   :day-of-week "Sabbath"}},
 :time
 {:year
  {:start #object[java.time.ZonedDateTime 0x3025eb10 "2025-03-29T18:23:56+01:00[Europe/Stockholm]"],
   :end #object[java.time.ZonedDateTime 0x2d1c36e5 "2026-04-17T20:08:29+02:00[Europe/Stockholm]"]},
  :month
  {:start #object[java.time.ZonedDateTime 0x4bb44016 "2025-03-29T18:23:56+01:00[Europe/Stockholm]"],
   :end #object[java.time.ZonedDateTime 0x3751a89 "2025-04-28T20:35:28+02:00[Europe/Stockholm]"]},
  :week
  {:start #object[java.time.ZonedDateTime 0x50423184 "2025-04-05T19:40:30+02:00[Europe/Stockholm]"],
   :end #object[java.time.ZonedDateTime 0x2df2b0e1 "2025-04-12T19:57:07+02:00[Europe/Stockholm]"]},
  :day
  {:start #object[java.time.ZonedDateTime 0x62560a71 "2025-04-11T19:54:45+02:00[Europe/Stockholm]"],
   :end #object[java.time.ZonedDateTime 0x96ff0a2 "2025-04-12T19:57:07+02:00[Europe/Stockholm]"],
   :adjusted-for-polar-region false}}}
```

## Background

The Bible instructs believers to keep several Feasts, or *Moedim*. There 
are several methods for determining what date it is according to the model 
described in the Bible. The most common would be 
[the Rabbinic Calendar](https://en.wikipedia.org/wiki/Hebrew_calendar) which 
is a mathematical calendar based on a lunar month and a solar year, but there 
are others, such as 
[the Karaite Calendar](https://en.wikipedia.org/wiki/Hebrew_calendar#Karaite_calendar),
which is also has a lunar month and a solar year, but where observation of the
new moon is the basis for a 'new month' and the status of the Barley in Israel
is the basis for a 'new year'.

This library is based both on The Bible and the 1st Book of Enoch and follows
the following conventions:

- A new year will start at the first sunset after the first lunar conjunction following the March Equinox.
- A new month will start at the first sunset following lunar conjunction.
- A new day starts at the sunset.
- The week starts on the evening of what is commonly known as Saturday.
- The Sabbath falls on the last day of the 7 day week.
- For locations outside of Israel, the new year and the new month will fall on
the same Gregorian date as they did in Israel. For example, if the next month will
start on the eve of March 3rd in Israel, the same will be true for locations where
the sunset will come before the lunar conjunction. This is so that everyone will
keep the feast days together which would be impossible if one would not base the
new year and the new month on the one in Israel.
- For locations where the sun would not rise or set on a particular date, the
latitude is adjusted to 65.7/-65.7 for that particular day, making sure that there
is always a sunset to start the new day. This is how I imagine keeping the day count
if moving to such a location. For days when there is a sunset at the actual coordinates,
that sunset will be used.
- The location used to calculate the timing of the new year and the new month is the
Temple Mount in Jerusalem.
- The feast of First Fruits falls on the day following the weekly Sabbath during
the Feast of Unleavened Bread.
- On leap years Purim falls on Adar I following the Karaite convention.

## Contribution

Pull requests are welcome. Especially those related to performance improvements,
and of course bug fixes. Please don't open an issue or a pull request to 'fix'
the interpretation of Scripture leading to this calendar system. There are many 
calendar systems out there and I absolutely respect that no one can know with
100% assurance how to calculate these matters until Messiah comes back.

With that being said, I will consider pull requests to support multiple variants,
such as sliver calculations, etc.

## Acknowledgements

- Sun and Moon calculations are provided by [shred's commons-suncalc](https://github.com/shred/commons-suncalc).
- Equinox calculations are provided by [xyz.thoren.equinox](https://github.com/johanthoren/equinox).
- All time operations are utilizing [clojure.java-time](https://github.com/dm3/clojure.java-time).

## License

Copyright &copy; 2021 Johan Thorén

This project is licensed under the [GNU Lesser General Public License v3.0][license].

[license]: https://choosealicense.com/licenses/lgpl-3.0
