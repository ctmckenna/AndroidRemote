package com.foggyciti.macremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetworkService {
	private InetAddress connectedServerAddr = null;
	private Callback onConnect;
	private Callback onDisconnect;
	private Callback onPending;
	private Activity activity;
	private DatagramSocket socket = null;
	private DatagramBuffer pingBuffer = new DatagramBuffer();
	
	private static final int MILLIS_BEFORE_PING = 3000;
	private static final int MILLIS_BEFORE_PING_RETRY = 500;
	private static final int CONNECT_TO_PENDING_UNACK = 5;
	private static final int PENDING_TO_DISCONNECT_UNACK = 10;
	
	private static final int SERVER_PORT = 10265;
	
	private static final String PING_RESP_EXPECTED = ":-)";
	
	private static long lastSuccess = 0;
	
	private DatagramListener listener = null;
	
	private ScheduledThreadPoolExecutor scheduledTimer = new ScheduledThreadPoolExecutor(1);
	private PingTimeout timeoutTask = new PingTimeout();
	
	private PingRetryRunnable pingRetry = new PingRetryRunnable();
	
	private int unackPings = 0;
	
	public NetworkService(Activity activity, String pingRequest, Callback onConnect, Callback onDisconnect, Callback onPending) {
		this.onConnect = onConnect;
		this.onDisconnect = onDisconnect;
		this.onPending = onPending;
		this.activity = activity;
		pingBuffer.reset();
		pingBuffer.copyByte(RemoteEvent.PING.getId());
		pingBuffer.copyData(pingRequest.getBytes());
		try {
			socket = new DatagramSocket();
		} catch(Exception ex) {
			onDisconnect.callback();
		}
	}
	
	private class PingRetryRunnable implements Runnable {
		public void run() {
			if (Calendar.getInstance().getTimeInMillis()-lastSuccess < MILLIS_BEFORE_PING)
				return;
			++unackPings;
			if (unackPings == PENDING_TO_DISCONNECT_UNACK) {
				connectedServerAddr = null;
				onDisconnect.callback();
			} else if (unackPings == CONNECT_TO_PENDING_UNACK) {
				onPending.callback();
			}
			try {
				InetAddress pingAddr;
				if (connectedServerAddr == null)
					pingAddr = getBroadcastAddress();
				else
					pingAddr = connectedServerAddr;
				sendPing(pingAddr);
			} catch (Exception ex) {
				onDisconnect.callback();
			}
		}
	}
		
	private class PingResponseHandler implements DatagramCallback {
		@Override
		public void callback(DatagramPacket packet) {
			String msg = new String(packet.getData(), 0, packet.getLength());
			if (0 != msg.compareTo(PING_RESP_EXPECTED))
				return;
			lastSuccess = Calendar.getInstance().getTimeInMillis();
			scheduledTimer.remove(timeoutTask);
			scheduledTimer.schedule(timeoutTask, MILLIS_BEFORE_PING, TimeUnit.MILLISECONDS);
			unackPings = 0;
			if (connectedServerAddr == null)
				connectedServerAddr = packet.getAddress();
				
			onConnect.callback();
		}
	}
		
	private class PingTimeout implements Runnable {
		public void run() {
			activity.runOnUiThread(pingRetry);
		}
	}
	
	private void send(byte[] buf, int buf_len, InetAddress address, int port) {
		try {
			DatagramPacket sendPacket = new DatagramPacket(buf, buf_len, address, port);
			socket.send(sendPacket);
		} catch(Exception ex) {
			onDisconnect.callback();
		}
	}
	
	public void cleanup() {
		listener.kill();
	}
	
	public void send(DatagramBuffer sendBuffer) {
		send(sendBuffer.toArray(), sendBuffer.length(), connectedServerAddr, SERVER_PORT);
	}
	
	/* two possible addresses for ping request */
	private void sendPing(InetAddress addr) {
		scheduledTimer.schedule(timeoutTask, MILLIS_BEFORE_PING_RETRY, TimeUnit.MILLISECONDS);
		send(pingBuffer.toArray(), pingBuffer.length(), addr, SERVER_PORT);
	}
	
	private byte[] intToByteArray(int integer) {
		byte[] byteArr = new byte[4];
		for (int i = 0; i < 4; i++) {
			byteArr[i] = (byte)(integer >>> (8 * i) & 0xFF);
		}
		return byteArr;
	}
	
	private InetAddress getWifiAddress() throws UnknownHostException {
		WifiManager wifiManager = (WifiManager)activity.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		Integer wifiIp = wifiInfo.getIpAddress();
		return InetAddress.getByAddress(intToByteArray(wifiIp));
	}
	
	private InetAddress getBroadcastAddress() throws Exception {
		InetAddress wifiAddress = getWifiAddress();
		for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
			NetworkInterface intf = en.nextElement();
			if (intf.isLoopback()) continue;
			for (InterfaceAddress intfAddr : intf.getInterfaceAddresses()) {
				if (wifiAddress.equals(intfAddr.getAddress())) {
					return intfAddr.getBroadcast();
				}
			}
		}
		return null;
	}
	
	public InetAddress findLanServerAddr() {
		try {
			InetAddress broadcastAddr = getBroadcastAddress();
			
			listener = new DatagramListener(activity, SERVER_PORT+1, new PingResponseHandler());
			listener.start();
			
			sendPing(broadcastAddr);
			
			
			
		} catch (Exception ex) {
			onDisconnect.callback();
		}
		return null;
	}
}
