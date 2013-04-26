package com.foggyciti.macremote;

public class Delta {
	public Delta() {
		x = 0;
		y = 0;
	}
	public Delta(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public Delta(float x1, float x2, float y1, float y2) {
		this.x = x2 - x1;
		this.y = y2 - y1;
	}
	
	public void reset() {
		x = 0;
		y = 0;
	}
	public void buffer(float x, float y) {
		this.x += x;
		this.y += y;
	}
	
	public void set(PointVector v) {
		x = v.getX();
		y = v.getY();
	}
	
	public float x;
	public float y;
}
