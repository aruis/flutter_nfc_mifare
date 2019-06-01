import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class FlutterNfcMifare {
  static const MethodChannel channel =
      const MethodChannel('flutter_nfc_mifare');

  static Future<bool> get isMF1Here async {
    return await channel.invokeMethod('isMF1Here');
  }

  static Future<Uint8List> readMF1(
      int sectorIndex, int blockIndex, Uint8List key) async {
    return await channel
        .invokeMethod('readMF1', [sectorIndex, blockIndex, key]);
  }

  static Future<bool> writeMF1(
      int sectorIndex, int blockIndex, Uint8List key, Uint8List data) async {
    return await channel
        .invokeMethod('writeMF1', [sectorIndex, blockIndex, key, data]);
  }
}

