package com.foggyciti.macremote;

import android.content.Context;

public class PixelUtil {
	
	public static float px(Context c, float dp) {
		return c.getResources().getDisplayMetrics().density * dp;
	}
	
	public static float dp(Context c, float px) {
		return px / c.getResources().getDisplayMetrics().density;
	}
}
