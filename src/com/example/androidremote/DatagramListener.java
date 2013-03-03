package com.example.androidremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import android.app.Activity;
import android.util.Log;

public class DatagramListener extends Thread {
	private Activity activity;                     /* so we can run callback on ui thread */
	private int port;
	private DatagramCallback reqCallback;
	private byte[] packetBuffer = new byte[1024];
	
	public DatagramListener(Activity activity, int port, DatagramCallback reqCallback) {
		this.port = port;
		this.reqCallback = reqCallback;
		this.activity = activity;
	}
	
	public void run() {
		DatagramSocket dg = null;
		try {
			dg = new DatagramSocket(port);
			DatagramPacket received = new DatagramPacket(packetBuffer, packetBuffer.length);
			while (true) {
				dg.receive(received);
				activity.runOnUiThread(new RunnableOneArg(new DatagramPacket(received.getData(), received.getLength(), received.getAddress(), received.getPort())) {
					public void run() {
						reqCallback.callback((DatagramPacket)getArg());
					}
				});
				
			}
		} catch(Exception e) {
			Log.e(this.getClass().getName(), "Datagram failed");
		} finally {
			dg.close();
		}
	}
}
