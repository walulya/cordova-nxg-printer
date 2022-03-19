package swapp.printer;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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


import com.nexgo.oaf.apiv3.DeviceEngine;
import com.nexgo.oaf.apiv3.device.printer.AlignEnum;
import com.nexgo.oaf.apiv3.device.printer.BarcodeFormatEnum;
import com.nexgo.oaf.apiv3.device.printer.DotMatrixFontEnum;
import com.nexgo.oaf.apiv3.device.printer.FontEntity;
import com.nexgo.oaf.apiv3.device.printer.OnPrintListener;
import com.nexgo.oaf.apiv3.device.printer.Printer;

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
    private static final String LOG_TAG = "NXGPrinter";

    private DeviceEngine deviceEngine;
    private Printer printer;
    private final int FONT_SIZE_SMALL = 20;
    private final int FONT_SIZE_NORMAL = 24;
    private final int FONT_SIZE_BIG = 24;
    private FontEntity fontSmall = new FontEntity(DotMatrixFontEnum.CH_SONG_20X20, DotMatrixFontEnum.ASC_SONG_8X16);
    private FontEntity fontNormal = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_12X24);
    private FontEntity fontBold = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_BOLD_16X24);
    private FontEntity fontBig = new FontEntity(DotMatrixFontEnum.CH_SONG_24X24, DotMatrixFontEnum.ASC_SONG_12X24, false, true);


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
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

        if (action.equals("performAdd")) {
            int arg1 = args.getInt(0);
            int arg2 = args.getInt(1);
            /* Indicating success is failure is done by calling the appropriate method on the
            callbackContext.*/
            int result = arg1 + arg2;
            callbackContext.success("result calculated in Java: " + result);
            return true;
        }
        return false;
    }

    public void initNexGo(CallbackContext callbackContext) {
        //初始化为针打printer
        //BaseApplication.instance.setCurrentCmdType(BaseEnum.CMD_ESC);
        deviceEngine = ((NexgoApplication) getApplication()).deviceEngine;
        printer = deviceEngine.getPrinter();
        printer.setTypeface(Typeface.DEFAULT);
        showToast("Print Initialized");
        callbackContext.success("Print Initialized");
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

        printer.appendPrnStr("Test String", FONT_SIZE_SMALL, AlignEnum.LEFT, false);
        printer.appendPrnStr("---------------------------", FONT_SIZE_NORMAL, AlignEnum.LEFT, false);
        printer.appendPrnStr("merchant name:app test", FONT_SIZE_NORMAL, AlignEnum.RIGHT, false);
        printer.startPrint(true, new OnPrintListener() {
            @Override
            public void onPrintResult(final int retCode) {
                Toast.makeText(PrinterActivity.this, retCode + "", Toast.LENGTH_SHORT).show();
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PrinterActivity.this, retCode + "", Toast.LENGTH_SHORT).show();
                    }
                }); */
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
                Toast.makeText(PrinterActivity.this, retCode + "", Toast.LENGTH_SHORT).show();
                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PrinterActivity.this, retCode + "", Toast.LENGTH_SHORT).show();
                    }
                });
                */
            }
        });
    }
}
