package com.foggyciti.macremote;

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;

public class RemoteControlActivity extends Activity {
	static int listenPort = 4023;
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.main);
        LinearLayout ll = (LinearLayout)findViewById(R.id.layout);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT);
        RCView v = new RCView(this);
        v.setLayoutParams(params);
        ll.addView(v);
        //trackpadView = v;
        
        if (networkService == null) {
          networkService = new NetworkService(this, PersistenceHandler.getPasscode(this), new Connect(), new Disconnect(), new Pending());
          networkService.findLanServerAddr();
        }
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	networkService.cleanup();
    }
    
    private class Connect implements Callback {
    	public void callback() {
    		connectionStatus = Connection.CONNECTED;
    	}
    }
    private class Disconnect implements Callback {
    	public void callback() {
    		connectionStatus = Connection.DISCONNECTED;
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
	}
}