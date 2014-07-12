package com.fivestars.mtab.plugin;

import org.apache.cordova.CallbackContext;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public class UsbBroadcastReceiver extends BroadcastReceiver{
	private CallbackContext callbackContext;
	private Activity activity;
	 
	private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	 
	
	public UsbBroadcastReceiver(CallbackContext callbackContext, Activity activity) {
		this.callbackContext = callbackContext;
		this.activity = activity;
	}
	 
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
             synchronized (this) {
                 UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                 if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                     if(device != null){
                    	 callbackContext.success("Permission to connect to the device was accepted!");
                     }
                 }
                 else {
                     callbackContext.error("permission denied for device " + device);
                 }
            }
        }
    }
}
