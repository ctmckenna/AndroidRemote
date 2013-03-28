package com.foggyciti.macremote;

import java.util.Calendar;
import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public class RCView extends View {
	private RemoteControlActivity activity;
	
	private static TouchState state = TouchState.empty;
	
	private long delayForClick = 250;
	private static long delayBeforeDrag = 1200;        // milliseconds before hold turns to drag
	
	/* distance measurements set in constructor since they're converted from dpi to pixels */
	private double clickRadius;
	private static float holdingRadius;
	
	private Delta delta = new Delta();
	private Delta tempDelta = new Delta();
	private Vector<Point> downPoints = new Vector<Point>();
	
	private Bitmap mainBitmap = null;
	Canvas mainCanvas = null;
	private Delta serverStatusOffset;
	private SparseArray<Bitmap> statusBitmaps = new SparseArray<Bitmap>();
	
	private Paint paint = new Paint();
	private Paint serverStatusPaint = new Paint();
	private Paint fillerPaint = new Paint();
	
	
	private Drawable pointGradient = null;
	private float pointWidth;
	private float pointHeight;
	
	
	private RefreshHandler refreshHandler = new RefreshHandler(this);

	public RCView(RemoteControlActivity activity) {
		super(activity);
		this.activity = activity;
		clickRadius = PixelUtil.px(activity, 4.5f);
		holdingRadius = PixelUtil.px(activity, 6f);
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
	
	private long downMillis;
	
	public boolean onTouchEvent(MotionEvent event) {
		Point downPt;
		Point upPt;
		switch(event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			downMillis = Calendar.getInstance().getTimeInMillis();
			//System.out.println("action down [" + state.toString() + "]");
			/* just starts a new gesture - all state switches needs to be handled in ACTION_MOVE */
			state = TouchState.holding;
			int pointId = getNewPointId(downPoints, event);
			downPt = new Point(event.getX(), event.getY(), pointId);
			downPoints.add(downPt);
			TouchState.holder = pointId;
			return true;
		case MotionEvent.ACTION_UP:
			//System.out.println("action up ["+ state.toString() + "]");
			//System.out.println("up event: " + event.getPointerCount() + " pts");
			//System.out.println((Calendar.getInstance().getTimeInMillis() - downMillis) + " millis");
			switch(state) {
			case holding:
				state = TouchState.empty;
				TouchState.holder = Integer.MAX_VALUE;
				upPt = downPoints.get(0);
				long elapse = event.getEventTime() - event.getDownTime();
				double dist = Math.sqrt(square(event.getY() - upPt.origY) + square(event.getX() - upPt.origX));
				System.out.println("click dist: " + dist + "lim: " + clickRadius);
				if (dist > clickRadius || elapse > delayForClick)
					break;
				activity.sendEvent(RemoteEvent.CLICK);
				break;
			case dragging_1:
				state = TouchState.empty;
				TouchState.holder = Integer.MAX_VALUE;
				activity.sendEvent(RemoteEvent.UP);
				break;
			case moving:
				state = TouchState.empty;
				break;
			}
			downPoints.clear();
			return true;
		case MotionEvent.ACTION_MOVE:
			//System.out.println("action move [" + state.toString() + "]");
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
			//System.out.println("action cancel [" + state.toString() + "]");
			downPoints.clear();
			state = TouchState.empty;
			TouchState.holder = Integer.MAX_VALUE;
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
			state = TouchState.dragging_2;
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
			if (pt.id == TouchState.holder) {
				state = TouchState.moving;
				TouchState.holder = Integer.MAX_VALUE;
				activity.sendEvent(RemoteEvent.UP);
			} else {
				state = TouchState.dragging_1;
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
			state = TouchState.scrolling;
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
			//System.out.println("drag dist: " + dist + "lim: " + holdingRadius);
			if (dist > holdingRadius) {
				state = TouchState.moving;
				TouchState.holder = Integer.MAX_VALUE;
				activity.sendEvent(delta.x, delta.y, RemoteEvent.MOVE);
			} else if (elapse > delayBeforeDrag) {
				Vibrator v = (Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(100);
				state = TouchState.dragging_1;
				activity.sendEvent(delta.x, delta.y, RemoteEvent.DRAG);
			} else {
				activity.sendEvent(delta.x, delta.y, RemoteEvent.MOVE);
			}
			break;
		case 2:
			state = TouchState.scrolling;
			TouchState.holder = Integer.MAX_VALUE;
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
			state = TouchState.moving;
			break;
		case 2:
			/* not yet implemented */
			break;
		}
	}
	
	private Bitmap getStatusBitmap(int statusDrawableId) {
		Bitmap bitmap = statusBitmaps.get(statusDrawableId);
		if (bitmap == null) {
			bitmap = BitmapFactory.decodeResource(activity.getResources(), statusDrawableId);
			statusBitmaps.put(statusDrawableId, bitmap);
		}
		return bitmap;
	}
	
	private void initializeBitmap(Connection connectionStatus, Integer width, Integer height) {
		if (mainBitmap == null) {
			mainBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			mainCanvas = new Canvas(mainBitmap);
			serverStatusOffset = new Delta((int)(PixelUtil.px(activity, 1) + 0.5), (int)(PixelUtil.px(activity, 1) + 0.5));
			
			fillerPaint.setARGB(255, 9, 0, 13);
			serverStatusPaint.setColor(Color.WHITE);
			serverStatusPaint.setStyle(Style.FILL);
			serverStatusPaint.setTextAlign(Align.LEFT);
			serverStatusPaint.setTextSize(20);
			
			pointGradient = activity.getResources().getDrawable(R.drawable.point_gradient);
			pointWidth = PixelUtil.px(activity, 70);
			pointHeight = PixelUtil.px(activity, 70);

		}
		mainCanvas.drawARGB(255, 9, 0, 13);

		
		Integer statusStringId = null;
		Bitmap statusBitmap = null;
		
		switch(connectionStatus) {
		case CONNECTED:
			statusStringId = R.string.connected;
			statusBitmap = getStatusBitmap(R.drawable.connectedd);
			break;
		case DISCONNECTED:
			statusStringId = R.string.disconnected;
			statusBitmap = getStatusBitmap(R.drawable.disconnected);
			break;
		case PENDING:
			statusStringId = R.string.pendingConnection;
			statusBitmap = getStatusBitmap(R.drawable.pending);
			break;
		}
		
		mainCanvas.drawBitmap(statusBitmap, serverStatusOffset.x, serverStatusOffset.y, paint);

		float textLeft = serverStatusOffset.x+statusBitmap.getWidth();
		float textHeight = PixelUtil.px(activity, 50);
		float textWidth = PixelUtil.px(activity, 100);
		mainCanvas.drawRect(textLeft, 0, textLeft+textWidth, textHeight, fillerPaint);
		mainCanvas.drawText(activity.getResources().getString(statusStringId), serverStatusOffset.x + statusBitmap.getWidth(), serverStatusOffset.y + statusBitmap.getHeight()/2, serverStatusPaint);
	
		for (int i = 0; i < downPoints.size(); ++i) {
			Point p = downPoints.get(i);
			pointGradient.setBounds((int)(p.curX - pointWidth/2), (int)(p.curY - pointHeight/2), (int)(p.curX + pointWidth/2), (int)(p.curY + pointHeight/2));
			pointGradient.draw(mainCanvas);
		}
	}
	
	public void onDraw(Canvas canvas) {		
		//if (connectionStatus != this.connectionStatus || mainBitmap == null) {
		initializeBitmap(activity.getConnectionStatus(), canvas.getWidth(), canvas.getHeight());

		canvas.drawBitmap(mainBitmap, 0, 0, paint);
		//canvas.drawARGB(255, 16, 16, 255);//80
		refreshHandler.sleep();
	}
}