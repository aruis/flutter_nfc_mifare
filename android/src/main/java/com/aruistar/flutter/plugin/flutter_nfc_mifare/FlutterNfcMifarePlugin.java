package com.aruistar.flutter.plugin.flutter_nfc_mifare;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.util.Log;
import android.widget.Toast;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * FlutterNfcMifarePlugin
 */
public class FlutterNfcMifarePlugin implements MethodCallHandler, PluginRegistry.NewIntentListener {

    private static NfcAdapter mNfcAdapter;
    private static PendingIntent mPendingIntent;
    private static Context context;
    static MethodChannel channel = null;
    static MifareClassic currentMF1 = null;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        channel = new MethodChannel(registrar.messenger(), "flutter_nfc_mifare");
        FlutterNfcMifarePlugin plugin = new FlutterNfcMifarePlugin();
        channel.setMethodCallHandler(plugin);

        context = registrar.context();

        registrar
                .addNewIntentListener(plugin);

        // initialize nfc stuff
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (mNfcAdapter == null) {
            // Stop here, we need NFC
            Toast.makeText(context, "对不起，您的设备不支持NFC，无法正常使用本软件",
                    Toast.LENGTH_LONG).show();
        } else if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(context, "请到系统设置，开启NFC功能", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (call.method.equals("readMF1")) {
            List arguments = (List) call.arguments;
            result.success(readMF1((int) arguments.get(0), (int) arguments.get(1), (byte[]) arguments.get(2)));
        } else if (call.method.equals("writeMF1")) {
            List arguments = (List) call.arguments;
            result.success(writeMF1((int) arguments.get(0), (int) arguments.get(1), (byte[]) arguments.get(2), (byte[]) arguments.get(3)));
        } else if (call.method.equals("isMF1Here")) {
            if (currentMF1 == null) {
                result.success(false);
                return;
            }

            try {
                currentMF1.connect();
                currentMF1.close();
                result.success(true);
            } catch (IOException e) {
                e.printStackTrace();
                currentMF1 = null;
                result.success(false);
            }
        } else {
            result.notImplemented();
        }
    }


    private static void log(String str) {
        Log.e("ttt", str);
    }

    private static void log(byte[] bytes) {
        String str = "";
        for (int i = 0; i < bytes.length; i++) {
            str += bytes[i];
            str += ",";
        }

        log(str);
    }

    @Override
    public boolean onNewIntent(Intent intent) {
        try {
            readFromIntent(intent);
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void readFromIntent(Intent intent) throws IOException, ExecutionException, InterruptedException {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);


            byte[] MF1_UID = tagFromIntent.getId();

            log(MF1_UID);

            MifareClassic mfc = MifareClassic.get(tagFromIntent);

            if (mfc == null) return;

            currentMF1 = mfc;

            mfc.connect();
            boolean isWhite = mfc.authenticateSectorWithKeyA(2, MifareClassic.KEY_DEFAULT);
            int blockCount = mfc.getBlockCount();
            int sectorCount = mfc.getSectorCount();

            HashMap map = new HashMap();
            map.put("uid", MF1_UID);
            map.put("isWhite", isWhite);
            map.put("sectorCount", sectorCount);
            map.put("unitBlockCount", blockCount / sectorCount);

            iamhere(map);

            mfc.close();
        }
    }

    private boolean writeMF1(int sectorIndex, int blockIndex, byte[] key, byte[] data) {
        MifareClassic mfc = currentMF1;
        try {
            mfc.connect();
            boolean isOpen = mfc.authenticateSectorWithKeyA(sectorIndex, key);
            if (isOpen) {
                mfc.writeBlock(mfc.sectorToBlock(sectorIndex) + blockIndex, data);
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                mfc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private byte[] readMF1(int sectorIndex, int blockIndex, byte[] key) {
        MifareClassic mfc = currentMF1;
        try {
            mfc.connect();
            boolean isOpen = mfc.authenticateSectorWithKeyA(sectorIndex, key);
            log("is OPen" + isOpen);
            if (isOpen) {
                return mfc.readBlock(mfc.sectorToBlock(sectorIndex) + blockIndex);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                mfc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void iamhere(HashMap map) {
        channel.invokeMethod("iamhere", map, new Result() {
            @Override
            public void success(Object o) {
                log("success");
            }

            @Override
            public void error(String s, String s1, Object o) {
                log("error");
            }

            @Override
            public void notImplemented() {
                log("notImplemented");
            }
        });
    }


    private boolean testKey(MifareClassic mfc, byte[] key) throws IOException {
        return mfc.authenticateSectorWithKeyA(2, key);
    }

    private byte[] getKeyByUID(byte[] MF1_UID) {
        byte[] key = new byte[6];

        key[0] = (byte) (0x81 ^ MF1_UID[0] ^ MF1_UID[2]);
        key[1] = (byte) (0xED ^ MF1_UID[1] ^ MF1_UID[3]);
        key[2] = (byte) (0xBF ^ MF1_UID[0] ^ MF1_UID[2]);
        key[3] = (byte) (0xE8 ^ MF1_UID[1] ^ MF1_UID[3]);
        key[4] = (byte) (0xA5 ^ MF1_UID[0] ^ MF1_UID[2]);
        key[5] = (byte) (0xFC ^ MF1_UID[1] ^ MF1_UID[3]);

        return key;
    }
}
