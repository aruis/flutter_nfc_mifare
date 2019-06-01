import 'dart:typed_data';

class MF1 {
  Uint8List uid;
  bool isWhite;
  int sectorCount;
  int unitBlockCount;

  MF1(this.uid, this.isWhite, this.sectorCount, this.unitBlockCount);

  static MF1 fromMap(Map map) {
    return MF1(
      map["uid"],
      map["isWhite"],
      map["sectorCount"],
      map["unitBlockCount"],
    );
  }
}
