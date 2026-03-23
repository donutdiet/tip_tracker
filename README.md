# Tip Tracker
[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" width="120">](https://play.google.com/store/apps/details?id=com.jerrywang.tiptracker)

A native android app I built to help me keep track of all my restaurant visits.

## Feature List
- Basic tip calculations with configurable tip percent presets, rounding, and bill splitting 
- Full log history stored locally on the device
- Rating distribution to visualize how you tend to rate restaurants
- Restaurant awards to track fun metrics across your logs 

## Architecture/Design Overview
- MVI architecture pattern
- Root NavDisplay for full-screen pages + tab NavDisplay for bottom navigation
- One-time events handled using Channels
- /ui - jetpack compose screen composables, viewmodels, state classes
- /data - room database schema, entity classes, dao, repository

## Tech Stack
- Kotlin
- Jetpack Compose
- Room (SQLite) database
- DataStore Preferences
- Koin
- Navigation 3
- Material 3
- [Vico](https://guide.vico.patrykandpatrick.com/)
