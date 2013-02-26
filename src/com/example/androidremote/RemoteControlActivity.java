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
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;

public class RemoteControlActivity extends Activity {
	private RCView trackpadView;
	static int listenPort = 4023;
	static int serverPort = 4023;
	static final String pingResponse = ":-)";
	static final String pingRequest = ":-/";
	private NetworkService networkService;
	private Delta deltaBuffer = new Delta();
	DatagramBuffer sendBuffer = new DatagramBuffer();
	
	/* data for batched movement */
	private long lastSendTime = Calendar.getInstance().getTimeInMillis();
	private static final long COLLECTION_TIME = 25; // in millis
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        LinearLayout ll = (LinearLayout)findViewById(R.id.layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        RCView v = new RCView(this);
        v.setLayoutParams(params);
        ll.addView(v);
        trackpadView = v;
        
        networkService = new NetworkService(this, serverPort, pingRequest, pingResponse, new ConnectionSuccessful(), new ConnectionFailure());
        networkService.findLanServerAddr();
    }
    
    private class ConnectionSuccessful implements Callback {
    	public void callback() {
    		System.out.println("We found that there server duuuude");
    	}
    }
    private class ConnectionFailure implements Callback {
    	public void callback() {
    		System.out.println("crap shit fuck tits motherfucker");
    	}
    }
	
	public void sendEvent(RemoteEvent ev) {
		sendBuffer.reset();
		sendBuffer.copyByte(ev.getId());
		networkService.send(sendBuffer);
	}
	
	public void sendEvent(float x, float y, RemoteEvent ev) {
		long now = Calendar.getInstance().getTimeInMillis();
		switch(ev) {
		case MOVE:
		case DRAG:
			if (lastSendTime + COLLECTION_TIME > now) {
				deltaBuffer.buffer(x, y);
				return;
			}
		default:
			x += deltaBuffer.x;
			y += deltaBuffer.y;
			deltaBuffer.reset();
			break;
		}
		lastSendTime = now;
		if (x == 0 && y == 0) return;
		sendBuffer.reset();
		sendBuffer.copyByte(ev.getId());
		sendBuffer.copyFloat(x);
		sendBuffer.copyFloat(y);

		networkService.send(sendBuffer);
	}

    
	public void checkWifiState() {
		//TODO: implement
	}
}