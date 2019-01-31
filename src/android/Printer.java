package pebuu.printer;

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
import android.content.res.Resources;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.StringWriter;
import java.io.PrintWriter;

import com.rt.printerlibrary.bean.BluetoothEdrConfigBean;
import com.rt.printerlibrary.bean.UsbConfigBean;
import com.rt.printerlibrary.bean.WiFiConfigBean;
import com.rt.printerlibrary.cmd.Cmd;
import com.rt.printerlibrary.cmd.CpclFactory;
import com.rt.printerlibrary.cmd.EscCmd;
import com.rt.printerlibrary.cmd.EscFactory;
import com.rt.printerlibrary.cmd.PinFactory;
import com.rt.printerlibrary.cmd.TscFactory;
import com.rt.printerlibrary.cmd.ZplFactory;
import com.rt.printerlibrary.bean.LableSizeBean;
import com.rt.printerlibrary.bean.Position;
import com.rt.printerlibrary.enumerate.BarcodeStringPosition;
import com.rt.printerlibrary.enumerate.BarcodeType;
import com.rt.printerlibrary.enumerate.EscBarcodePrintOritention;
import com.rt.printerlibrary.enumerate.PrintDirection;
import com.rt.printerlibrary.enumerate.ESCFontTypeEnum;
import com.rt.printerlibrary.enumerate.PrintRotation;
import com.rt.printerlibrary.enumerate.CommonEnum;
import com.rt.printerlibrary.enumerate.BmpPrintMode;
import com.rt.printerlibrary.enumerate.ConnectStateEnum;
import com.rt.printerlibrary.exception.SdkException;
import com.rt.printerlibrary.connect.PrinterInterface;
import com.rt.printerlibrary.factory.cmd.CmdFactory;
import com.rt.printerlibrary.factory.connect.BluetoothFactory;
import com.rt.printerlibrary.factory.connect.PIFactory;
import com.rt.printerlibrary.factory.connect.UsbFactory;
import com.rt.printerlibrary.factory.connect.WiFiFactory;
import com.rt.printerlibrary.factory.printer.LabelPrinterFactory;
import com.rt.printerlibrary.factory.printer.PinPrinterFactory;
import com.rt.printerlibrary.factory.printer.PrinterFactory;
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory;
import com.rt.printerlibrary.factory.printer.UniversalPrinterFactory;
import com.rt.printerlibrary.observer.PrinterObserver;
import com.rt.printerlibrary.observer.PrinterObserverManager;
import com.rt.printerlibrary.printer.RTPrinter;
import com.rt.printerlibrary.utils.FuncUtils;
import com.rt.printerlibrary.setting.BarcodeSetting;
import com.rt.printerlibrary.setting.BitmapSetting;
import com.rt.printerlibrary.setting.CommonSetting;
import com.rt.printerlibrary.setting.TextSetting;

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
public class Printer extends CordovaPlugin implements PrinterObserver{
    private static final String LOG_TAG = "RTPrinter";


    @BaseEnum.CmdType
    private int currentCmdType = BaseEnum.CMD_ESC;//默认为针打

    @BaseEnum.ConnectType
    private int checkedConType = BaseEnum.CON_BLUETOOTH;
    BluetoothDevice currentBTDevice;
    private RTPrinter rtPrinter = null;
    private PrinterFactory printerFactory;
    private ArrayList<PrinterInterface> printerInterfaceArrayList = new ArrayList<PrinterInterface>();
    private PrinterInterface curPrinterInterface = null;
    private Object configObj;

    private BarcodeType barcodeType;
    private String barcodeContent;
    private PrintRotation printRotation = PrintRotation.Rotate0;
    private Bitmap mBitmap;
    private int bmpPrintWidth = 40;

