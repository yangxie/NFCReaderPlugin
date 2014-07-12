package com.fivestars.mtab.plugin;

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

    private UsbManager mManager;
    private Reader mReader;
    private PendingIntent mPermissionIntent;
    private UsbBroadcastReceiver mReceiver;

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
                mPermissionIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(ACTION_USB_PERMISSION), 0);
                mReceiver= new UsbBroadcastReceiver(callbackContext, cordova.getActivity());
                IntentFilter filter = new IntentFilter();
                filter.addAction(ACTION_USB_PERMISSION);
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
                cordova.getActivity().registerReceiver(mReceiver, filter);

                for (UsbDevice device : mManager.getDeviceList().values()) {
                    if (mReader.isSupported(device)) {
                        // Request permission
                        mManager.requestPermission(device, mPermissionIntent);
                    }
                }
                callbackContext.error("No device found!");
            }
        });
    }

    private void registerReadCallback(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                //readCallback = callbackContext;
                JSONObject returnObj = new JSONObject();
                addProperty(returnObj, "registerReadCallback", "true");
                // Keep the callback
                PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, returnObj);
                pluginResult.setKeepCallback(true);
                callbackContext.sendPluginResult(pluginResult);
            }
        });
    }

    /**
     * Utility method to add some properties to a {@link JSONObject}
     * @param obj the json object where to add the new property
     * @param key property key
     * @param value value of the property
     */
    private void addProperty(JSONObject obj, String key, Object value) {
        try {
            obj.put(key, value);
        }
        catch (JSONException e){}
    }
}
