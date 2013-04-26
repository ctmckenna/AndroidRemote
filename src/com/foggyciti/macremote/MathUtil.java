package com.foggyciti.macremote;

public class MathUtil {
	
	/* Math.pow is slow due to having power of type double - this impl is faster */
	public static double square(float n) {
		return n * n;
	}
	
	public static double distance(Delta delta) {
		return Math.sqrt(square(delta.x) + square(delta.y));
	}
	
	public static double distance(PointVector vector) {
		return Math.sqrt(square(vector.getX()) + square(vector.getY()));
	}
	
	public static double distance(float x1, float x2, float y1, float y2) {
		return Math.sqrt(square(x1 - x2) + square(y1 - y2));
	}
	
	public static double dot(PointVector v1, PointVector v2) {
		return v1.getX() * v2.getX() + v1.getY() * v2.getY();
	}
	
	private static PointVector baseVector = new PointVector(0, 0);
	public static double angleFromHorizon(PointVector v) {
		if (v.getX() >= 0)
			baseVector.setXY(1, 0);
		else
			baseVector.setXY(-1, 0);
		return angle(v, baseVector);
	}
	
	
	private static PointVector v1 = new PointVector(0, 0);
	private static PointVector v2 = new PointVector(0, 0);
	public static double angle(final PointVector finalV1, final PointVector finalV2) {
		v1.copy(finalV1);
		v2.copy(finalV2);
		if (!v1.isNormalized())
			v1.normalize();
		if (!v2.isNormalized())
			v2.normalize();
		if (v1.empty() || v2.empty())
			return 180;
		double costheta = dot(v1, v2);
		double theta = Math.toDegrees(Math.acos(costheta));
		return theta;
	}
	
}
