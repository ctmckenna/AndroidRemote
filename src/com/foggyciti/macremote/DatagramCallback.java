package com.foggyciti.macremote;

import java.net.DatagramPacket;

public interface DatagramCallback {
	public void callback(DatagramPacket pckt);
}
