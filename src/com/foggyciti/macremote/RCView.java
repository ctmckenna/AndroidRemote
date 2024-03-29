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
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;

public class RCView extends View {
	private RemoteControlActivity activity;
	
	private static TouchState state = TouchState.empty;
	
	private long delayForClick = 250;
	private static long delayBeforeDrag = 900;        // milliseconds before hold turns to drag
	
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
	
	private void adjustDownPoints(MotionEvent event, Vector<Point> points) {
		for (int e_i = 0; e_i < event.getPointerCount(); ++e_i) {
			for (int p_i = 0; p_i < points.size(); ++p_i) {
				Point p = points.get(p_i);
				if (p.id == event.getPointerId(e_i)) {
					p.lastX = p.curX;
					p.lastY = p.curY;
					p.curX = event.getX(e_i);
					p.curY = event.getY(e_i);
				}
				
			}
		}
	}

	/* finds and returns the id of the point added to event */
	Point getNewPoint(Vector<Point> points, MotionEvent event) {
		boolean found;
		for (int e_i = 0; e_i < event.getPointerCount(); ++e_i) {
			found = false;
			for (int p_i = 0; p_i < points.size(); ++p_i) {
				if (points.get(p_i).id == event.getPointerId(e_i)) {
					found = true;
				}
			}
			if (!found) {
				int id = event.getPointerId(e_i);
				int idx = event.findPointerIndex(id);
				return new Point(event.getX(idx), event.getY(idx), id);
			}
		}
		return null;
	}
	
	private long downTime;
	private long lastTouchEvent;
	
	/* some android api versions don't continue to send MotionEvents when no pointers move,
	 * so there's an issue where (given two pointers) you lift one up and move it elsewhere, android
	 * will only send events while it's down and we miss that the pointer is off the screen
	 */
	private void correctDownPoints(MotionEvent event, Vector<Point> downPoints) {
		long acceptedMillisBetweenEvents = 200;
		double acceptedPixelsBetweenEvents = PixelUtil.px(activity, 20f);
		long currentTime = Calendar.getInstance().getTimeInMillis();
		//if we haven't received an event in some time, and one of the pointers moved beyond acceptedPixelsBetweenEvents, 
		//we assume it's been lifted and placed back down so we remove that downPoint to pretend like we noticed it was lifted
		if (state == TouchState.dragging_2 && downPoints.size() == event.getPointerCount() && currentTime - lastTouchEvent > acceptedMillisBetweenEvents) {
			for (int j = 0; j < downPoints.size(); ++j) {
				Point point = downPoints.get(j);
				for (int i = 0; i < event.getPointerCount(); ++i) {
					if (point.id == event.getPointerId(i)) {
						tempDelta.x = point.curX - event.getX(i);
						tempDelta.y = point.curY - event.getY(i);
						if (MathUtil.distance(tempDelta) > acceptedPixelsBetweenEvents) {
							downPoints.remove(j);
							correctState(downPoints.size());
						}
					}
				}
			}
		}
		lastTouchEvent = currentTime;
	}
	
	private void correctState(int numPointers) {
		switch(state) {
		case dragging_2:
			if (numPointers == 1)
				state = TouchState.dragging_1;
			break;
		case scrolling:
			if (numPointers == 1)
				state = TouchState.moving;
		default:
			break;
		}
	}
	
