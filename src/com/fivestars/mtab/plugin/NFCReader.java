package com.fivestars.mtab.plugin;


import android.app.PendingIntent;
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
    
    private boolean[] connected = new boolean[1];
    private boolean[] permissionCount = new boolean[1];
   
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        // request permission
        if (ACTION_REQUEST_PERMISSION.equals(action)) {
            if (mReader == null || mManager == null) {
            	initReader();
            }
            
            requestPermission(callbackContext);
            return true;
        }
        // Register read callback
        else if (ACTION_READ_CALLBACK.equals(action)) {
        	if (connected[0]){
        		registerReadCallback(callbackContext);
        	}
            return true;
        }
        // the action doesn't exist
        return false;
    }
    
    /**
     * init reader
     */
    public void initReader() {
    	mManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
        mReader = new Reader(mManager);
        setupReader();
    }
    
    /**
     * request permission to connect usb device.
     * @param callbackContext
     */
    private void requestPermission(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
            	mPermissionIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_USB_PERMISSION);
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            	mReceiver= new UsbBroadcastReceiver(callbackContext, cordova.getActivity(), mReader, connected, permissionCount);
                cordova.getActivity().registerReceiver(mReceiver, filter);
            
                
                boolean requested = false;

                for (UsbDevice device : mManager.getDeviceList().values()) {
                    if (mReader.isSupported(device)) {
                        // Request permission
                    	requested = true;
                    	if (!permissionCount[0]) {
                        	permissionCount[0] = true;
                        	mManager.requestPermission(device, mPermissionIntent);
                        }else {
                        	callbackContext.error("Already requesting permission now");
                        }
                    	
                        break;
                    }
                }
                
                if (!requested) {
                	callbackContext.error("No device found!");
                }
            }
        });
    }
    
    /**
     * register callback function, which will be called when we scanning a card.
     * @param callbackContext
     */
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
     * Setup card state change listener. For now we only care when a card present
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
                    updateReceivedData(bytesToHex(response).substring(0, 14), readCallback);
                }
            }
        });
    }
    
    /**
     * read card uid from card
     * @param slotNum
     * @return
     */
    public byte[] readFromCard(int slotNum) {
    	// read card uid command
        byte[] command = {(byte) 0xFF, (byte) 0xCA, 0x00, 0x00, 0x08 };
        byte[] response = new byte[300];
        try {
        	// activate card
        	mReader.power(slotNum, Reader.CARD_WARM_RESET);
        	// setup protocol
            mReader.setProtocol(slotNum, Reader.PROTOCOL_T0 | Reader.PROTOCOL_T1);
            // read data from card
            mReader.transmit(slotNum, command, command.length, response,
                    response.length);
        } catch (ReaderException e) {
            e.printStackTrace();
        }
        return response;
    }
    
    /**
     * send data back via registered callback
     * @param data
     */
    private void updateReceivedData(String data, CallbackContext callback) {
        if( callback != null ) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, data);
            result.setKeepCallback(true);
            callback.sendPluginResult(result);
        }
    }
    
    /**
     * Convert bytes to hex string
     * @param bytes
     * @return
     */
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
