package com.foggyciti.macremote;

public class Point {
	Point(float x, float y, int id) {
		origX = x;
		curX = x;
		origY = y;
		curY = y;
		lastDrawX = Float.NaN;
		lastDrawY = Float.NaN;
		this.id = id;
	}
	float origX;
	float origY;
	float curX;
	float curY;
	float lastDrawX;
	float lastDrawY;
	int id;
}
