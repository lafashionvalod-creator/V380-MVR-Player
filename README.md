# V380 MVR Player Android

This project is a first playable prototype for V380 `.MVR` recordings.

Features:
- Open MVR using Android file picker
- Internally detects the V380 H.264 stream after the proprietary header
- Extracts to a temporary H.264 file
- Uses FFmpegKit to remux the recovered stream to a temporary MP4
- Plays the temporary MP4 with Media3
- Play/pause, ±10 seconds, timeline, fullscreen, screenshot
- No user-facing MP4 conversion

Build:
1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Run on an Android phone.
4. Build > Build APK(s).

The app needs no network at runtime for playback. The Gradle build downloads Media3/FFmpegKit dependencies.
