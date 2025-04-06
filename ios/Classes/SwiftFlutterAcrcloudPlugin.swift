import Flutter
import UIKit

public class SwiftFlutterAcrcloudPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "flutter_acrcloud", binaryMessenger: registrar.messenger())
    let instance = SwiftFlutterAcrcloudPlugin(channel)
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  var channel: FlutterMethodChannel!
  
  var _config: ACRCloudConfig!
  var _client: ACRCloudRecognition?

  var isListening = false

  init(_ channel: FlutterMethodChannel) {
    self.channel = channel
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    let args = call.arguments as? Dictionary<String, AnyObject>

    if call.method == "setUp" {
      self._config = ACRCloudConfig()

      _config.accessKey = (args!["accessKey"] as! String)
      _config.accessSecret = (args!["accessSecret"] as! String)
      _config.host = (args!["host"] as! String)

      _config.recMode = rec_mode_remote
      _config.requestTimeout = 10
      _config.protocol = "https"

      _config.resultBlock = {[weak self] res, resType in
        self?.handleResult(res!, resType:resType)
      }

      _config.volumeBlock = {[weak self] volume in
        self?.handleVolume(volume)
      }

      self._client = ACRCloudRecognition(config: _config)
      result(true)
    }

    else if call.method == "listen" {
      if self.isListening {
        result(nil)
        return
      }

      isListening = true
      self._client!.startRecordRec()
      result(true)
    }
        
    else if call.method == "cancel" {
        if !self.isListening {
            result(nil)
            return
        }
        
        self._client!.stopRecordRec()
        self.isListening = false
        result(true)
    } else if call.method == "createFingerprint" {
        guard let args = call.arguments as? [String: Any],
              let base64PCM = args["pcmData"] as? String,
              let pcmData = Data(base64Encoded: base64PCM) else {
          result(FlutterError(code: "INVALID_ARGUMENT", message: "Missing or invalid PCM", details: nil))
          return
        }

         if let fingerprintData = ACRCloudRecognition.get_fingerprint(pcmData) {
            let fingerprintBase64 = fingerprintData.base64EncodedString()
            result(fingerprintBase64)
         } else {
            result(FlutterError(code: "FINGERPRINT_ERROR", message: "Failed to create fingerprint", details: nil))
         }
    } else if call.method == "recognizeFingerPrint" {
         guard let args = call.arguments as? [String: Any],
              let base64Fingerprint = args["fingerprint"] as? String,
              let fingerprintData = Data(base64Encoded: base64Fingerprint) else {
          result(FlutterError(code: "INVALID_ARGUMENT", message: "Missing or invalid fingerprint", details: nil))
          return
        }
        let recResult = self._client!.recognize_fp(fingerprintData)
        result(recResult)
    }

    else {
      result(FlutterMethodNotImplemented)
    }
  }

  func handleResult(_ res: String, resType: ACRCloudResultType) -> Void {
    DispatchQueue.main.async {
      self._client?.stopRecordRec()
      self.isListening = false
      self.channel.invokeMethod("result", arguments: res)
    }
  }

  func handleVolume(_ volume: Float) -> Void {
    DispatchQueue.main.async {
      self.channel.invokeMethod("volume", arguments: volume)
    }
  }
}
