package com.fivestars.mtab.plugin;

import org.apache.cordova.CallbackContext;

import com.acs.smartcard.Reader;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public class UsbBroadcastReceiver extends BroadcastReceiver{
	private CallbackContext callbackContext;
	private Activity activity;
	private Reader mReader;
	private boolean[] connected;
	 
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
	
	public UsbBroadcastReceiver(CallbackContext callbackContext, Activity activity, Reader mReader, boolean[] connected) {
		this.callbackContext = callbackContext;
		this.activity = activity;
		this.mReader = mReader;
		this.connected = connected;
	}
	 
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
             synchronized (this) {
                 UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                 if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                	 try {
                		 // open reader
                		 mReader.open(device);
                		 connected[0] = true;
                		 callbackContext.success("Permission to connect to the device was accepted!");
                	 }catch (Exception e) {
                		 callbackContext.error("Permission to connect to the device was accepted, but open reader failed");
                     }
                 }
                 else {
                     callbackContext.error("permission denied for device " + device);
                 }
            }
        }else if (ACTION_USB_DETACHED.equals(action)) {
        	synchronized (this) {
        		connected[0] = false;
        	}
        }
    }
}
