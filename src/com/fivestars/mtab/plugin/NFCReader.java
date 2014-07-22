package com.fivestars.mtab.plugin;

import java.io.UnsupportedEncodingException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.acs.smartcard.Reader;
import com.acs.smartcard.ReaderException;

public class NFCReader extends CordovaPlugin {
    private static final String ACTION_REQUEST_PERMISSION = "requestPermission";
    private static final String ACTION_READ_CALLBACK = "registerReadCallback";
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    
    
    final private static char[] hexArray = "0123456789abcdef".toCharArray();

    private UsbManager mManager;
    private Reader mReader;
    private PendingIntent mPermissionIntent;
    private UsbBroadcastReceiver mReceiver;
    
    private CallbackContext readCallback;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException { 
        JSONObject arg_object = args.optJSONObject(0);
        // request permission
        if (ACTION_REQUEST_PERMISSION.equals(action)) {
            requestPermission(callbackContext);
            return true;
        }
        // Register read callback
        else if (ACTION_READ_CALLBACK.equals(action)) {
            registerReadCallback(callbackContext);
            return true;
        }
        // the action doesn't exist
        return false;
    }

    private void requestPermission(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                mManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
                mReader = new Reader(mManager);
                setupReader();
                mPermissionIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                mReceiver= new UsbBroadcastReceiver(callbackContext, cordova.getActivity(), mReader);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_USB_PERMISSION);
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                cordova.getActivity().registerReceiver(mReceiver, filter);
                
                boolean requested = false;

                for (UsbDevice device : mManager.getDeviceList().values()) {
                    if (mReader.isSupported(device)) {
                        // Request permission
                    	requested = true;
                        mManager.requestPermission(device, mPermissionIntent);
                        break;
                    }
                }
                
                if (!requested) {
                	callbackContext.error("No device found!");
                }
            }
        });
    }
    
    private void registerReadCallback(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                readCallback = callbackContext;
                JSONObject returnObj = new JSONObject();
                // Keep the callback
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
    }
    
    /**
     * Setup reader when reader state changed. For now we only care when a card present
     * 
     */
    private void setupReader() {
    	mReader.setOnStateChangeListener(new Reader.OnStateChangeListener() {
            @Override
            public void onStateChange(int slotNum, int prevState, int currState) {
                // Create output string
                byte[] response = new byte[300];
                if (currState == Reader.CARD_PRESENT) {
                    response = readFromCard(slotNum);
                    updateReceivedData(bytesToHex(response).substring(0, 14));
                }
            }
        });
    }

    public byte[] readFromCard(int slotNum) {
        byte[] command = {(byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x08 };
        byte[] response = new byte[300];
        try {
        	mReader.power(slotNum, Reader.CARD_WARM_RESET);
            mReader.setProtocol(slotNum, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
            mReader.transmit(slotNum, command, command.length, response,
                    response.length);
        } catch (ReaderException e) {
            e.printStackTrace();
        }
        return response;
    }
    
    private void updateReceivedData(String data) {
        if( readCallback != null ) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, data);
            result.setKeepCallback(true);
            readCallback.sendPluginResult(result);
        }
    }

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

}
