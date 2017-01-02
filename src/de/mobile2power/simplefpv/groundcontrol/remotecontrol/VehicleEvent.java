package de.mobile2power.simplefpv.groundcontrol.remotecontrol;

import java.io.Serializable;

import de.mobile2power.simplefpv.groundcontrol.VehicleEventType;

public class VehicleEvent implements Serializable {

	private static final long serialVersionUID = -3976241259873258301L;
	
	private VehicleEventType type;

	public VehicleEvent(VehicleEventType type) {
		this.type = type;
	}
	
	public VehicleEventType getType() {
		return type;
	}

	public void setType(VehicleEventType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		VehicleEvent other = (VehicleEvent) obj;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "VehicleEvent [type=" + type + "]";
	}

}
