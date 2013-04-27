package com.foggyciti.macremote;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class ButtonStateManager {
	private Activity activity = null;
	
	public ButtonStateManager(Activity activity) {
		this.activity = activity;
	}
	
	public void setImageButtonStates(int buttonId, final int onDownDrawable, final int onUpDrawable) {
		ImageButton button = (ImageButton)activity.findViewById(buttonId);

		button.setOnTouchListener(new OnTouchListener(onUpDrawable, onDownDrawable));
	}
	
	private class OnTouchListener implements View.OnTouchListener {
		private int onDown;
		private int onUp;

		public OnTouchListener(int onUp, int onDown) {
			this.onUp = onUp;
			this.onDown = onDown;
		}
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			ImageButton b = (ImageButton)v;
			switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				b.setImageDrawable(v.getContext().getResources().getDrawable(onDown));
				break;
			case MotionEvent.ACTION_UP:
				b.setImageDrawable(v.getContext().getResources().getDrawable(onUp));
				break;
			}
			return false;
		}
		
	}
}
