package com.example.androidremote;

import android.os.Handler;
import android.os.Message;
import android.view.View;

public class RefreshHandler extends Handler {
	private long millisBetweenUpdate = 20;
	private View v;
	
	public RefreshHandler(View v) {
		this.v = v;
	}
	
	@Override
	public void handleMessage(Message msg) {
		v.invalidate();
	}
	
	public void sleep() {
		this.removeMessages(0);
		sendMessageDelayed(obtainMessage(0), millisBetweenUpdate);
	}
}