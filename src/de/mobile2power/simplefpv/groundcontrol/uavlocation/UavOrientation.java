package de.mobile2power.simplefpv.groundcontrol.uavlocation;

import java.io.Serializable;
import java.util.Arrays;

public class UavOrientation implements Serializable{

	private static final long serialVersionUID = -2442331478340916251L;

	private int roll = 0;
	private int pitch = 0;
	private int yaw = 0;
	
	public int getRoll() {
		return roll;
	}
	public void setRoll(int roll) {
		this.roll = roll;
	}
	public int getPitch() {
		return pitch;
	}
	public void setPitch(int pitch) {
		this.pitch = pitch;
	}
	public int getYaw() {
		return yaw;
	}
	public void setYaw(int yaw) {
		this.yaw = yaw;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pitch;
		result = prime * result + roll;
		result = prime * result + yaw;
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
		UavOrientation other = (UavOrientation) obj;
		if (pitch != other.pitch)
			return false;
		if (roll != other.roll)
			return false;
		if (yaw != other.yaw)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "UavOrientation [roll=" + roll + ", pitch=" + pitch + ", yaw="
				+ yaw + "]";
	}
}
