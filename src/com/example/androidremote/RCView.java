package com.example.androidremote;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;

public class RCView extends View {
	enum State {
		holding,
		dragging_1,
		dragging_2,
		scrolling,
		moving,
		empty;
		static int holder;    /* when dragging or holding, id of pointer that initiated the event */
	}
	
	private class Point {
		Point(float x, float y, int id) {
			origX = x;
			curX = x;
			origY = y;
			curY = y;
			this.id = id;
		}
		float origX;
		float origY;
		float curX;
		float curY;
		int id;
	}
	
	private RemoteControlActivity activity;
	
	private static State state = State.empty;
	
	private long delayForClick = 250;
	private double clickRadius = 2f;
	
	private static long delayBeforeDrag = 1200; //milliseconds before hold turns to drag
	private static final float holdingRadius = 0.5f;
	

	
	private Delta delta = new Delta();
	private Delta tempDelta = new Delta();
	private Vector<Point> downPoints = new Vector<Point>();

	public RCView(RemoteControlActivity activity) {
		super(activity);
		this.activity = activity;
	}
	
	/* Math.pow is slow due to having power of type double - this impl is faster */
	private double square(double n) {
		return n * n;
	}

	/* finds the point that was removed from the gesture, and removes
	 * it from the vector of points.
	 */
	Point removePt(Vector<Point> points, MotionEvent event) {
		Point p;
		boolean hasPoint;
		for (int p_i = 0; p_i < points.size(); ++p_i) {
			hasPoint = false;
			p = points.get(p_i);
			for (int e_i = 0; e_i < event.getPointerCount(); ++e_i) {
				if (p.id == event.getPointerId(e_i)) {
					hasPoint = true;
				}
			}
			if (hasPoint == false)
				return points.remove(p_i);
		}
		return null;
	}	
	
	double getDistance(Delta delta) {
		return Math.sqrt(square(delta.x) + square(delta.y));
	}
	
	/* finds and returns the point that moved the most. it also updates the curX and curY,
	 * for the list of points
	 */
	Delta findMovingPointDelta(Vector<Point> points, MotionEvent event) {
		double maxDist = -1;
		Delta d = tempDelta;
		for (int e_i = 0; e_i < event.getPointerCount(); ++e_i) {
			for (int p_i = 0; p_i < points.size(); ++p_i) {
				if (points.get(p_i).id == event.getPointerId(e_i)) {
					getDelta(points.get(p_i), event.getX(e_i), event.getY(e_i), delta);
					double dist = getDistance(delta);
					if (dist > maxDist) {
						maxDist = dist;
						tempDelta.x = delta.x;
						tempDelta.y = delta.y;
					}
				}
			}
		}
		return d;
	}

	/* finds and returns the id of the point added to event */
	int getNewPointId(Vector<Point> points, MotionEvent event) {
		boolean found;
		for (int e_i = 0; e_i < event.getPointerCount(); ++e_i) {
			found = false;
			for (int p_i = 0; p_i < points.size(); ++p_i) {
				if (points.get(p_i).id == event.getPointerId(e_i)) {
					found = true;
				}
			}
			if (!found) {
				return event.getPointerId(e_i);
			}
		}
		return Integer.MAX_VALUE;
	}
	
