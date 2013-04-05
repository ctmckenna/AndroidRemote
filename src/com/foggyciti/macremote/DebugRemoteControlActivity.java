package com.foggyciti.macremote;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DebugRemoteControlActivity extends Activity {

	private Handler timeoutHandler = new Handler();
	private Runnable timeoutRun;
	private List<KeyValue> elements = null;
	private NetworkService networkService;
	private Connection connectionStatus = Connection.PENDING;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_remote);
		
		elements = Arrays.asList(new KeyValue(R.string.pings_sent, R.string.NA),
								 new KeyValue(R.string.broadcast_address, R.string.NA),
								 new KeyValue(R.string.netmask, R.string.NA),
								 new KeyValue(R.string.received_ping, R.string.NA),
								 new KeyValue(R.string.expected_ping, R.string.NA),
								 new KeyValue(R.string.response_ip, R.string.NA),
								 new KeyValue(R.string.connection_status, R.string.pendingConnection, Color.YELLOW));
		
		buildLayout();
        networkService = new NetworkService(this, PersistenceHandler.getPasscode(this), new Connect(), new Disconnect(), new Pending());
		networkService.findLanServerAddr();
		
		timeoutRun = new Runnable() {
			@Override
			public void run() {
				updateUI();
			}
		};
		timeoutHandler.postDelayed(timeoutRun, 250);
	}
	
	private	class Connect implements Callback {
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
	
	private class KeyValue {
		public String key;
		public String value;
		public int keyId;
		public int color;
		
		public KeyValue(int keyId, int valueId) {
			key = DebugRemoteControlActivity.this.getResources().getString(keyId);
			this.keyId = keyId;
			value = DebugRemoteControlActivity.this.getResources().getString(valueId);
			color = Color.WHITE;
		}
		public KeyValue(int keyId, int valueId, int color) {
			key = DebugRemoteControlActivity.this.getResources().getString(keyId);
			this.keyId = keyId;
			value = DebugRemoteControlActivity.this.getResources().getString(valueId);
			this.color = color;
		}
	}
	
	private void buildLayout() {
		
		LinearLayout ll = (LinearLayout)findViewById(R.id.debug_remote_ll);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		LinearLayout.LayoutParams weightParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		weightParams.weight = 1f;
		params.gravity = Gravity.CENTER_HORIZONTAL;
		LinearLayout topWedge = new LinearLayout(this);
		topWedge.setLayoutParams(weightParams);
		ll.addView(topWedge);
		for (KeyValue kv : elements) {
			LinearLayout wrapper = new LinearLayout(this);
			wrapper.setLayoutParams(params);
			
			TextView keyTV = new TextView(this);
			keyTV.setLayoutParams(params);
			keyTV.setText(kv.key);
			
			TextView valTV = new TextView(this);
			valTV.setLayoutParams(params);
			valTV.setText(kv.value);
			valTV.setTextColor(kv.color);
			valTV.setId(kv.hashCode());
			
			wrapper.addView(keyTV);
			wrapper.addView(valTV);
			ll.addView(wrapper);
		}
		LinearLayout bottomWedge = new LinearLayout(this);
		bottomWedge.setLayoutParams(weightParams);
		bottomWedge.setWeightSum(1f);
		ll.addView(bottomWedge);
	}	
	
	private void updateUI() {
		try {
		for (KeyValue kv : elements) {
			TextView tv = (TextView)findViewById(kv.hashCode());
			
			if (kv.keyId == R.string.broadcast_address) {
				tv.setText(BroadcastHandler.getInstance(this).getBroadcastAddress().getHostAddress());
			} else if (kv.keyId == R.string.netmask) {
				tv.setText(InetAddress.getByAddress(NetworkService.getNetmask(this)).getHostAddress());
			} else if (kv.keyId == R.string.received_ping) {
				String resp = networkService.getLastPingResponse();
				if (resp != null)
					tv.setText(resp);
			} else if (kv.keyId == R.string.expected_ping) {
				tv.setText(networkService.getExpectedPingResponse());
			} else if (kv.keyId == R.string.pings_sent) {
				tv.setText(Integer.valueOf(networkService.getTotalPings()).toString());
			} else if (kv.keyId == R.string.response_ip) {
				String ip = networkService.getLastAddrResponse();
				if (ip != null)
					tv.setText(ip);
			} else if (kv.keyId == R.string.connection_status) {
				int stringId = -1;
				int color = Color.YELLOW;
				switch(connectionStatus) {
				case CONNECTED:
					stringId = R.string.connected;
					color = Color.GREEN;
					break;
				case DISCONNECTED:
					stringId = R.string.disconnected;
					color = Color.RED;
					break;
				case PENDING:
					stringId = R.string.pendingConnection;
					color = Color.YELLOW;
					break;
				}
				tv.setText(getResources().getString(stringId));
				tv.setTextColor(color);
			}
		}
		} catch (Exception ex) {}
		timeoutHandler.postDelayed(timeoutRun, 250);
	}
}
