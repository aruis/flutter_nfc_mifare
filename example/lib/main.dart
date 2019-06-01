import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_nfc_mifare/flutter_nfc_mifare.dart';
import 'package:flutter_nfc_mifare/MF1.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool _nfcHere = false;
  MF1 _currentMF1;
  Uint8List _showBlock;

  @override
  void initState() {
    super.initState();
    isMF1Here();

    FlutterNfcMifare.channel.setMethodCallHandler((MethodCall call) async {
      switch (call.method) {
        case 'iamhere':
          setState(() {
            _nfcHere = true;
          });
          _currentMF1 = MF1.fromMap(call.arguments);
          return true;
      }
    });
  }

  Future<void> isMF1Here() async {
    var _flag = await FlutterNfcMifare.isMF1Here;
    setState(() {
      _nfcHere = _flag;
    });
  }

  Future<Uint8List> readMF1(int sectorIndex, int blockIndex) async {
    Uint8List key = Uint8List.fromList([255, 255, 255, 255, 255, 255]);
    if (!_currentMF1.isWhite) {
      key = getKeyByUID(_currentMF1.uid);
    }

    var result = await FlutterNfcMifare.readMF1(sectorIndex, blockIndex, key);

    setState(() {
      _showBlock = result;
    });
    return result;
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        home: Scaffold(
      appBar: AppBar(
        title: const Text('Plugin example app'),
      ),
      body: Center(
        child: Column(children: [
          Text(_nfcHere ? 'NFC is Here' : "No NFC"),
          _nfcHere
              ? Column(
                  children: <Widget>[
                    RaisedButton(
                      child: Text('检查设备'),
                      onPressed: isMF1Here,
                    ),
                    RaisedButton(
                      child: Text('读取信息'),
                      onPressed: () {
                        readMF1(2, 1);
                      },
                    ),
                    Text(_showBlock != null ? _showBlock.toString() : "null")
                  ],
                )
              : SizedBox(),
        ]),
      ),
    ));
  }

  Uint8List getKeyByUID(Uint8List MF1_UID) {
    Uint8List key = Uint8List(6);

    key[0] = 0x10 ^ MF1_UID[0] ^ MF1_UID[2];
    key[1] = 0x10 ^ MF1_UID[1] ^ MF1_UID[3];
    key[2] = 0x10 ^ MF1_UID[0] ^ MF1_UID[2];
    key[3] = 0x10 ^ MF1_UID[1] ^ MF1_UID[3];
    key[4] = 0x10 ^ MF1_UID[0] ^ MF1_UID[2];
    key[5] = 0x10 ^ MF1_UID[1] ^ MF1_UID[3];

    return key;
  }
}
