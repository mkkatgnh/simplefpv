package de.mobile2power.simplefpv;

import java.io.IOException;

import android.app.Activity;
import android.widget.Toast;
import eu.mightyfrog.udpcomm.common.ConnectionType;
import eu.mightyfrog.udpcomm.node.DatagrammNode;

public class SendToOppositeDeviceTask extends Thread {

	private DatagrammNode datagrammNode;
	private Activity parentActivity;
	private long currentTime = 0;
	private boolean running = true;
	boolean udpConnectionEstablished = false;

	private IPointCommunication pointCommunication;

	public SendToOppositeDeviceTask(Activity parent) {
		this.parentActivity = parent;
	}

	public SendToOppositeDeviceTask(Activity parent, DatagrammNode node) {
		this.parentActivity = parent;
		this.datagrammNode = node;
	}

	public IPointCommunication getPointCommunication() {
		return pointCommunication;
	}

	public void setPointCommunication(IPointCommunication pointCommunication) {
		this.pointCommunication = pointCommunication;
	}

	public boolean isUdpConnectionEstablished() {
		return udpConnectionEstablished;
	}

	public void setUdpConnectionEstablished(boolean udpConnectionEstablished) {
		this.udpConnectionEstablished = udpConnectionEstablished;
	}

	public void setDatagrammNode(DatagrammNode node) {
		this.datagrammNode = node;
	}

	public DatagrammNode getDatagrammNode() {
		return this.datagrammNode;
	}

	public void stopTask() {
		if (datagrammNode != null
				&& !datagrammNode.getConnecionType()
						.equals(ConnectionType.NONE)) {
			datagrammNode.closeConnection();
		}
		running = false;
	}

	@Override
	public void run() {
		while (running) {
			if (udpConnectionEstablished) {
				if (!datagrammNode.getConnecionType().equals(
						ConnectionType.NONE))
					try {
						if (pointCommunication != null) {
							pointCommunication
									.communicateTaskCaller(currentTime);
						}
					} catch (IOException e1) {
						Toast.makeText(parentActivity,
								"Cannot send control data!", Toast.LENGTH_LONG)
								.show();
					}
			}
			try {
				sleep(5);
			} catch (InterruptedException e) {
				Toast.makeText(parentActivity, e.getMessage(),
						Toast.LENGTH_LONG).show();
			}
			currentTime = System.currentTimeMillis();
		}
	}

}
