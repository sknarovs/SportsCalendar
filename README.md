# Latvian National Teams Calendar

Automatically generated iCalendar (`.ics`) subscription for the **Latvian men's
national football team** and the **Latvian men's national futsal team**.

Match dates, kick-off times, venues, final scores and goalscorers come from the
official Latvian Football Federation (LFF) website, [lff.lv](https://lff.lv).
GitHub Actions builds a native binary and publishes tagged releases; a daily
cron job on a Raspberry Pi (DietPi) downloads it, refreshes the calendar and
pushes the result to the `calendar` branch of this repository.

## Subscribe

Add this URL to Google Calendar, Apple Calendar, Thunderbird or any other
calendar app that supports iCalendar subscriptions:

```
https://raw.githubusercontent.com/sknarovs/SportsCalendar/calendar/cal/latvia.ics
```

The calendar contains one combined feed:

| Category | Events |
|----------|--------|
| Football | `Latvija vs Armēnija (competition)` or `Melnkalne 2-1 Latvija (competition)` |
| Futsal | same format, marked `CATEGORIES:Telpu futbols` |

Finished matches keep their final score; when a match page also lists the goal
timeline, the scorers and minutes appear in the event description. Penalty
shootouts are shown as `(5-4 pēcsp.)`.

## How it works

- `TeamPageParser` reads the fixture tables on
  [`nacionala-izlase`](https://lff.lv/izlases/nacionala-izlase/) and
  [`telpu-futbola-izlase`](https://lff.lv/izlases/nacionala-izlase/telpu-futbola-izlase/).
- `MatchPageParser` reads each finished match page for the venue and the goal
  timeline.
- `CalendarUpdater` keeps the last 365 days of results plus every future
  fixture, caches match details in `cache/matches.tsv`, and refetches a match
  page only when its score changes or it was played in the last two days.
- `IcsWriter` renders RFC 5545 output with `Europe/Riga` timestamps, line
  folding and a stable `DTSTAMP` per event. Unchanged days therefore produce
  no new commit.
- The output is written atomically. If scraping returns no matches at all, the
  existing calendar is kept instead of being replaced with an empty one.

## Build and test

Requires JDK/GraalVM 25.

```bash
./gradlew test                 # unit tests against saved lff.lv pages
./gradlew run --args="--test"  # offline self-test of the shipped code
./gradlew nativeCompile        # build/native/nativeCompile/sports-calendar
```

Run the generator locally:

```bash
./gradlew run                  # writes cal/latvia.ics and cache/matches.tsv
```

Options:

```
usage: sports-calendar [-h] [-o ICS] [-c CACHE] [-l DAYS] [-t]

  -o, --output ICS      Calendar file to write (default: cal/latvia.ics)
  -c, --cache CACHE     Match-details cache (default: cache/matches.tsv)
  -l, --lookback DAYS   Days of finished matches to keep (default: 365)
  -t, --test            Run the offline self-test and exit
```

## Releases

Push a tag such as `v1.0.0`. The `Release` workflow builds
`sports-calendar-linux-aarch64` in a Debian 12 (bookworm) container on an
arm64 runner, runs the native self-test and attaches the binary and its SHA-256
checksum to a GitHub Release. The version is deliberately built against glibc
2.36 so it runs on 64-bit Raspberry Pi OS / DietPi v10 and newer.

## DietPi setup

The Pi clones only the `calendar` branch, which contains `run.sh`, the
generated calendar and the cache:

```bash
sudo apt-get install -y git curl
sudo mkdir -p /opt/sports-calendar
sudo chown "$USER" /opt/sports-calendar
git clone --branch calendar --single-branch \
  git@github.com:sknarovs/SportsCalendar.git /opt/sports-calendar
```

Create a passphrase-less deploy key on the Pi, add it to the repository as a
**write** deploy key, and make sure the clone can push to the `calendar`
branch:

```bash
ssh-keygen -t ed25519 -N '' -f ~/.ssh/sports_calendar
cat ~/.ssh/sports_calendar.pub
```

`run.sh` pulls the latest calendar branch, installs the newest release binary
when its tag changed, runs the built-in self-test, regenerates the calendar and
commits/pushes only when the output changed. Install it as a daily cron job,
for example at 05:17:

```cron
17 5 * * * /opt/sports-calendar/run.sh >> /var/log/sports-calendar.log 2>&1
```

Run it once by hand first to confirm that the deploy key and the binary
download work:

```bash
/opt/sports-calendar/run.sh
```

## Repository layout

- `main` — Java sources, tests, Gradle build and GitHub Actions workflows.
- `calendar` — `run.sh`, `cal/latvia.ics` and `cache/matches.tsv`, updated by
  the cron job on the Pi.