	private void getDelta(Point pt, float x, float y, Delta d) {
		d.x = x - pt.curX;
		d.y = y - pt.curY;
		pt.curX = x;
		pt.curY = y;
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		Point downPt;
		Point upPt;
		switch(event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			/* just starts a new gesture - all state switches needs to be handled in ACTION_MOVE */
			state = State.holding;
			int pointId = getNewPointId(downPoints, event);
			downPt = new Point(event.getX(), event.getY(), pointId);
			downPoints.add(downPt);
			State.holder = pointId;
			return true;
		case MotionEvent.ACTION_UP:
			//System.out.println("up event: " + event.getPointerCount() + " pts");
			switch(state) {
			case holding:
				state = State.empty;
				State.holder = Integer.MAX_VALUE;
				upPt = downPoints.get(0);
				long elapse = event.getEventTime() - event.getDownTime();
				double dist = Math.sqrt(square(event.getY() - upPt.origY) + square(event.getX() - upPt.origX));
				if (dist > clickRadius || elapse > delayForClick)
					break;
				activity.sendEvent(RemoteEvent.CLICK);
				break;
			case dragging_1:
				state = State.empty;
				State.holder = Integer.MAX_VALUE;
				activity.sendEvent(RemoteEvent.UP);
				break;
			case moving:
				state = State.empty;
				break;
			}
			downPoints.clear();
			return true;
		case MotionEvent.ACTION_MOVE:
			//System.out.println("move event: " + event.getPointerCount() + " pts");
			switch(state) {
			case holding:
				handle_holding(event);
				break;
			case dragging_1:
				handle_dragging_1(event);
				break;
			case dragging_2:
				handle_dragging_2(event);
				break;
			case scrolling:
				handle_scrolling(event);
				break;
			case moving:
				handle_moving(event);
				break;
			}
			return true;
		case MotionEvent.ACTION_CANCEL:
			downPoints.clear();
			state = State.empty;
			State.holder = Integer.MAX_VALUE;
			return true;
		}
		return false;
	}
	/* ACTION_MOVE event handlers */
	private void handle_dragging_1(MotionEvent event) {
		Point pt;
		switch(event.getPointerCount()) {
		case 1:
			pt = downPoints.get(0);
			getDelta(pt, event.getX(), event.getY(), delta);
			activity.sendEvent(delta.x, delta.y, RemoteEvent.DRAG);
			break;
		case 2:
			state = State.dragging_2;
			int id = getNewPointId(downPoints, event);
			int idx = event.findPointerIndex(id);
			pt = new Point(event.getX(idx), event.getY(idx), id);
			downPoints.add(pt);
			break;
		}
	}

	/* not finished */
	private void handle_dragging_2(MotionEvent event) {
		Point pt;
		switch(event.getPointerCount()) {
		case 1:
			pt = removePt(downPoints, event);
			if (pt.id == State.holder) {
				state = State.moving;
				State.holder = Integer.MAX_VALUE;
				activity.sendEvent(RemoteEvent.UP);
			} else {
				state = State.dragging_1;
			}
			break;
		case 2:
			Delta d = findMovingPointDelta(downPoints, event);
			activity.sendEvent(d.x, d.y, RemoteEvent.DRAG);
			break;
		}
	}
	private void handle_moving(MotionEvent event) {
		Point pt;
		switch(event.getPointerCount()) {
		case 1:
			pt = downPoints.get(0);
			getDelta(pt, event.getX(), event.getY(), delta);
			activity.sendEvent(delta.x, delta.y, RemoteEvent.MOVE);
			break;
		case 2:
			state = State.scrolling;
			int id = getNewPointId(downPoints, event);
			int idx = event.findPointerIndex(id);
			pt = new Point(event.getX(idx), event.getY(idx), id);
			downPoints.add(pt);
			break;
		}
	}
	
	private void handle_holding(MotionEvent event) {
		Point pt;
		switch(event.getPointerCount()) {
		case 1:
			pt = downPoints.get(0);
			getDelta(pt, event.getX(), event.getY(), delta);
			long elapse = event.getEventTime() - event.getDownTime();
			double dist = Math.sqrt(square(pt.curY - pt.origY) + square(pt.curX - pt.origX));
			if (dist > holdingRadius) {
				state = State.moving;
				State.holder = Integer.MAX_VALUE;
				activity.sendEvent(delta.x, delta.y, RemoteEvent.MOVE);
			} else if (elapse > delayBeforeDrag) {
				Vibrator v = (Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(100);
				state = State.dragging_1;
				activity.sendEvent(delta.x, delta.y, RemoteEvent.DRAG);
			} else {
				activity.sendEvent(delta.x, delta.y, RemoteEvent.MOVE);
			}
			break;
		case 2:
			state = State.scrolling;
			State.holder = Integer.MAX_VALUE;
			int id = getNewPointId(downPoints, event);
			int idx = event.findPointerIndex(id);
			pt = new Point(event.getX(idx), event.getY(idx), id);
			downPoints.add(pt);
			break;
		}
	}
	/* to be implemented */
	private void handle_scrolling(MotionEvent event) {
		switch (event.getPointerCount()) {
		case 1:
			removePt(downPoints, event);
			state = State.moving;
			break;
		case 2:
			/* not yet implemented */
			break;
		}
	}
	
	
	public void onDraw(Canvas canvas) {
		canvas.drawARGB(0, 16, 16, 255);//80
	}
}
