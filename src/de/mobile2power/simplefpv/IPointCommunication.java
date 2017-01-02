package de.mobile2power.simplefpv;

import java.io.IOException;

public interface IPointCommunication {
	void communicateTaskCaller(long currentTime) throws IOException;
}
