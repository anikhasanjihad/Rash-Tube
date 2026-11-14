# Rash Tube

A YouTube and YouTube Music client for Android (phone and Android TV) with a liquid-glass interface,
built-in ambient light for video, a dedicated downloads section, Picture-in-Picture, background
playback and optional YouTube account sign-in.

**Developer:** AHR

## Features
- Liquid-glass UI with smooth, expressive motion across every screen
- YouTube-style player: draggable mini player, chapters, gestures, Picture-in-Picture
- Ambient light behind the video (on by default)
- Separate Downloads section for videos and songs
- Sign in with your YouTube account and import your subscriptions (Settings -> YouTube account)
- In-app updates from this repository's GitHub Releases

## Building
`./gradlew assembleGithubDebug` (needs JDK 21 and the Android SDK).
To build a signed release on GitHub, see `ci/build-apk.yml`.

## License and credits
Rash Tube is free software under the **GNU GPL v3.0** (see `License`).
It is based on [Flow](https://github.com/Flow-Tube/Flow) by A-EDev, also GPL-3.0, with modifications by AHR.
Third-party notices are shown in the app under About -> Open source notices.
