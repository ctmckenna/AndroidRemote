package com.foggyciti.macremote;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

public abstract class BroadcastHandler {
	private Context c;
	private static BroadcastHandler broadcastHandler = null;
	
	public abstract InetAddress getBroadcastAddress() throws Exception;
	
	public static BroadcastHandler getInstance(Context c) {
		final int sdkVersion = Build.VERSION.SDK_INT;
		if (broadcastHandler != null)
			return broadcastHandler;
		if (sdkVersion < Build.VERSION_CODES.GINGERBREAD)
			broadcastHandler = new FroyoBroadcastHandler(c);
		else
			broadcastHandler = new GingerbreadBroadcastHandler(c);
		return broadcastHandler;
	}
	
	private static class FroyoBroadcastHandler extends BroadcastHandler {
		
		public FroyoBroadcastHandler(Context c) {
			super.c = c;
		}
		
		public InetAddress getBroadcastAddress() throws Exception {
			InetAddress wifiAddress = NetworkService.getWifiAddress(super.c);
			return InetAddress.getByAddress(NetworkService.orOperation(wifiAddress.getAddress(), NetworkService.inverseOperation(NetworkService.getNetmask(super.c))));
		}
	}
	
	private static class GingerbreadBroadcastHandler extends BroadcastHandler {
		
		public GingerbreadBroadcastHandler(Context c) {
			super.c = c;
		}
		
		@TargetApi(Build.VERSION_CODES.GINGERBREAD)
		public InetAddress getBroadcastAddress() throws Exception {
			InetAddress wifiAddress = NetworkService.getWifiAddress(super.c);
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				if (intf.isLoopback())
					continue;
				for (InterfaceAddress intfAddr : intf.getInterfaceAddresses()) {
					if (wifiAddress.equals(intfAddr.getAddress())) {
						return intfAddr.getBroadcast();
					}
				}
			}
			return null;
		}
	}
}
