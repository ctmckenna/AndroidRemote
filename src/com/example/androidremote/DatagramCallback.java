package com.example.androidremote;

import java.net.DatagramPacket;

public interface DatagramCallback {
	public void callback(DatagramPacket pckt);
}
