package de.mobile2power.simplefpv.external;


public class SerialMWC {

	private static final byte MWC_RCDATA_ID = (byte)200;
	private static final byte PAYLOAD_SIZE_8CHANNELS = (byte)16;
	private static final byte[] START_SEQUENCE = new byte[] {36, 77, 60};
	static byte[] transferBytes = new byte[22];
	static int transferBytesPointer = 0;
	
	public static byte[] transformControlCommands(int[] channels) {
		for (byte byteValue : START_SEQUENCE) {
			transferBytes[transferBytesPointer++] = byteValue;
		}
		byte checksum = 0;
		transferBytes[transferBytesPointer++] = PAYLOAD_SIZE_8CHANNELS;
		checksum ^= (PAYLOAD_SIZE_8CHANNELS & 0xFF);
		transferBytes[transferBytesPointer++] = MWC_RCDATA_ID;
		checksum ^= MWC_RCDATA_ID;
		for (int i = 0; i < 8; i++) {
			// channels[x] range: 128 - 255, mwcStickValue range: 1000 - 2000
			int mwcStickValue = (channels[i] - 128) * 1000 / 128 + 1000;
			char charValue = (char)(mwcStickValue & 0xFF);
			checksum ^= (charValue & 0xFF);
			transferBytes[transferBytesPointer++] = (byte)(charValue & 0xFF);
			charValue = (char)((mwcStickValue >> 8) & 0xFF);
			checksum ^= (charValue & 0xFF);
			transferBytes[transferBytesPointer++] = (byte)(charValue & 0xFF);
		}
		transferBytes[transferBytesPointer++] = checksum;
		transferBytesPointer = 0;
		return transferBytes;
	}
}
