package com.foggyciti.macremote;

import java.util.Calendar;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Tracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.widget.LinearLayout;

public class RemoteControlActivity extends Activity {
	static int serverPort = 4023;
	static final String pingResponse = ":-)";
	private NetworkService networkService = null;
	private Delta deltaBuffer = new Delta();
	DatagramBuffer sendBuffer = new DatagramBuffer();
	private Connection connectionStatus = Connection.PENDING;
	
	/* data for batched movement */
	private long lastSendTime = Calendar.getInstance().getTimeInMillis();
	private static final long COLLECTION_TIME = 25; // in millis
		
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        EasyTracker.getInstance().setContext(this);
        
        setContentView(R.layout.main);
        LinearLayout ll = (LinearLayout)findViewById(R.id.layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        RCView v = new RCView(this);
        v.setLayoutParams(params);
        ll.addView(v);
        
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			sendEvent(RemoteEvent.VOLUME, 6);
			return true;
		}
    	if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
			sendEvent(RemoteEvent.VOLUME, -6);
			return true;
		}
    	return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	networkService = new NetworkService(this, PersistenceHandler.getPasscode(this), new Connect(), new Disconnect(), new Pending());
    	networkService.findLanServerAddr();
    	EasyTracker.getInstance().activityStart(this);
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	networkService.cleanup();
        networkService = null;
        connectionStatus = Connection.PENDING;
        EasyTracker.getInstance().activityStop(this);
    }
    
    private class Connect implements Callback {
    	public void callback() {
    		connectionStatus = Connection.CONNECTED;
    		Analytics.sendConnected();
    	}
    }
    private class Disconnect implements Callback {
    	public void callback() {
    		connectionStatus = Connection.DISCONNECTED;
    		Analytics.sendDisconnected();
    	}
    }
    private class Pending implements Callback {
    	public void callback() {
    		connectionStatus = Connection.PENDING;
    	}
    }
	
    public Connection getConnectionStatus() {
    	return connectionStatus;
    }
    
	public void sendEvent(RemoteEvent ev) {
		if (connectionStatus != Connection.CONNECTED)
			return;
		sendBuffer.reset();
		sendBuffer.copyByte(ev.getId());
		networkService.send(sendBuffer);
	}
	
	public void sendEvent(RemoteEvent ev, int i) {
		if (connectionStatus != Connection.CONNECTED)
			return;
		long now = Calendar.getInstance().getTimeInMillis();
		sendBuffer.reset();
		sendBuffer.copyByte(ev.getId());
		sendBuffer.copyInt(i);
		sendBuffer.copyInt((int)now);
		networkService.send(sendBuffer);
		Analytics.remoteControlEvent(ev);
	}
	
	public void sendEvent(float x, float y, RemoteEvent ev) {
		if (connectionStatus != Connection.CONNECTED)
			return;
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
		sendBuffer.copyFloat(PixelUtil.dp(this, x));
		sendBuffer.copyFloat(PixelUtil.dp(this, y));
		sendBuffer.copyInt((int)now);
		networkService.send(sendBuffer);
		Analytics.remoteControlEvent(ev);
	}
}