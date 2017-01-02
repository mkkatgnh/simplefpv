package de.mobile2power.simplefpv.groundcontrol.remotecontrol;

import java.io.Serializable;

public class ControlChannels implements Serializable {
	private static final long serialVersionUID = 2897376554058281001L;
	private int thrust = 128;
	private int yaw = 128;
	private int nick = 128;
	private int roll = 128;
	private int mNick = 128;
	private int mRoll = 128;
	private int mYaw = 128;
	public int getThrust() {
		return thrust;
	}
	public void setThrust(int thrust) {
		this.thrust = thrust;
	}
	public int getYaw() {
		return yaw;
	}
	public void setYaw(int yaw) {
		this.yaw = yaw;
	}
	public int getNick() {
		return nick;
	}
	public void setNick(int nick) {
		this.nick = nick;
	}
	public int getRoll() {
		return roll;
	}
	public void setRoll(int roll) {
		this.roll = roll;
	}
	public int getmNick() {
		return mNick;
	}
	public void setmNick(int mNick) {
		this.mNick = mNick;
	}
	public int getmRoll() {
		return mRoll;
	}
	public void setmRoll(int mRoll) {
		this.mRoll = mRoll;
	}
	public int getmYaw() {
		return mYaw;
	}
	public void setmYaw(int mYaw) {
		this.mYaw = mYaw;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mNick;
		result = prime * result + mRoll;
		result = prime * result + mYaw;
		result = prime * result + nick;
		result = prime * result + roll;
		result = prime * result + thrust;
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
		ControlChannels other = (ControlChannels) obj;
		if (mNick != other.mNick)
			return false;
		if (mRoll != other.mRoll)
			return false;
		if (mYaw != other.mYaw)
			return false;
		if (nick != other.nick)
			return false;
		if (roll != other.roll)
			return false;
		if (thrust != other.thrust)
			return false;
		if (yaw != other.yaw)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "ControlChannels [thrust=" + thrust + ", yaw=" + yaw + ", nick="
				+ nick + ", roll=" + roll + ", mNick=" + mNick + ", mRoll="
				+ mRoll + ", mYaw=" + mYaw + "]";
	}
}
