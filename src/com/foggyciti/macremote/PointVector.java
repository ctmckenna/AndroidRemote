package com.foggyciti.macremote;

public class PointVector {

	private float x;
	private float y;
	private boolean normalized = false;
	
	public PointVector(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public void setXY(float x, float y) {
		this.x = x;
		this.y = y;
		normalized = false;
	}
	
	public void copy(PointVector pv) {
		this.x = pv.getX();
		this.y = pv.getY();
		this.normalized = pv.isNormalized();
	}
	
	public float getX() {
		return x;
	}
	
	public float getY() {
		return y;
	}
	
	public void normalize() {
		double dist = MathUtil.distance(x, 0, y, 0);
		if (x != 0)
			x = x / (float)dist;
		if (y != 0)
			y = y / (float)dist;
		normalized = true;
	}
	
	public boolean isNormalized() {
		return normalized;
	}
	
	public boolean empty() {
		if (x == 0 && y == 0)
			return true;
		return false;
	}
	
	private static PointVector avgVector = new PointVector(0, 0);
	public final PointVector average(PointVector v) {
		avgVector.setXY((this.getX() + v.getX()) / 2, (this.getY() + v.getY()) / 2);
		return avgVector;
	}
}
