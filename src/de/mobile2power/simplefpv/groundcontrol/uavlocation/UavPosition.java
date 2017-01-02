package de.mobile2power.simplefpv.groundcontrol.uavlocation;

import java.io.Serializable;

public class UavPosition implements Serializable {
	private static final long serialVersionUID = 4768980831937502880L;
	private double longitude;
	private double latitude;
	private double altitude;
	private float direction;
	private float speed;
	private boolean accuracy;
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getAltitude() {
		return altitude;
	}
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	public float getDirection() {
		return direction;
	}
	public void setDirection(float direction) {
		this.direction = direction;
	}
	public float getSpeed() {
		return speed;
	}
	public void setSpeed(float speed) {
		this.speed = speed;
	}
	public boolean isAccuracy() {
		return accuracy;
	}
	public void setAccuracy(boolean accuracy) {
		this.accuracy = accuracy;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (accuracy ? 1231 : 1237);
		long temp;
		temp = Double.doubleToLongBits(altitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Float.floatToIntBits(direction);
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Float.floatToIntBits(speed);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UavPosition other = (UavPosition) obj;
		if (accuracy != other.accuracy)
			return false;
		if (Double.doubleToLongBits(altitude) != Double
				.doubleToLongBits(other.altitude))
			return false;
		if (Float.floatToIntBits(direction) != Float
				.floatToIntBits(other.direction))
			return false;
		if (Double.doubleToLongBits(latitude) != Double
				.doubleToLongBits(other.latitude))
			return false;
		if (Double.doubleToLongBits(longitude) != Double
				.doubleToLongBits(other.longitude))
			return false;
		if (Float.floatToIntBits(speed) != Float.floatToIntBits(other.speed))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "UavPosition [longitude=" + longitude + ", latitude=" + latitude
				+ ", altitude=" + altitude + ", direction=" + direction
				+ ", speed=" + speed + ", accuracy=" + accuracy + "]";
	}
}
