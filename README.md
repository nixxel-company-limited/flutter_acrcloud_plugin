# flutter_acrcloud_plugin

<!-- Keywords: flutter_acrcloud_plugin, acr_cloud, flutter, music recognition, audio fingerprinting -->

A Flutter plugin for the ACRCloud music recognition API.
This is a third-party plugin; there is no relation between the developer and ACRCloud.


**Original repository:** [github.com/nrubin29/flutter_acrcloud](https://github.com/nrubin29/flutter_acrcloud)

## Setup

In order to get access to the microphone, you have to explicitly list the required permission in your iOS and Android apps.

### iOS

1. Open `ios/Runner/Info.plist`.
2. Add the following lines somewhere inside of the `<dict>`:

   ```xml
   <key>NSMicrophoneUsageDescription</key>
   <string>Recognize the music around you</string>
   ```

3. You can replace the `<string>` with whatever message you want. This message will be displayed in the alert that asks the user to grant permission to access to the microphone.

### Android

1. Open `android/app/src/main/AndroidManifest.xml`
2. Add the following line inside of the `<manifest>` and above the `<application>`:

   ```xml
   <uses-permission android:name="android.permission.RECORD_AUDIO" />
   ```

## Usage

1. Call `ACRCloud.setUp()` to provide your API key, API secret, and preferred host. `setUp()` takes an instance of `ACRCloudConfig`.
2. When you want to recognize a song, call `ACRCloud.startSession()` to start a recording session. You will get an instance of `ACRCloudSession` that you can use to interact with the session.
3. To get the current volume, you can use the `volume` property of `ACRCloudSession`. This is a `Stream` that is updated every time a new volume value is recoded.
4. To cancel a session, just call `cancel()` on the session.
5. To get the result of a session, you can `await` the `result` property of the session. If the result is `null`, then the request was cancelled. Otherwise, you'll get an instance of `ACRCloudResponse` that contains all the information.

## Response

The result of a recognition session is an `ACRCloudResponse` object. This contains:

- `status`: Information about the request status and any errors.
- `metadata`: Contains lists of matched music tracks (`music`) and custom files (`customFiles`).

Each music item in `metadata.music` contains details such as:
- `label`
- `album` (with `name`)
- `artists` (list of artist objects with `name`)
- `acrId`
- `resultFrom`
- `title`
- `durationMs`
- `releaseDate`
- `score`
- `playOffsetMs`

Custom file items in `metadata.customFiles` contain:
- `acrId`
- `title`
- `durationMs`
- `score`
- `playOffsetMs`

Refer to the source code for full details on the response structure.

## Advanced Usage

### Creating and Recognizing Fingerprints

You can use the following static methods for advanced scenarios where you want to generate and recognize audio fingerprints manually:

- **createFingerPrint**  
  Generates an audio fingerprint from raw PCM data.  
  ```dart
  Uint8List? fingerprint = await ACRCloud.createFingerPrint(
    pcmData, // Uint8List of PCM audio data
    sampleRate, // int, e.g. 44100
    channels, // int, e.g. 1 or 2
  );
  ```

- **recognizeFingerprint**  
  Recognizes a song from a previously generated fingerprint.  
  ```dart
  ACRCloudResponse? response = await ACRCloud.recognizeFingerprint(
    fingerprint, // Uint8List fingerprint data
  );
  ```

These methods are useful if you want to handle audio recording and processing yourself, or if you want to recognize audio from sources other than the microphone.

## Example

Here is a minimal example of how to use `flutter_acrcloud_plugin` in your Flutter app:

```dart
import 'package:flutter_acrcloud_plugin/flutter_acrcloud_plugin.dart';

void main() async {
  // Set up the plugin with your ACRCloud credentials
  await ACRCloud.setUp(
    const ACRCloudConfig(
      'your_access_key',
      'your_access_secret',
      'your_host',
    ),
  );

  // Start a recognition session
  final session = ACRCloud.startSession();

  // Listen to volume changes (optional)
  session.volumeStream.listen((volume) {
    print('Current volume: $volume');
  });

  // Await the result
  final result = await session.result;
  if (result != null) {
    print('Recognized: \\${result.metadata?.music.first.title}');
  } else {
    print('Recognition cancelled or failed.');
  }
}
```

Replace `'your_access_key'`, `'your_access_secret'`, and `'your_host'` with your actual ACRCloud credentials.
