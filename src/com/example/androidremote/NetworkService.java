package com.example.androidremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Enumeration;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetworkService {
	private InetAddress connectedServerAddr = null;
	private int serverPort = 0;
	private DatagramStateMachine datagramStateMachine = null;
	private Callback connectionSuccess;
	private Callback connectionFailure;
	private String pingResponse;
	private Activity activity;
	DatagramSocket socket = null;
	DatagramBuffer pingBuffer = new DatagramBuffer();

	public static class DatagramStateMachine {
		private long ackPingTimestamp = 0;
		private int pendingPings = 0;
		private int ackPingNo = 0;
		private int requestNo = 0;
		private State curState = State.Unconnected;

		public static final int requestsBeforePing = 1000;
		public static final int millisBeforePing = 1000;

		public static final int requestsBeforePingRetry = 10;
		public static final int millisBeforePingRetry = 2;

		public static final int unacknowledgedPingsBeforeReset = 10;

		private enum State {
			Unconnected,
			Running,
			PingMode
		}

		public void sendingPing() throws ConnectionResetException {
			++pendingPings;
			if (pendingPings > unacknowledgedPingsBeforeReset) {
				curState = State.Unconnected;
				throw new ConnectionResetException();
			}
		}
		
		public void pingAcknowledged() {
			curState = State.Running;
			ackPingTimestamp = Calendar.getInstance().getTimeInMillis();
			ackPingNo = requestNo;
			pendingPings = 0;
		}
		
		/* returns whether or not a ping should also be sent */
		public boolean sendingRequest() {
			++requestNo;
			long curTimestamp = Calendar.getInstance().getTimeInMillis();
			switch (curState) {
			case Running:
				if (requestNo - ackPingNo >= requestsBeforePing || curTimestamp - ackPingTimestamp >= millisBeforePing) {
					pendingPings = 0;
					ackPingNo = requestNo;
					ackPingTimestamp = curTimestamp;
					curState = State.PingMode;
					return true;
				}
				return false;
			case PingMode:
				if (requestNo - ackPingNo >= requestsBeforePingRetry || curTimestamp - ackPingTimestamp >= millisBeforePingRetry) {
					ackPingNo = requestNo;
					ackPingTimestamp = curTimestamp;
					return true;
				}
				return false;
			}
			return false;
		}
		
		public State getState() {
			return curState;
		}
		
		public void setConnected() {
			curState = State.Running;
		}
	}
	
	private class PingResponseHandler implements DatagramCallback {
		@Override
		public void callback(DatagramPacket packet) {
			String msg = new String(packet.getData(), 0, packet.getLength());
			if (0 != msg.compareTo(pingResponse))
				return;
			switch(datagramStateMachine.getState()) {
			case Unconnected:
				connectedServerAddr = packet.getAddress();
				datagramStateMachine.setConnected();
				connectionSuccess.callback();
				break;
			case Running:
			case PingMode:
				datagramStateMachine.pingAcknowledged();
				break;
			}
		}
	}
	
	public NetworkService(Activity activity, int port, String pingRequest, String pingResponse, Callback connectionSuccess, Callback connectionFailure) {
		serverPort = port;
		datagramStateMachine = new DatagramStateMachine();
		this.connectionSuccess = connectionSuccess;
		this.connectionFailure = connectionFailure;
		this.pingResponse = pingResponse;
		this.activity = activity;
		pingBuffer.reset();
		pingBuffer.copyByte(RemoteEvent.PING.getId());
		pingBuffer.copyData(pingRequest.getBytes());
		try {
			socket = new DatagramSocket();
		} catch(Exception ex) {
			connectionFailure.callback();
		}
	}
	
	private void send(byte[] buf, int buf_len, InetAddress address, int port) {
		try {
			DatagramPacket sendPacket = new DatagramPacket(buf, buf_len, address, port);
			socket.send(sendPacket);
		} catch(Exception ex) {
			connectionFailure.callback();
		}
	}
	
	public void send(DatagramBuffer sendBuffer) {
		datagramStateMachine.sendingRequest();
		send(sendBuffer.toArray(), sendBuffer.length(), connectedServerAddr, serverPort);
	}
	
	private void sendPing(InetAddress addr) {
		try {
			datagramStateMachine.sendingPing();
			send(pingBuffer.toArray(), pingBuffer.length(), addr, serverPort);
		} catch (ConnectionResetException ex) { 
			connectionFailure.callback();
		}
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
			
			new DatagramListener(activity, serverPort, new PingResponseHandler()).start();
			
			sendPing(broadcastAddr);
			
		} catch (Exception ex) {
			connectionFailure.callback();
		}
		return null;
	}
}
