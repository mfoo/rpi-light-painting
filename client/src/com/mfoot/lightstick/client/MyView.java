package com.mfoot.lightstick.client;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.View;

public class MyView extends View {
	// Width and height of the dotted grid and pixels to render
	private static int GRID_SIZE;

	// Number of RGB LEDs in the array
	private static final int MAX_PIXELS_Y = 32;

	private Bitmap mBitmap;
	private Canvas mCanvas;
	private final Rect mRect = new Rect();
	private final Paint mPaint;
	private final Paint mLinePaint;
	private final Paint mBorderPaint;
	private float mOffsetX;
	private float mOffsetY;
	private int mMaxX;
	private int mMaxY;
	private boolean mDragging = false;

	public MyView(Context c, AttributeSet attrs) {
		super(c, attrs);
		setFocusable(true);

		mOffsetX = 0;
		mOffsetY = 0;

		mBorderPaint = new Paint();
		mBorderPaint.setAntiAlias(true);
		mBorderPaint.setARGB(255, 255, 0, 0);
		mBorderPaint.setStrokeWidth(2);
		mBorderPaint.setStyle(Style.STROKE);

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setARGB(255, 255, 255, 255);

		mLinePaint = new Paint();
		mLinePaint.setARGB(100, 100, 100, 100);
		mLinePaint.setStyle(Style.STROKE);
		mLinePaint.setPathEffect(new DashPathEffect(new float[] { 4, 16 }, 0));
	}

	public void toggleDragging() {
		mDragging = !mDragging;
	}

	public Bitmap getBitmap() {
		return mBitmap;
	}

	public boolean getDragging() {
		return mDragging;
	}

	public int getBitmapHeight() {
		return mMaxY;
	}

	public int getBitmapWidth() {
		return mMaxX;
	}

	public void clear() {
		if (mCanvas != null) {
			mPaint.setARGB(0xff, 0, 0, 0);
			mCanvas.drawPaint(mPaint);
			invalidate();
		}
	}

	public void setColour(int colour) {
		mPaint.setColor(colour);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		int curW = mBitmap != null ? mBitmap.getWidth() : 0;
		int curH = mBitmap != null ? mBitmap.getHeight() : 0;
		if (curW >= w && curH >= h) {
			return;
		}

		if (curW < w)
			curW = w;
		if (curH < h)
			curH = h;

		// Fit the entire grid on the screen vertically
		GRID_SIZE = h / MAX_PIXELS_Y;
		
		mMaxY = GRID_SIZE * MAX_PIXELS_Y;
		mMaxX = (int) (Math.floor(w / GRID_SIZE) * GRID_SIZE);

		Bitmap newBitmap = Bitmap.createBitmap(curW, curH,
				Bitmap.Config.RGB_565);

		Canvas newCanvas = new Canvas();
		newCanvas.setBitmap(newBitmap);
		newCanvas.drawColor(Color.BLACK);

		if (mBitmap != null) {
			newCanvas.drawBitmap(mBitmap, 0, 0, null);
		}
		mBitmap = newBitmap;
		mCanvas = newCanvas;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mBitmap != null) {
			canvas.translate(FloatMath.floor(mOffsetX), FloatMath.floor(mOffsetY));
			canvas.drawBitmap(mBitmap, 0, 0, null);
			int numColumns = mCanvas.getWidth(); // TODO (and fix in below)
			
			// Render the boundary
			canvas.drawRect(0, 0, numColumns, GRID_SIZE * MAX_PIXELS_Y, mBorderPaint);
			
			canvas.translate(-FloatMath.floor(mOffsetX), -FloatMath.floor(mOffsetY));
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {		
		int action = event.getActionMasked();
		
		if(action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE) {
			if(!mDragging) {
				// Single pointer, draw a rectangle
				if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE) {
					float currentX = event.getX() - mOffsetX;
					float currentY = event.getY() - mOffsetY;

					if (currentX >= mMaxX || currentY >= mMaxY) {
						return true;
					}

					drawPoint(currentX, currentY);

					int numColumns = (int) Math.floor(mMaxX / GRID_SIZE);
					int numRows = MAX_PIXELS_Y;
					// Vertical lines
					for (int columnNo = 0; columnNo < numColumns; columnNo++) {
						mCanvas.drawLine(columnNo * GRID_SIZE, 0, columnNo
								* GRID_SIZE, mMaxY, mLinePaint);
					}

					// Horizontal lines
					for (int rowNo = 0; rowNo < numRows; rowNo++) {
						mCanvas.drawLine(0, rowNo * GRID_SIZE, mMaxX, rowNo
								* GRID_SIZE, mLinePaint);
					}

					// TODO: Expensive!
					invalidate();
				}
			} else {
				float previousOffsetX = event.getHistoricalX(0);
				float previousOffsetY = event.getHistoricalY(0);
		
				// Ignore multi-touch
				mOffsetX += event.getX() - previousOffsetX;
				mOffsetY += event.getY() - previousOffsetY;
				
				invalidate();
			}
		}
		
		return true;
	}

	private void drawPoint(float x, float y) {
		if (mBitmap != null) {
			int column = (int) FloatMath.floor(x / (float) GRID_SIZE);
			int row = (int) FloatMath.floor(y / (float) GRID_SIZE);

			mRect.set(column * GRID_SIZE, row * GRID_SIZE, (column + 1)
					* GRID_SIZE, (row + 1) * GRID_SIZE);

			mCanvas.drawRect(mRect, mPaint);
		}
	}
}