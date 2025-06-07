import 'package:flutter/material.dart';
import 'package:flutter_acrcloud_plugin/flutter_acrcloud_plugin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ACRCloud Example',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const ExampleHomePage(),
    );
  }
}

class ExampleHomePage extends StatefulWidget {
  const ExampleHomePage({super.key});

  @override
  State<ExampleHomePage> createState() => _ExampleHomePageState();
}

class _ExampleHomePageState extends State<ExampleHomePage> {
  String? _result;
  bool _isListening = false;
  Stream<double>? _volumeStream;

  @override
  void initState() {
    super.initState();
    _setupACRCloud();
  }

  Future<void> _setupACRCloud() async {
    await ACRCloud.setUp(const ACRCloudConfig(
      'your_access_key',
      'your_access_secret',
      'your_host',
    ));
  }

  Future<void> _startRecognition() async {
    setState(() {
      _isListening = true;
      _result = null;
    });
    final session = ACRCloud.startSession();
    _volumeStream = session.volumeStream;
    session.volumeStream.listen((volume) {
      // Optionally handle volume updates
    });
    final result = await session.result;
    setState(() {
      _isListening = false;
      if (result != null && result.metadata?.music.isNotEmpty == true) {
        _result = result.metadata!.music.first.title;
      } else {
        _result = 'No result or recognition cancelled.';
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ACRCloud Example')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (_isListening) const CircularProgressIndicator(),
            if (_result != null) Text('Result: $_result'),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _isListening ? null : _startRecognition,
              child: const Text('Start Recognition'),
            ),
          ],
        ),
      ),
    );
  }
}
