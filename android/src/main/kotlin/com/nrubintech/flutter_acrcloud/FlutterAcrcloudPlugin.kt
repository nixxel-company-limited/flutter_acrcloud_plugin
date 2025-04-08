package com.nrubintech.flutter_acrcloud

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.acrcloud.rec.ACRCloudClient
import com.acrcloud.rec.ACRCloudConfig
import com.acrcloud.rec.ACRCloudResult
import com.acrcloud.rec.IACRCloudListener

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import kotlinx.coroutines.*

/** FlutterAcrcloudPlugin */
class FlutterAcrcloudPlugin: FlutterPlugin, MethodCallHandler, PluginRegistry.RequestPermissionsResultListener, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext

    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_acrcloud")
    channel.setMethodCallHandler(this)
  }

  private lateinit var context: Context
  private var currentActivity: Activity? = null

  private lateinit var config: ACRCloudConfig
  private lateinit var client: ACRCloudClient

  private var onPermissionGrant: (() -> Unit)? = null

  private var isPermissionGranted = false
  private var isListening = false

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "setUp") {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED) {
        setUp(call, result)
      }

      else {
        onPermissionGrant = { setUp(call, result) }
        ActivityCompat.requestPermissions(currentActivity!!, arrayOf(Manifest.permission.RECORD_AUDIO), 29)
      }
    }

    else if (call.method == "listen") {
      if (isListening) {
        result.error("ALREADY_LISTENING", "Already listening", null)
        return
      }

      isListening = true
      client.startRecognize()
      result.success(true)
    }

    else if (call.method == "cancel") {
      if (!isListening) {
        result.error("NOT_LISTENING", "Not listening, so nothing to cancel.", null)
        return
      }

      client.stopRecordToRecognize()
      isListening = false
      result.success(true)
    }
    else if (call.method == "createFingerprint") {
      val byteData = call.argument<ByteArray>("pcmData")
      val soundSample = call.argument<Int>("sampleRate") ?: 16000
      val soundChannels = call.argument<Int>("channels") ?: 2
      if (byteData == null) {
        result.error("UNAVAILABLE","Empty Byte Data",null)
      }
      else {
        val fpByteArray = createFingerprint(byteData,soundSample,soundChannels)
        result.success(fpByteArray)
      }
    }

    else if (call.method == "recognizeFingerprint") {
      val byteData = call.argument<ByteArray>("fingerprint")
      if (byteData == null) {
        result.error("UNAVAILABLE","Empty Fingerprint Data",null)
      }
      else {
        recogniseFingerprintAsync(byteData) { fingerprintResult ->
          result.success(fingerprintResult)
        }
      }

    }

    else {
      result.notImplemented()
    }
  }

  private fun setUp(@NonNull call: MethodCall, @NonNull result: Result) {
    config = ACRCloudConfig()

    config.acrcloudListener = ACRListener()
    config.context = context

    config.accessKey = call.argument("accessKey")
    config.accessSecret = call.argument("accessSecret")
    config.host = call.argument("host")

    config.recorderConfig.rate = 8000
    config.recorderConfig.channels = 1
    config.recorderConfig.isVolumeCallback = true
    config.protocol = ACRCloudConfig.NetworkProtocol.HTTPS

    client = ACRCloudClient()
    client.initWithConfig(config)

    result.success(true)
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private inner class ACRListener : IACRCloudListener {
    override fun onResult(result: ACRCloudResult?) {
      client.stopRecordToRecognize()
      isListening = false
      channel.invokeMethod("result", result?.result)
    }

    override fun onVolumeChanged(volume: Double) {
      channel.invokeMethod("volume", volume)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
    if (requestCode == 29) {
      isPermissionGranted = grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED
      onPermissionGrant?.invoke()

      return true
    }

    return false
  }

  override fun onDetachedFromActivity() {
    currentActivity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    currentActivity = binding.activity
    binding.addRequestPermissionsResultListener(this)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    currentActivity = binding.activity
    binding.addRequestPermissionsResultListener(this)
  }

  override fun onDetachedFromActivityForConfigChanges() {
    currentActivity = null
  }

  private fun recogniseFingerprintAsync(
    buffer: ByteArray,
    callback: (String?) -> Unit
  ) {
    CoroutineScope(Dispatchers.IO).launch {
      val result = client?.recognizeFingerprint(
        buffer,
        buffer.size,
        ACRCloudConfig.RecognizerType.AUDIO,
        null
      )

      withContext(Dispatchers.Main) {
        callback(result)
      }
    }
  }

  private fun createFingerprint(pcmData: ByteArray, sampleRate: Int, channels: Int): ByteArray {
    return ACRCloudClient.createClientFingerprint(pcmData,pcmData.size,sampleRate,channels)
  }

}
