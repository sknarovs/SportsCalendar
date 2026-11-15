# SportsCalendar data branch

This branch only holds the generated iCalendar feed and the match cache.
`run.sh` runs daily from cron on a Raspberry Pi (DietPi): it installs the
newest release binary, regenerates the calendar and commits any change here.

- Subscribe: `https://raw.githubusercontent.com/sknarovs/SportsCalendar/calendar/cal/latvia.ics`
- Source, documentation and releases: `https://github.com/sknarovs/SportsCalendar`

Do not edit the generated files by hand; the next run overwrites them.
