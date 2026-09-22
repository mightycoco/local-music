# Privacy Policy for Local Music

**Effective date:** September 22, 2026

Local Music is a free and open-source Android music player for audio stored on your device, with optional support for online radio streams, external artwork lookup, and Google Cast. This Privacy Policy explains what information the app accesses, how it is used, and when information may be sent to third-party services.

## Summary

- Local Music does not require an account.
- Local Music does not contain advertising or user-tracking analytics.
- The developer does not collect, sell, rent, or profile your personal information.
- Your music library, playlists, favourites, queue, and settings are stored locally on your device.
- Local audio files are not uploaded by the app.
- Network access occurs only for features that require it, such as optional artwork lookup, online radio, and Google Cast.

## Information Accessed and Stored on Your Device

### Local music library

With your permission, Local Music reads audio files and related metadata available through Android's media library or folders you explicitly select. This can include:

- Song title, artist, album, album artist, genre, composer, year, comments, and description
- File name, folder, content URI, duration, and date added
- Embedded or nearby cover artwork
- Playback history and favourite status

This information is indexed in the app's private on-device database to provide library browsing, search, sorting, duplicate detection, playback, and artwork display.

### Playlists and playback state

The app stores playlists, favourites, the playback queue, the last selected song, and playback-related preferences locally on your device. Imported and exported M3U playlists are accessed only when you select them through Android's document picker.

### Settings

App settings are stored locally. These can include appearance options, selected library folders, artwork preferences, Car Mode settings, recognized Bluetooth car-audio devices, and other playback preferences.

Local Music disables Android app-data backup. App-managed data is not intentionally copied to cloud backup by the app.

## Permissions

Depending on your Android version and the features you use, Local Music may request:

- **Music and audio access:** To discover and play audio files on your device.
- **Selected folder access:** To scan folders that you explicitly choose through Android's Storage Access Framework.
- **Bluetooth access:** To detect connected Bluetooth audio devices for optional Car Mode behavior. The app does not use Bluetooth data to track your location.
- **Foreground media playback:** To continue playback and provide media controls while the app is in the background.
- **Internet and network access:** For optional artwork lookup, online radio streams, and Google Cast.
- **Notifications:** Where required by Android, to display playback controls and foreground playback status.

You can revoke system permissions through Android Settings. Features that depend on a revoked permission may stop working.

## Network Features and Third-Party Services

Local Music does not operate a developer-controlled server. The following optional features communicate directly with third-party services or destinations.

### MusicBrainz and Cover Art Archive

When external artwork lookup is enabled and local artwork cannot be found, the app may send artist and album text to MusicBrainz to search for a matching release. If a match is found, the app may request cover artwork from Cover Art Archive. Downloaded artwork is cached in the app's private storage.

External artwork lookup can be disabled in Settings. The privacy practices of these services are governed by their respective policies:

- [MusicBrainz Privacy Policy](https://metabrainz.org/privacy)
- [Cover Art Archive](https://coverartarchive.org/)

### Radio Browser and online streams

When you search for radio stations, the search text is sent to a Radio Browser server. When you preview or play an online station or another stream URL that you added, the app connects to that stream provider. Those services receive ordinary network information required for the connection, such as your IP address, request time, and client connection details.

The app does not control the privacy or retention practices of Radio Browser, station operators, or other stream providers. Their own terms and privacy policies apply.

### Google Cast

When you use Google Cast, Google Play services discovers compatible devices on your network and establishes the Cast session. For eligible online streams, the stream URI and available media metadata are sent to the Cast device so that it can load and play the media. Local audio files are not uploaded or served to Cast devices by Local Music.

Google's handling of information is governed by the [Google Privacy Policy](https://policies.google.com/privacy).

## Information Not Collected by the Developer

The app does not include developer-operated:

- User accounts or authentication
- Advertising
- Behavioral tracking
- Analytics
- Crash-reporting services
- Marketing profiles
- Cloud music-library synchronization
- Sale of personal information

The developer does not receive your local library database, playlists, favourites, settings, audio files, or listening history through the app.

## Data Sharing

The developer does not sell or rent user information. Information is transmitted to third parties only when needed for a feature you use, as described above. Third-party services may process ordinary network and request information under their own privacy policies.

## Data Retention and Deletion

Library metadata, playlists, favourites, settings, playback state, and cached artwork remain on your device until one of the following occurs:

- You remove or change the relevant data in the app.
- You clear the artwork cache or other available app data.
- You clear Local Music's storage through Android Settings.
- You uninstall the app.

Android may retain files that you explicitly exported outside the app, such as exported M3U playlists. You can delete those files using your device's file manager or another document-management app.

Because the developer does not receive or maintain an account or cloud copy of your app data, there is normally no developer-held personal data to retrieve or delete.

## Children's Privacy

Local Music is a general-audience music utility and is not specifically directed to children. The app does not knowingly collect personal information from children. Users should only access audio and online services appropriate for them and permitted by the applicable service provider.

## Security

Local Music uses Android app-private storage for its database, preferences, and cached artwork. No software can guarantee absolute security. Keep Android and the app updated, and install releases only from sources you trust.

## Open-Source Software

The source code is publicly available for inspection. The behavior of a particular installation depends on the version and distribution source from which it was obtained. Modified third-party builds may not follow this Privacy Policy.

## Changes to This Policy

This policy may be updated when the app's features, dependencies, or legal requirements change. Material changes will be reflected by updating the effective date and publishing the revised policy with the project and at the public privacy-policy URL used by the app's store listing.

## Contact

For privacy questions, contact:

**Email:** `REPLACE_WITH_SUPPORT_EMAIL`

Before publishing the app, replace this placeholder with a monitored contact address and publish this policy at a stable public HTTPS URL.