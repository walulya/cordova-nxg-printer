package swapp.printer;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.content.res.Resources;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.StringWriter;
import java.io.PrintWriter;

import com.nexgo.oaf.apiv3.APIProxy;
import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.printer.AlignEnum;
import com.nexgo.oaf.apiv3.device.printer.BarcodeFormatEnum;
import com.nexgo.oaf.apiv3.device.printer.DotMatrixFontEnum;
import com.nexgo.oaf.apiv3.device.printer.FontEntity;
import com.nexgo.oaf.apiv3.device.printer.OnPrintListener;
import com.nexgo.oaf.apiv3.device.printer.Printer;
import com.nexgo.oaf.smartpos.jni.SmartPOSJni;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;

/**
 * This class echoes a string called from JavaScript.
 */
public class PrinterHelper extends CordovaPlugin {
    private static final String LOG_TAG = "NXPrinter";

    private DeviceEngine deviceEngine;
    private Printer printer;
    private Context context;
    private Activity activity;
    private final int FONT_SIZE_SMALL = 20;
    private final int FONT_SIZE_NORMAL = 24;
    private final int FONT_SIZE_BIG = 28;
    private final int TEXT   = 0;
    private final int BARCODE = 1;
    private final int QRCODE  = 2;
    private FontEntity fontSmall = new FontEntity(DotMatrixFontEnum.CH_SONG_20X20, DotMatrixFontEnum.ASC_SONG_8X16);
    private FontEntity fontNormal = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_12X24);
    private FontEntity fontBold = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_BOLD_16X24);
    private FontEntity fontBig = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_12X24, false, true);

    AlignEnum[] align = new AlignEnum[]{AlignEnum.LEFT, AlignEnum.CENTER, AlignEnum.RIGHT};

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        context = this.cordova.getActivity().getBaseContext();
        activity = this.cordova.getActivity();
        deviceEngine = APIProxy.getDeviceEngine();
        printer = deviceEngine.getPrinter();
        printer.setTypeface(Typeface.DEFAULT);
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        try{
            if (action.equals("coolMethod")) {
                String message = args.getString(0);
                callbackContext.success("coolMethod ");
                //this.coolMethod(message, callbackContext);
                //listBT(callbackContext);
                return true;
            }
            if (action.equals("init")) {
                initNexGo(callbackContext);
                return true;
            }
            else if (action.equals("printtest")) {
                printTest(callbackContext);
                return true;
            }
            else if (action.equals("text")) {
                String text = args.getString(0);
                printText(callbackContext);
                return true;
            }
            else if (action.equals("receipt")) {
                printReceipt(args, callbackContext);
                return true;
            }

            if (action.equals("performAdd")) {
                int arg1 = args.getInt(0);
                int arg2 = args.getInt(1);
                /* Indicating success is failure is done by calling the appropriate method on the
                callbackContext.*/
                int result = arg1 + arg2;
                showToast("result calculated in Java: " + result);
                callbackContext.success("result calculated in Java: " + result);
                return true;
            }
        } catch(Exception e){
            callbackContext.error(e.getMessage());
        }
        return false;
    }

    public void initNexGo(CallbackContext callbackContext) {
        if (printer != null) {
            showToast("Print Initialized");
            callbackContext.success("Print Initialized");
        } else {
            callbackContext.error("Failed to initialize printer.");
        }

    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    public void showToast(String msg){
        Toast.makeText(cordova.getActivity().getWindow().getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void printTest(CallbackContext callbackContext) {
        printer.initPrinter();
        printer.setTypeface(Typeface.DEFAULT);
        printer.setLetterSpacing(5);

        printer.appendPrnStr("Test String", FONT_SIZE_SMALL, align[2], false);
        printer.appendPrnStr("---------------------------", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendPrnStr("merchant name:app test", FONT_SIZE_NORMAL, AlignEnum.RIGHT, false);
        printer.appendPrnStr("---------------------------", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendBarcode("20220301808908009", 50, 0, 2, BarcodeFormatEnum.CODE_128, AlignEnum.CENTER);
        printer.appendQRcode("this qr code", 200, AlignEnum.CENTER);
        printer.appendPrnStr("Ivan Walulya", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendPrnStr("\n", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendPrnStr("\n", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendPrnStr("\n", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendPrnStr("---------------------------", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendPrnStr("Swedbank", FONT_SIZE_SMALL, AlignEnum.LEFT, false);
        printer.appendPrnStr("I ACKNOWLEDGE SATISFACTORY RECEIPT OF RELATIVE GOODS/SERVICES", FONT_SIZE_SMALL, AlignEnum.LEFT, false);
        printer.startPrint(true, new OnPrintListener() {
            @Override
            public void onPrintResult(final int retCode) {
                 cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(cordova.getActivity().getWindow().getContext(), "Printing Succeeded", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void printText(CallbackContext callbackContext) {
        printer.initPrinter();
        printer.setTypeface(Typeface.DEFAULT);
        printer.setLetterSpacing(5);

        printer.appendPrnStr("Test String", FONT_SIZE_SMALL, AlignEnum.LEFT, false);
        printer.appendPrnStr("---------------------------", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendPrnStr("merchant name:app test", FONT_SIZE_NORMAL, AlignEnum.RIGHT, false);
        printer.startPrint(true, new OnPrintListener() {
            @Override
            public void onPrintResult(final int retCode) {
                 cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callbackContext.success("Printing Successful");
                        Toast.makeText(cordova.getActivity().getWindow().getContext(), "Printing Succeeded", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    private void printBarCode(String code) {
        try {
            Bitmap barcode = BarCodeUtil.encodeAsBitmap(code, 320, 90);
            printer.appendImage(barcode, AlignEnum.CENTER);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(cordova.getActivity().getWindow().getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void printQRCode(String code) {
        try {
            Bitmap qrcode = QRCodeUtil.encodeAsBitmap(code, 120, 120);
            printer.appendImage(qrcode, AlignEnum.CENTER);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(cordova.getActivity().getWindow().getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }    
    }

    private void printReceipt(JSONArray args, CallbackContext callbackContext) throws JSONException {
        printer.initPrinter();
        printer.setTypeface(Typeface.DEFAULT);
        printer.setLetterSpacing(5);

        Resources activityRes = cordova.getActivity().getResources();
        int logoResId = activityRes.getIdentifier("swapp_logo", "drawable", cordova.getActivity().getPackageName());

        Bitmap bitmap;

        bitmap = BitmapFactory.decodeResource(activityRes, logoResId);
        if (bitmap == null) {
            showToast("No image has been configured");
            return;
        }

        // printer.appendImage(bitmap, AlignEnum.CENTER);
         printer.appendPrnStr("Left", "Right", fontNormal);
        for (int i = 0; i < args.length(); ++ i) {
            JSONObject arg = args.getJSONObject(i);
            String text      = arg.getString("text");
            int fontSize     = arg.getInt("size");
            int alignment    = arg.getInt("align");
            boolean isBold   = arg.getBoolean("isbold");
            int ptype        = arg.getInt("type");
            if (ptype == TEXT) {
                printer.appendPrnStr(text, fontSize, align[alignment], isBold);
            } else if (ptype == BARCODE) {
                printBarCode(text);   // 1234567890
            } else if (ptype == QRCODE) {
                printQRCode(text);
            } else {
                Toast.makeText(cordova.getActivity().getWindow().getContext(), "Unknown content type", Toast.LENGTH_SHORT).show();
            }
        }

        

        

        printer.startPrint(true, new OnPrintListener() {
            @Override
            public void onPrintResult(final int retCode) {
                 cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(cordova.getActivity().getWindow().getContext(), "Printing Succeeded", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
