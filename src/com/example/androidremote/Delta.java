package com.example.androidremote;

public class Delta {
	public Delta() {
		x = 0;
		y = 0;
	}
	public Delta(float x, float y) {
		this.x = x;
		this.y = y;
	}
	public void reset() {
		x = 0;
		y = 0;
	}
	public void buffer(float x, float y) {
		this.x += x;
		this.y += y;
	}
	public float x;
	public float y;
}