    private TextSetting textSetting;
    private String mChartsetName = "UTF-8";
    private ESCFontTypeEnum curESCFontType = null;
    private String printStr;
    int lineSpacing = 30;


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            //listBT(callbackContext);
            return true;
        }
        if (action.equals("setBTPrinter")) {
            String name = args.getString(0);
            this.setBlueToothPrinter(name, callbackContext);
            //listBT(callbackContext);
            return true;
        }
        if (action.equals("init")) {
            initRT(callbackContext);
            return true;
        }
        if (action.equals("status")) {
            getPrinterStatus(callbackContext);
            return true;
        }
        if (action.equals("list")) {
            listBT(callbackContext);
            return true;
        }
        else if (action.equals("connect")) {
            //callbackContext.success("Printer connected successfully");
            doConnect(callbackContext);
            return true;
        }
        else if (action.equals("printtest")) {
            selfTestPrint(callbackContext);
            return true;
        }
        else if (action.equals("barcode")) {
            String code = args.getString(0);
            printBarCode(code, callbackContext);
            return true;
        }
        else if (action.equals("logo")) {
            printLogo(callbackContext);
            return true;
        }
        else if (action.equals("text")) {
            String text = args.getString(0);
            printText(text, callbackContext);
            return true;
        }
        else if (action.equals("printtype")) {
            int type = args.getInt(0);
            this.setPrinterType(type, callbackContext);
            return true;
        }
        else if (action.equals("textalign")) {
            int type = args.getInt(0);
            this.setTextAlign(type);
            return true;
        }
        else if (action.equals("conntype")) {
            int type = args.getInt(0);
            this.setConnectionType(type, callbackContext);
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

    public void initRT(CallbackContext callbackContext) {
        //初始化为针打printer
        //BaseApplication.instance.setCurrentCmdType(BaseEnum.CMD_ESC);
        printerFactory = new UniversalPrinterFactory();
        rtPrinter = printerFactory.create();

        //barcodeType = Enum.valueOf(BarcodeType.class, "UPC_A");
        barcodeType = Enum.valueOf(BarcodeType.class, "EAN13");

        textSetting = new TextSetting();

        PrinterObserverManager.getInstance().add(this);//Add connection status listener
        callbackContext.success("Print Module Initialized");
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

    private boolean isConfigPrintEnable(Object configObj) {
        if (isInConnectList(configObj)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isInConnectList(Object configObj) {
        boolean isInList = false;
        for (int i = 0; i < printerInterfaceArrayList.size(); i++) {
            PrinterInterface printerInterface = printerInterfaceArrayList.get(i);
            if (configObj.toString().equals(printerInterface.getConfigObject().toString())) {
                if (printerInterface.getConnectState() == ConnectStateEnum.Connected) {
                    isInList = true;
                    break;
                }
            }
        }
        return isInList;
    }

    private void setPrinterType(int type, CallbackContext callbackContext) {
        String errMsg = null;
        boolean set = true;
        this.currentCmdType = type;
        switch(type){
            case BaseEnum.CMD_PIN:
                printerFactory = new PinPrinterFactory();
                rtPrinter = printerFactory.create();
                rtPrinter.setPrinterInterface(curPrinterInterface);
            break;
            case BaseEnum.CMD_ESC:
                printerFactory = new ThermalPrinterFactory();
                rtPrinter = printerFactory.create();
                rtPrinter.setPrinterInterface(curPrinterInterface);             
            break;
            case BaseEnum.CMD_TSC:
                printerFactory = new LabelPrinterFactory();
                rtPrinter = printerFactory.create();
                rtPrinter.setPrinterInterface(curPrinterInterface);
            break;
            case BaseEnum.CMD_CPCL:
                printerFactory = new LabelPrinterFactory();
                rtPrinter = printerFactory.create();
                rtPrinter.setPrinterInterface(curPrinterInterface);
            break;
            case BaseEnum.CMD_ZPL:
                printerFactory = new LabelPrinterFactory();
                rtPrinter = printerFactory.create();
                rtPrinter.setPrinterInterface(curPrinterInterface);
            break;
            default:
                this.currentCmdType = BaseEnum.CMD_PIN;//默认为针打
                errMsg = "Unknown printer type selected";
				callbackContext.error(errMsg);
                set = false;
            break;
        }
        if (set) {
            callbackContext.success("Printer type set successfully");
        }
    }

    private void getPrinterType( CallbackContext callbackContext) {
        callbackContext.success(this.currentCmdType);
    }

    private void getPrinterStatus( CallbackContext callbackContext) {
        if (curPrinterInterface != null && curPrinterInterface.getConfigObject() != null){
           callbackContext.success("Printer status: " + rtPrinter.getConnectState()); 
        }

        callbackContext.error("Printer not initialized" );
    }
    

    private void setConnectionType(int type, CallbackContext callbackContext) {
        String errMsg = null;
        boolean set = true;
        switch(type){
            case BaseEnum.CON_WIFI:
                checkedConType = BaseEnum.CON_WIFI;
            break;
            case BaseEnum.CON_BLUETOOTH:
                checkedConType = BaseEnum.CON_BLUETOOTH;
            break;
            case BaseEnum.CON_USB:
                checkedConType = BaseEnum.CON_USB;
            break;
            case BaseEnum.CON_COM:
                checkedConType = BaseEnum.CON_COM;
            break;
            default:
                errMsg = "Unknown connection type";
				callbackContext.error(errMsg);
                set = false;
            break;
        } 
        if (set){
            callbackContext.success("Connection type set successfully");
         }
    }
    private void getConnectionType( CallbackContext callbackContext) {
        callbackContext.success(checkedConType);
    }

    private void setFontType(int type, CallbackContext callbackContext) {
        String errMsg = null;
        boolean set = true;
        switch(type){
            case 0:
                curESCFontType = null;
                break;
            case 1:
                curESCFontType = ESCFontTypeEnum.FONT_A_12x24;
                break;
            case 2:
                curESCFontType = ESCFontTypeEnum.FONT_B_9x24;
                break;
            case 3:
                curESCFontType = ESCFontTypeEnum.FONT_C_9x17;
                break;
            case 4:
                curESCFontType = ESCFontTypeEnum.FONT_D_8x16;
                break;
            default:
                curESCFontType = null;
                break;
        } 
        if (set){
            textSetting.setEscFontType(curESCFontType);
            callbackContext.success("Connection type set successfully");
         }
    }

    //This will return the array list of paired bluetooth printers
	void listBT(CallbackContext callbackContext) {
		BluetoothAdapter mBluetoothAdapter = null;
		String errMsg = null;
		try {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				errMsg = "No bluetooth adapter available";
				//Log.e(LOG_TAG, errMsg);
				callbackContext.error(errMsg);
				return;
			}
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
			}
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
				JSONArray json = new JSONArray();
				for (BluetoothDevice device : pairedDevices) {
					
					Hashtable map = new Hashtable();
					map.put("type", device.getType());
					map.put("address", device.getAddress());
					map.put("name", device.getName());
					JSONObject jObj = new JSONObject(map);
					json.put(jObj);
					//json.put(device.getName());
				}
				callbackContext.success(json);
			} else {
				callbackContext.error("No Bluetooth Device Found");
			}
			//Log.d(LOG_TAG, "Bluetooth Device Found: " + mmDevice.getName());
		} catch (Exception e) {
			errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}
	}

    boolean setBlueToothPrinter(String name, CallbackContext callbackContext) {
        BluetoothAdapter mBluetoothAdapter = null;
		String errMsg = null;
		try {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBluetoothAdapter == null) {
				Log.e(LOG_TAG, "No bluetooth adapter available");
			}
			if (!mBluetoothAdapter.isEnabled()) {
				Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
			}
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
			if (pairedDevices.size() > 0) {
				for (BluetoothDevice device : pairedDevices) {
					if (device.getName().equalsIgnoreCase(name)) {
						currentBTDevice = device;
                        configObj = new BluetoothEdrConfigBean(currentBTDevice);
                        callbackContext.success("Bluetooth Device Connected: " + currentBTDevice.getName());

                        
						return true;
					}
				}
			}
			callbackContext.error("Bluetooth Device Not Found: " + name);
		} catch (Exception e) {
			errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
		}
		return false;
	}

    private void doConnect(CallbackContext callbackContext) {
        String errMsg = null;
                   
        switch (checkedConType) {
            case BaseEnum.CON_WIFI:
                //WiFiConfigBean wiFiConfigBean = (WiFiConfigBean) configObj;
                //connectWifi(wiFiConfigBean);
                break;
            case BaseEnum.CON_BLUETOOTH:
                //TimeRecordUtils.record("RT连接start：", System.currentTimeMillis());
                if (configObj == null){
                    errMsg = "No printer selected. call 'setBTPrinter' first";
				    callbackContext.error(errMsg);
                } else {
                    BluetoothEdrConfigBean bluetoothEdrConfigBean = (BluetoothEdrConfigBean) configObj;
                    connectBluetooth(bluetoothEdrConfigBean, callbackContext);
                }
                break;
            case BaseEnum.CON_USB:
                //UsbConfigBean usbConfigBean = (UsbConfigBean) configObj;
                //connectUSB(usbConfigBean);
                break;
            default:
                errMsg = "No printer type selected";
				callbackContext.error(errMsg);
                            
                break;
        }

    }

    private void doDisConnect() {
        if (rtPrinter != null && rtPrinter.getPrinterInterface() != null) {
            rtPrinter.disConnect();
        }
    }

    private void connectBluetooth(BluetoothEdrConfigBean bluetoothEdrConfigBean, CallbackContext callbackContext) {
        PIFactory piFactory = new BluetoothFactory();
        PrinterInterface printerInterface = piFactory.create();
        printerInterface.setConfigObject(bluetoothEdrConfigBean);
        rtPrinter.setPrinterInterface(printerInterface);
        try {
            rtPrinter.connect(bluetoothEdrConfigBean);
            callbackContext.success("Printer connected successfully");
            showToast(printerInterface.getConfigObject().toString());
        } catch (Exception e) {
            String errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			//e.printStackTrace();
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
			callbackContext.error(sStackTrace);
        } finally {

        }
    }

    @Override
    public void printerObserverCallback(final PrinterInterface printerInterface, final int state) {
         cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                 switch (state) {
                    case CommonEnum.CONNECT_STATE_SUCCESS:
                        showToast(printerInterface.getConfigObject().toString() + "Connected");
                        //tv_device_selected.setText(printerInterface.getConfigObject().toString());
                       // tv_device_selected.setTag(BaseEnum.HAS_DEVICE);
                        curPrinterInterface = printerInterface;// set current Printer Interface
                        printerInterfaceArrayList.add(printerInterface);//Multiple connections - added to the connected list
                        rtPrinter.setPrinterInterface(printerInterface);
                        //BaseApplication.getInstance().setRtPrinter(rtPrinter);
                        //setPrintEnable(true);
                        break;
                    case CommonEnum.CONNECT_STATE_INTERRUPTED:
                        if (printerInterface != null && printerInterface.getConfigObject() != null) {
                            showToast(printerInterface.getConfigObject().toString() + "Disconnected");
                        } else {
                            showToast("Disconnected");
                        }
                        //tv_device_selected.setText(R.string.please_connect);
                        //tv_device_selected.setTag(BaseEnum.NO_DEVICE);
                        curPrinterInterface = null;
                        printerInterfaceArrayList.remove(printerInterface);//多连接-从已连接列表中移除
                        //BaseApplication.getInstance().setRtPrinter(null);
                        //setPrintEnable(false);
                        break;
                    default:
                        break;
                }               
            }
         });
    
    }

    @Override
    public void printerReadMsgCallback(PrinterInterface printerInterface, byte[] bytes) {

    }

    private void selfTestPrint(CallbackContext callbackContext) {
        if ( rtPrinter.getConnectState() != ConnectStateEnum.Connected){
            callbackContext.error("Printer not initialized" ); 
        }
        switch (this.currentCmdType) {
            case BaseEnum.CMD_PIN:
                pinSelftestPrint();
                break;
            case BaseEnum.CMD_ESC:
                escSelftestPrint();
                break;
            case BaseEnum.CMD_TSC:
                tscSelftestPrint();
                break;
            case BaseEnum.CMD_CPCL:
                cpclSelftestPrint();
                break;
            case BaseEnum.CMD_ZPL:
                zplSelftestPrint();
                break;
            default:
                break;
        }
        callbackContext.success(this.currentCmdType+" Printer started: " + rtPrinter.getConnectState()); 

    }

    private void cpclSelftestPrint() {
        CmdFactory cmdFactory = new CpclFactory();
        Cmd cmd = cmdFactory.create();
//        cmd.append(cmd.getCpclHeaderCmd(80,60,1));
        cmd.append(cmd.getSelfTestCmd());
        rtPrinter.writeMsgAsync(cmd.getAppendCmds());
    }

    private void zplSelftestPrint() {
        CmdFactory cmdFactory = new ZplFactory();
        Cmd cmd = cmdFactory.create();
        cmd.append(cmd.getHeaderCmd());
        cmd.append(cmd.getSelfTestCmd());
        cmd.append(cmd.getEndCmd());
        rtPrinter.writeMsgAsync(cmd.getAppendCmds());
    }

    private void tscSelftestPrint() {
        CmdFactory cmdFactory = new TscFactory();
        Cmd cmd = cmdFactory.create();
        cmd.append(cmd.getHeaderCmd());
        cmd.append(cmd.getLFCRCmd());
        cmd.append(cmd.getLFCRCmd());
        cmd.append(cmd.getSelfTestCmd());
        rtPrinter.writeMsgAsync(cmd.getAppendCmds());
    }

    private void escSelftestPrint() {
        CmdFactory cmdFactory = new EscFactory();
        Cmd cmd = cmdFactory.create();
        cmd.append(cmd.getHeaderCmd());
        cmd.append(cmd.getLFCRCmd());
        cmd.append(cmd.getSelfTestCmd());
        cmd.append(cmd.getLFCRCmd());
        rtPrinter.writeMsgAsync(cmd.getAppendCmds());
    }

    private void pinSelftestPrint() {
        CmdFactory cmdFactory = new PinFactory();
        Cmd cmd = cmdFactory.create();
        cmd.append(cmd.getHeaderCmd());
        cmd.append(cmd.getLFCRCmd());
        cmd.append(cmd.getLFCRCmd());
        cmd.append(cmd.getSelfTestCmd());
        rtPrinter.writeMsgAsync(cmd.getAppendCmds());
    }
    private void printBarCode(String code, CallbackContext callbackContext) {
        if ( rtPrinter.getConnectState() != ConnectStateEnum.Connected){
            callbackContext.error("Printer not initialized" ); 
        }

        try{
           switch ( this.currentCmdType ) {
            case BaseEnum.CMD_ESC:
            case BaseEnum.CMD_PIN:
                escPrintBarcode(code);
                tscPrintBarcode(code);
                cpclPrintBarcode(code);
                zplPrintBarcode(code);
                break;
            case BaseEnum.CMD_TSC:
                tscPrintBarcode(code);
                break;
            case BaseEnum.CMD_CPCL:
                cpclPrintBarcode(code);
                break;
            case BaseEnum.CMD_ZPL:
                zplPrintBarcode(code);
                break;
            default:
                break;
            }   
        } catch (SdkException e) {
            String errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
            return;
        }        
        callbackContext.success("Barcode completed ");
    }

     private void tscPrintBarcode(String barcodeContent) throws SdkException {
        int labelWidth = 80;
        int labelHeight = 40;

        CmdFactory tscFac = new TscFactory();
        Cmd tscCmd = tscFac.create();

        tscCmd.append(tscCmd.getHeaderCmd());
        CommonSetting commonSetting = new CommonSetting();
        commonSetting.setLableSizeBean(new LableSizeBean(labelWidth, labelHeight));
        commonSetting.setLabelGap(3);
        commonSetting.setPrintDirection(PrintDirection.NORMAL);
        tscCmd.append(tscCmd.getCommonSettingCmd(commonSetting));
        BarcodeSetting barcodeSetting = new BarcodeSetting();
        barcodeSetting.setNarrowInDot(2);//narrow bar setting, bar width
        barcodeSetting.setWideInDot(4);
        barcodeSetting.setHeightInDot(48);//bar height setting
        barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.BELOW_BARCODE);
        barcodeSetting.setPrintRotation(printRotation);
        int x = 30, y = 80;
        switch (printRotation) {
            case Rotate0:
                x = 30;
                y = 80;
                break;
            case Rotate90:
                x = (labelWidth * 8) / 2;
                y = 20;
                break;
            case Rotate270:
                x = (labelWidth * 8) / 2;
                y = (labelHeight * 8) - 20;
                break;
            default:
                break;
        }
        barcodeSetting.setPosition(new Position(x, y));
        byte[] barcodeCmd = tscCmd.getBarcodeCmd(barcodeType, barcodeSetting, barcodeContent);
        tscCmd.append(barcodeCmd);

        tscCmd.append(tscCmd.getPrintCopies(1));
        tscCmd.append(tscCmd.getEndCmd());
        if (rtPrinter != null) {
            rtPrinter.writeMsgAsync(tscCmd.getAppendCmds());
        }
    }

      private void zplPrintBarcode(String barcodeContent) throws SdkException {
        int labelWidth = 80;
        int labelHeight = 40;

        CmdFactory zplFac = new ZplFactory();
        Cmd zplCmd = zplFac.create();

        zplCmd.append(zplCmd.getHeaderCmd());
        CommonSetting commonSetting = new CommonSetting();
        commonSetting.setLableSizeBean(new LableSizeBean(labelWidth, labelHeight));
        commonSetting.setLabelGap(2);
        commonSetting.setPrintDirection(PrintDirection.NORMAL);
        zplCmd.append(zplCmd.getCommonSettingCmd(commonSetting));
        BarcodeSetting barcodeSetting = new BarcodeSetting();
        barcodeSetting.setHeightInDot(48);
        barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.BELOW_BARCODE);
        barcodeSetting.setPrintRotation(printRotation);
        int x = 10, y = 80;
        switch (printRotation) {
            case Rotate0:
                x = 10;
                y = 10;
                break;
            case Rotate90:
                x = (labelWidth * 8) / 2;
                y = 20;
                break;
            case Rotate270:
                x = (labelWidth * 8) / 2;
                y = (labelHeight * 8) - 20;
                break;
            default:
                break;
        }
        barcodeSetting.setPosition(new Position(x, y));
        byte[] barcodeCmd = zplCmd.getBarcodeCmd(barcodeType, barcodeSetting, barcodeContent);
        zplCmd.append(barcodeCmd);

        zplCmd.append(zplCmd.getPrintCopies(1));
        zplCmd.append(zplCmd.getEndCmd());
        if (rtPrinter != null) {
            rtPrinter.writeMsgAsync(zplCmd.getAppendCmds());
        }
    }

    private void cpclPrintBarcode(String barcodeContent) throws SdkException {
        CmdFactory cpclFac = new CpclFactory();
        Cmd cmd = cpclFac.create();
        String labelSizeStr = "80*40", labelWidth="80", labelHeight="40", labelSpeed="2", labelType="CPCL", labelOffset="0";
        cmd.append(cmd.getCpclHeaderCmd(Integer.parseInt(labelWidth), Integer.parseInt(labelHeight), 1, Integer.parseInt(labelOffset)));
        BarcodeSetting barcodeSetting = new BarcodeSetting();
        barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.NONE);
        barcodeSetting.setPrintRotation(printRotation);
        barcodeSetting.setNarrowInDot(2);//narrow bar width
        barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.BELOW_BARCODE );
        if (printRotation == PrintRotation.Rotate0) {
            barcodeSetting.setPosition(new Position(10, 20));//bar height setting
        } else {
            barcodeSetting.setPosition(new Position(300, 300));//bar height setting
        }
        barcodeSetting.setHeightInDot(48);
        byte[] barcodeCmd = cmd.getBarcodeCmd(barcodeType, barcodeSetting, barcodeContent);
        cmd.append(barcodeCmd);


        cmd.append(cmd.getEndCmd());
        if (rtPrinter != null) {
            rtPrinter.writeMsgAsync(cmd.getAppendCmds());
        }
    }

    private void escPrintBarcode(String barcodeContent) throws SdkException {
        CmdFactory cmdFactory = new EscFactory();
        Cmd escCmd = cmdFactory.create();
        escCmd.append(escCmd.getHeaderCmd());

        CommonSetting commonSetting = new CommonSetting();
        commonSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
        escCmd.append(escCmd.getCommonSettingCmd(commonSetting));        


        BarcodeSetting barcodeSetting = new BarcodeSetting();
        barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.BELOW_BARCODE);
        barcodeSetting.setHeightInDot(72);//accept value:1~255
        barcodeSetting.setBarcodeWidth(3);//accept value:2~6
        barcodeSetting.setQrcodeDotSize(5);//accept value: Esc(1~15), Tsc(1~10)
        try {
            escCmd.append(escCmd.getBarcodeCmd(barcodeType, barcodeSetting, barcodeContent));
           // escCmd.append(escCmd.getBarcodeCmd(Enum.valueOf(BarcodeType.class, "EAN13"), barcodeSetting, "123456789"));
            //escCmd.append(escCmd.getBarcodeCmd(Enum.valueOf(BarcodeType.class, "CODE39"), barcodeSetting, "987654321"));
            escCmd.append(escCmd.getBarcodeCmd(Enum.valueOf(BarcodeType.class, "UPC_A"), barcodeSetting, "12909120912"));
            escCmd.append(escCmd.getBarcodeCmd(Enum.valueOf(BarcodeType.class, "ITF"), barcodeSetting, "12345678901234"));
        } catch (SdkException e) {
            e.printStackTrace();
            String errMsg = e.getMessage();
            showToast(errMsg);
        }
        escCmd.append(escCmd.getLFCRCmd());
        escCmd.append(escCmd.getLFCRCmd());
        //escCmd.append(escCmd.getLFCRCmd());
        //escCmd.append(escCmd.getLFCRCmd());
        //escCmd.append(escCmd.getLFCRCmd());
        //escCmd.append(escCmd.getLFCRCmd());

        rtPrinter.writeMsgAsync(escCmd.getAppendCmds());
    }


    private void printLogo(CallbackContext callbackContext) {
        if ( rtPrinter.getConnectState() != ConnectStateEnum.Connected){
            callbackContext.error("Printer not initialized" ); 
        }

        try{
           printImageForInd();
        } catch (SdkException e) {
            String errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
            return;
        }        
        callbackContext.success("Logo print completed ");
    }


    private void printImageForInd() throws SdkException {
        Resources activityRes = cordova.getActivity().getResources();
        int logoResId = activityRes.getIdentifier("pebuu_africa", "drawable", cordova.getActivity().getPackageName());


        mBitmap = BitmapFactory.decodeResource(activityRes, logoResId);
        if (mBitmap == null) {//未选择图片
            showToast("No image has been configured");
            return;
        }
        
        switch (this.currentCmdType) {
            case BaseEnum.CMD_PIN:
                pinPrintImage();
                break;
            case BaseEnum.CMD_ESC:
                escPrintImage();
                break;
            case BaseEnum.CMD_TSC:
                tscPrintImage();
                break;
            case BaseEnum.CMD_CPCL:
                cpclPrintImage();
                break;
            case BaseEnum.CMD_ZPL:
                zplPrintImage();
                break;
            default:
                break;
        }
    }

    private void escPrintImage() throws SdkException {
                CmdFactory cmdFactory = new EscFactory();
                Cmd cmd = cmdFactory.create();
                cmd.append(cmd.getHeaderCmd());

                CommonSetting commonSetting = new CommonSetting();
                commonSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
                cmd.append(cmd.getCommonSettingCmd(commonSetting));

                BitmapSetting bitmapSetting = new BitmapSetting();

                /**
                 * MODE_MULTI_COLOR - 适合多阶灰度打印<br/> Suitable for multi-level grayscale printing<br/>
                 * MODE_SINGLE_COLOR-适合白纸黑字打印<br/>Suitable for printing black and white paper
                 */
                bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_MULTI_COLOR);
                // bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_MULTI_COLOR);

               if (bmpPrintWidth > 72) {
                    bmpPrintWidth = 72;
                }


                bitmapSetting.setBimtapLimitWidth(bmpPrintWidth * 8);
                try {
                    cmd.append(cmd.getBitmapCmd(bitmapSetting, mBitmap));
                } catch (SdkException e) {
                    e.printStackTrace();
                    showToast(e.getMessage());
                }
                cmd.append(cmd.getLFCRCmd());
                cmd.append(cmd.getLFCRCmd());
                if (rtPrinter != null) {
                    rtPrinter.writeMsg(cmd.getAppendCmds());//Sync Write
                }

    }

    private void pinPrintImage() throws SdkException {

                CmdFactory cmdFactory = new PinFactory();
                Cmd cmd = cmdFactory.create();
                // cmd.append(cmd.getHeaderCmd());

                int limitDots = 2*8;
                BitmapSetting bitmapSetting = new BitmapSetting();
                CommonSetting commonSetting = new CommonSetting();



                commonSetting.setAlign(CommonEnum.ALIGN_MIDDLE);//Left, Middle, Right
                cmd.append(cmd.getCommonSettingCmd(commonSetting));

                cmd.append(cmd.getLFCRCmd());
                cmd.append(cmd.getLFCRCmd());


                bitmapSetting.setBimtapLimitWidth(limitDots);

                try {
                    cmd.append(cmd.getBitmapCmd(bitmapSetting, mBitmap));
                } catch (SdkException e) {
                    e.printStackTrace();
                }
                cmd.append(cmd.getEndCmd());

                rtPrinter.writeMsg(cmd.getAppendCmds());

    }

   private void zplPrintImage() throws SdkException {

        CmdFactory zplFac = new ZplFactory();
        Cmd zplCmd = zplFac.create();

        zplCmd.append(zplCmd.getHeaderCmd());
        CommonSetting commonSetting = new CommonSetting();
        commonSetting.setLableSizeBean(new LableSizeBean(80, 80));
        commonSetting.setLabelGap(2);
        commonSetting.setPrintDirection(PrintDirection.NORMAL);
        zplCmd.append(zplCmd.getHeaderCmd());
        zplCmd.append(zplCmd.getCommonSettingCmd(commonSetting));


        BitmapSetting bitmapSetting = new BitmapSetting();
        bitmapSetting.setPrintPostion(new Position(20, 20));
        bitmapSetting.setBimtapLimitWidth(bmpPrintWidth * 8);
        try {
            zplCmd.append(zplCmd.getBitmapCmd(bitmapSetting, mBitmap));
            zplCmd.append(zplCmd.getPrintCopies(1));
        } catch (SdkException e) {
            e.printStackTrace();
        }

        zplCmd.append(zplCmd.getEndCmd());
        if (rtPrinter != null) {
            rtPrinter.writeMsg(zplCmd.getAppendCmds());
        }
 
    }

    private void tscPrintImage() throws SdkException {

        CmdFactory tscFac = new TscFactory();
        Cmd tscCmd = tscFac.create();

        tscCmd.append(tscCmd.getHeaderCmd());
        CommonSetting commonSetting = new CommonSetting();
        commonSetting.setLableSizeBean(new LableSizeBean(80, 40));
        commonSetting.setLabelGap(2);
        commonSetting.setPrintDirection(PrintDirection.NORMAL);
        tscCmd.append(tscCmd.getHeaderCmd());
        tscCmd.append(tscCmd.getCommonSettingCmd(commonSetting));


        BitmapSetting bitmapSetting = new BitmapSetting();
        bitmapSetting.setPrintPostion(new Position(20, 80));
        bitmapSetting.setBimtapLimitWidth(bmpPrintWidth * 8);
        bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_SINGLE_COLOR);
        try {
            tscCmd.append(tscCmd.getBitmapCmd(bitmapSetting, mBitmap));
            tscCmd.append(tscCmd.getPrintCopies(1));//Print Copies setting
        } catch (SdkException e) {
            e.printStackTrace();
        }

        if (rtPrinter != null) {
            rtPrinter.writeMsg(tscCmd.getAppendCmds());
        }

    }
  
   private void cpclPrintImage() throws SdkException {

        CmdFactory cpclFactory = new CpclFactory();
        Cmd cmd = cpclFactory.create();

        cmd.append(cmd.getCpclHeaderCmd(80, 60, 1, 0));
        BitmapSetting bitmapSetting = new BitmapSetting();
        bitmapSetting.setPrintPostion(new Position(20, 20));
        bitmapSetting.setBimtapLimitWidth(bmpPrintWidth * 8);
        bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_SINGLE_COLOR);
        try {
            cmd.append(cmd.getBitmapCmd(bitmapSetting, mBitmap));
        } catch (SdkException e) {
            e.printStackTrace();
        }
        cmd.append(cmd.getEndCmd());
        if (rtPrinter != null) {
            rtPrinter.writeMsg(cmd.getAppendCmds());
        }
    }

     private void printText(String text, CallbackContext callbackContext) {
        if ( rtPrinter.getConnectState() != ConnectStateEnum.Connected){
            callbackContext.error("Printer not initialized" ); 
        }

        printStr = text;

        if (TextUtils.isEmpty(printStr)) {
            callbackContext.error("Empty text");
            return;
        }

        try{
            switch (this.currentCmdType) {
                case BaseEnum.CMD_ESC:
                    escPrintText();
                    callbackContext.success("Text printing completed ");
                    break;
                default:
                    callbackContext.error("Not supported");
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            String errMsg = e.getMessage();
			Log.e(LOG_TAG, errMsg);
			e.printStackTrace();
			callbackContext.error(errMsg);
            return;
        } 
    }

    private void escPrintText() throws UnsupportedEncodingException {
        
            CmdFactory escFac = new EscFactory();
            Cmd escCmd = escFac.create();
            escCmd.append(escCmd.getHeaderCmd());//初始化, Initial

            escCmd.setChartsetName(mChartsetName);

            CommonSetting commonSetting = new CommonSetting();
            commonSetting.setEscLineSpacing(getInputLineSpacing());
            escCmd.append(escCmd.getCommonSettingCmd(commonSetting));

            escCmd.append(escCmd.getTextCmd(textSetting, printStr));

            escCmd.append(escCmd.getLFCRCmd());
            escCmd.append(escCmd.getHeaderCmd());//初始化, Initial
            //escCmd.append(escCmd.getLFCRCmd());

            rtPrinter.writeMsg(escCmd.getAppendCmds());
        
    }

    /**
     * line spacing setting
     */
    private int getInputLineSpacing() {
        return lineSpacing;
    }
    private void setInputLineSpacing(int n) {
        if (n > 255) {
            n = 255;
        }
        lineSpacing =  n;
    }

   private void setTextAlign(int i) {
        switch (i) {
            case 0:
                textSetting.setAlign(CommonEnum.ALIGN_LEFT);
                break;
            case 1:
                textSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
                break;
            case 2:
                textSetting.setAlign(CommonEnum.ALIGN_RIGHT);
                break;
            default:
                break;
        }
    }

}
