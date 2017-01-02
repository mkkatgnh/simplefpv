package de.mobile2power.simplefpv.rc;

/*
 Copyright 2012 Martin Keydel

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class ControlStick {

	private int stickMiddleX, stickMiddleY, stickRange;
	private float stickX, stickY;

	public void set(int x, int y, int size) {
		this.stickMiddleX = x;
		this.stickMiddleY = y;
		this.stickX = x;
		this.stickY = y;
		this.stickRange = size;
	}

	public boolean isWithin(float x, float y, float border) {
		return Math.abs(stickMiddleX - x) < (stickRange + border)
				&& Math.abs(stickMiddleY - y) < (stickRange + border);
	}

	public int getNormalizedX(float x) {
		int scaled = scaleDistance(stickMiddleX, x);
		if (isWithin(x, stickY, -5)) {
			stickX = x;
		}
		return trimStickValues(192 + scaled);
	}

	public int getNormalizedY(float y) {
		int scaled = scaleDistance(stickMiddleY, y);
		if (isWithin(stickX, y, -5)) {
			stickY = y;
		}
		return trimStickValues(192 - scaled);
	}

	private int scaleDistance(int origin, float position) {
		int diff = (int) position - origin;
		int scaled = (diff * 64) / stickRange;
		return scaled;
	}

	private int trimStickValues(int value) {
		int normalizedValue = value;
		normalizedValue = normalizedValue > 255 ? 255 : normalizedValue;
		normalizedValue = normalizedValue < 127 ? 127 : normalizedValue;
		return normalizedValue;
	}

	public void draw(Canvas canvas, Paint paint) {
		clearPad(canvas, paint);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(5);
		paint.setColor(Color.GRAY);
		canvas.drawRect(stickMiddleX - stickRange, stickMiddleY - stickRange,
				stickMiddleX + stickRange, stickMiddleY + stickRange, paint);
		paint.setStyle(Paint.Style.FILL);
		paint.setStrokeWidth(0);
		// canvas.drawCircle(orgStickX, orgStickY, 10, paint);
		// draw center/neutral point
		paint.setColor(Color.DKGRAY);
		canvas.drawCircle(stickMiddleX, stickMiddleY, 10, paint);
		// draw current stick position
		paint.setColor(Color.GRAY);
		canvas.drawCircle(stickX, stickY, 10, paint);
	}

	private void clearPad(Canvas canvas, Paint paint) {
		paint.setStyle(Paint.Style.FILL);
		paint.setStrokeWidth(0);
		paint.setColor(Color.BLACK);
		canvas.drawRect(stickMiddleX - stickRange, stickMiddleY - stickRange,
				stickMiddleX + stickRange, stickMiddleY + stickRange, paint);
	}
}
