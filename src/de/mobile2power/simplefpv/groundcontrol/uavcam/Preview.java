package de.mobile2power.simplefpv.groundcontrol.uavcam;

import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

public class Preview implements Parcelable {

	private int width = 0;
	private int height = 0;
	private int arrayLength = 0;
	
	private byte[] jpegImage = {};
	
	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public byte[] getJpegImage() {
		return jpegImage;
	}

	public void setJpegImage(byte[] jpegImage) {
		this.jpegImage = jpegImage;
		this.arrayLength = jpegImage.length;
	}


	@Override
	public String toString() {
		return "Preview [width=" + width + ", height=" + height
				+ ", jpegImage=" + Arrays.toString(jpegImage) + "]";
	}

	public Preview() {
		
	}
	
	public Preview(Parcel in) {
		width = in.readInt();
		height = in.readInt();
		arrayLength = in.readInt();
		
		jpegImage = new byte[arrayLength];
		in.readByteArray(jpegImage);
		
	}
	
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(width);
		dest.writeInt(height);
		dest.writeInt(arrayLength);
		dest.writeByteArray(jpegImage);
	}
	
	static final Parcelable.Creator<Preview> CREATOR = new Parcelable.Creator<Preview>() {

		public Preview createFromParcel(Parcel in) {
			return new Preview(in);
		}

		public Preview[] newArray(int size) {
			return new Preview[size];
		}
	};
}
