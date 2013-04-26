package com.foggyciti.macremote;

import java.util.Vector;

public class Point {
	private PointVector vector;
	
	private static Delta tempDelta;
	
	Point(float x, float y, int id) {
		origX = x;
		curX = x;
		origY = y;
		curY = y;
		lastX = Float.NaN;
		lastY = Float.NaN;
		lastDrawX = Float.NaN;
		lastDrawY = Float.NaN;
		this.id = id;
	}
	float origX;
	float origY;
	float lastX;
	float lastY;
	float curX;
	float curY;
	float lastDrawX;
	float lastDrawY;
	int id;
	
	public PointVector getUnitVector() {
		PointVector v = getVector();
		v.normalize();
		return v;
	}
	
	public PointVector getVector() {
		if (vector == null)
			vector = new PointVector(curX - lastX, curY - lastY);
		else
			vector.setXY(curX - lastX, curY - lastY);
		return vector;
	}
	
	public static void minDelta(Vector<Point> points, Delta d) {
		double minDist = Double.POSITIVE_INFINITY;
		d.reset();
		for (Point p : points) {
			PointVector v = p.getVector();
			if (MathUtil.distance(v) < minDist) {
				d.set(v);
			}
		}
	}
	
	public static void maxDelta(Vector<Point> points, Delta d) {
		double maxDist = -1;
		d.reset();
		for (Point p : points) {
			PointVector v = p.getVector();
			if (MathUtil.distance(v) > maxDist) {
				d.set(v);
			}
		}
	}
}
