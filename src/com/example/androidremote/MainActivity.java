package com.example.androidremote;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

public class MainActivity extends Activity {
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Display display = getWindowManager().getDefaultDisplay();
		System.out.println("width: " + display.getWidth() + " height: " + display.getHeight());
		setContentView(R.layout.main_menu);
		
	}

}
