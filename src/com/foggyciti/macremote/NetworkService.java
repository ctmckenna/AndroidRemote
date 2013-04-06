package com.foggyciti.macremote;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;

public class NetworkService {
	private InetAddress connectedServerAddr = null;
	private Callback onConnect;
	private Callback onDisconnect;
	private Callback onPending;
	private Activity activity;
	private DatagramBuffer pingBuffer = new DatagramBuffer();
	
	private static final int MILLIS_BEFORE_PING = 3000;
	private static final int MILLIS_BEFORE_PING_RETRY = 500;
	private static final int CONNECT_TO_PENDING_UNACK = 2;
	private static final int PENDING_TO_DISCONNECT_UNACK = 8;
	
	private static final int SERVER_PORT = 10265;
	
	private static final String PING_RESP_EXPECTED = ":-)";
	
	private static long currentPingInterval = MILLIS_BEFORE_PING_RETRY;
	
	private DatagramListener listener = null;
	
	private Handler timeoutHandler = new Handler();
	
	private PingRetryRunnable pingRetry = new PingRetryRunnable();
	
	private String lastPingResponse = null;
	private InetAddress lastAddrResponse = null;
	private int totalPingsSent = 0;
	
	private int pendingPings = 0;
	
	public NetworkService(Activity activity, String pingRequest, Callback onConnect, Callback onDisconnect, Callback onPending) {
		this.onConnect = onConnect;
		this.onDisconnect = onDisconnect;
		this.onPending = onPending;
		this.activity = activity;
		pingBuffer.reset();
		pingBuffer.copyByte(RemoteEvent.PING.getId());
		pingBuffer.copyData(pingRequest.getBytes());
	}
	
	private class PingRetryRunnable implements Runnable {
		public void run() {
			if (pendingPings == PENDING_TO_DISCONNECT_UNACK) {
				connectedServerAddr = null;
				currentPingInterval = MILLIS_BEFORE_PING_RETRY;
				onDisconnect.callback();
			} else if (pendingPings == CONNECT_TO_PENDING_UNACK) {
				onPending.callback();
				currentPingInterval = MILLIS_BEFORE_PING_RETRY;
			}
			try {
				InetAddress pingAddr;
				if (connectedServerAddr == null)
					pingAddr = BroadcastHandler.getInstance(activity).getBroadcastAddress();
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
			lastPingResponse = msg;
			lastAddrResponse = packet.getAddress();
			if (0 != msg.compareTo(PING_RESP_EXPECTED))
				return;
			pendingPings = 0;
			currentPingInterval = MILLIS_BEFORE_PING;
			if (connectedServerAddr == null)
				connectedServerAddr = packet.getAddress();
				
			onConnect.callback();
		}
	}
	
	private static class SendThread extends Thread {
		private static DatagramPacket sendPacket = new DatagramPacket(new byte[1024], 1024);
		private static DatagramSocket socket = null;
		static {
			try {
				socket = new DatagramSocket();
			} catch (IOException ex) {}
		}
		
		public SendThread(byte[] buf, int buf_len, InetAddress address, int port) {
			sendPacket.setData(buf);
			sendPacket.setLength(buf_len);
			sendPacket.setAddress(address);
			sendPacket.setPort(port);			
		}

		public void run() {
			try {
				socket.send(sendPacket);
			} catch(Exception ex) {}
		}
	}
	
	public String getLastPingResponse() {
		return lastPingResponse;
	}
	
	public String getExpectedPingResponse() {
		return PING_RESP_EXPECTED;
	}
	
	public String getLastAddrResponse() {
		if (lastAddrResponse == null)
			return null;
		return lastAddrResponse.getHostAddress();
	}
	
	public int getTotalPings() {
		return totalPingsSent;
	}
	
	public void cleanup() {
		if (listener != null)
			listener.kill();
		timeoutHandler = null;
	}
	
	public void send(DatagramBuffer sendBuffer) {
		new SendThread(sendBuffer.toArray(), sendBuffer.length(), connectedServerAddr, SERVER_PORT).start();
	}
	
	/* two possible addresses for ping request */
	private void sendPing(InetAddress addr) {
		++pendingPings;
		++totalPingsSent;
		if (timeoutHandler != null)
			timeoutHandler.postDelayed(pingRetry, currentPingInterval);
		//scheduledTimer.schedule(timeoutTask, currentPingInterval, TimeUnit.MILLISECONDS);
		new SendThread(pingBuffer.toArray(), pingBuffer.length(), addr, SERVER_PORT).start();
	}
	
	public static byte[] intToByteArray(int integer) {
		byte[] byteArr = new byte[4];
		for (int i = 0; i < 4; i++) {
			byteArr[i] = (byte)(integer >>> (8 * i) & 0xFF);
		}
		return byteArr;
	}
	
	public static int byteArrayToInt(byte[] bytes) {
		int result = 0;
		if (bytes.length != 4)
			return -1;
		for (int i = 0; i < 4; i++) {
			result |= ((int)bytes[3-i]) << i;
		}
		return result;
	}
	
	public static InetAddress getWifiAddress(Context c) throws UnknownHostException {
		WifiManager wifiManager = (WifiManager)c.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		Integer wifiIp = wifiInfo.getIpAddress();
		return InetAddress.getByAddress(NetworkService.intToByteArray(wifiIp));
	}
	
	public static byte[] getNetmask(Context c) {
		byte[] netmask = new byte[4];
		int m = ((WifiManager)c.getSystemService(Context.WIFI_SERVICE)).getDhcpInfo().netmask;
		for (int i = 0; i < 4; ++i) {
			netmask[i] = (byte)((m >>> i * 8) & 0xFF);
		}
		return netmask;
	}
	
	public static byte[] orOperation(byte[] b1, byte[] b2) {
		byte[] or = new byte[b1.length];
		if (b1.length != b2.length)
			return null;
		for (int i = 0; i < or.length; ++i) {
			or[i] = (byte)(b1[i] | b2[i]);
		}
		return or;
	}
	
	public static byte[] inverseOperation(byte[] b) {
		for (int i = 0; i < b.length; ++i) {
			b[i] = (byte)~b[i];
		}
		return b;
	}
	
	public InetAddress findLanServerAddr() {
		try {
			InetAddress broadcastAddr = BroadcastHandler.getInstance(activity).getBroadcastAddress();
			
			listener = new DatagramListener(activity, SERVER_PORT+1, new PingResponseHandler());
			listener.start();
			
			sendPing(broadcastAddr);
			
			
			
		} catch (Exception ex) {
			onDisconnect.callback();
		}
		return null;
	}
}