	private int actionMasked(int action) {
		return action & MotionEvent.ACTION_MASK;
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		Point upPt;
		correctDownPoints(event, downPoints);
		adjustDownPoints(event, downPoints);
		switch(actionMasked(event.getAction())) {
		case MotionEvent.ACTION_DOWN:
			downTime = Calendar.getInstance().getTimeInMillis();
			/* just starts a new gesture - all state switches needs to be handled in ACTION_MOVE */
			state = TouchState.holding;
			downPoints.add(getNewPoint(downPoints, event));
			return true;
		case MotionEvent.ACTION_UP:
			//System.out.println("up event: " + event.getPointerCount() + " pts");
			//System.out.println((Calendar.getInstance().getTimeInMillis() - downMillis) + " millis");
			switch(state) {
			case holding:
				state = TouchState.empty;
				TouchState.holder = Integer.MAX_VALUE;
				upPt = downPoints.get(0);
				long elapse = event.getEventTime() - event.getDownTime();
				double dist = Math.sqrt(MathUtil.square(event.getY() - upPt.origY) + MathUtil.square(event.getX() - upPt.origX));
				//System.out.println("click dist: " + dist + "lim: " + clickRadius);
				if (dist > clickRadius || elapse > delayForClick)
					break;
				activity.sendEvent(RemoteEvent.CLICK);
				break;
			case dragging_1:
				state = TouchState.empty;
				TouchState.holder = Integer.MAX_VALUE;
				activity.sendEvent(RemoteEvent.UP);
				break;
			case scrolling:
				state = TouchState.empty;
				TouchState.holder = Integer.MAX_VALUE;
				break;
			case moving:
				state = TouchState.empty;
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
			activity.sendEvent(pt.curX - pt.lastX, pt.curY - pt.lastY, RemoteEvent.DRAG);
			break;
		case 2:
			state = TouchState.dragging_2;
			downPoints.add(getNewPoint(downPoints, event));
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
			Point.maxDelta(downPoints, delta);
			activity.sendEvent(delta.x, delta.y, RemoteEvent.DRAG);
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
			double angle = MathUtil.angle(downPoints.get(0).getUnitVector(), downPoints.get(1).getUnitVector());
			
			if (angle >= 90)
				break;
			PointVector avg = downPoints.get(0).getVector().average(downPoints.get(1).getVector());
			angle = MathUtil.angleFromHorizon(avg);
			if (angle < 45)
				activity.sendEvent(avg.getX(), 0, RemoteEvent.SCROLL);
			else
				activity.sendEvent(0, avg.getY(), RemoteEvent.SCROLL);
			break;
		}
	}
	
	private void handle_moving(MotionEvent event) {
		Point pt;
		switch(event.getPointerCount()) {
		case 1:
			pt = downPoints.get(0);
			activity.sendEvent(pt.curX - pt.lastX, pt.curY - pt.lastY, RemoteEvent.MOVE);
			break;
		case 2:
			state = TouchState.scrolling;
			downPoints.add(getNewPoint(downPoints, event));
			break;
		}
	}
	
	private void set_state_dragging_1() {
		Vibrator v = (Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(100);
		state = TouchState.dragging_1;
		activity.sendEvent(delta.x, delta.y, RemoteEvent.DRAG);
	}
	
	private void handle_holding(MotionEvent event) {
		Point pt;
		switch(event.getPointerCount()) {
		case 1:
			pt = downPoints.get(0);
			long elapse = event.getEventTime() - event.getDownTime();
			double dist = Math.sqrt(MathUtil.square(pt.curY - pt.origY) + MathUtil.square(pt.curX - pt.origX));
			//System.out.println("elapse: " + elapse + " drag dist: " + dist);
			if (dist > holdingRadius) {
				state = TouchState.moving;
				TouchState.holder = Integer.MAX_VALUE;
				activity.sendEvent(pt.curX - pt.lastX, pt.curY - pt.lastY, RemoteEvent.MOVE);
			} else if (elapse > delayBeforeDrag) {
				set_state_dragging_1();
			} else {
				activity.sendEvent(delta.x, delta.y, RemoteEvent.MOVE);
			}
			break;
		case 2:
			state = TouchState.scrolling;
			TouchState.holder = Integer.MAX_VALUE;
			downPoints.add(getNewPoint(downPoints, event));
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
		if (state == TouchState.holding && Calendar.getInstance().getTimeInMillis() - downTime > delayBeforeDrag)
			set_state_dragging_1();
		
		initializeBitmap(activity.getConnectionStatus(), canvas.getWidth(), canvas.getHeight());

		canvas.drawBitmap(mainBitmap, 0, 0, paint);
		
		//canvas.drawARGB(255, 16, 16, 255);//80
		refreshHandler.sleep();
	}
}
