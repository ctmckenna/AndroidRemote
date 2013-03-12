package com.foggyciti.macremote;

public class DatagramBuffer {
	private byte[] buffer = new byte[1024];
	private int wlocpos = 0;
	
	public int length() {
		return wlocpos;
	}
	
	public void reset() {
		wlocpos = 0;
	}
	
	public byte[] toArray() {
		return buffer;
	}
		
	public void copyByte(byte b) {
		buffer[wlocpos] = b;
		++wlocpos;
	}
	
	public void copyInt(int integer) {
		for (int i = 0; i < 4; i++) {
			copyByte((byte)((integer >>> (24 - i*8)) & 0xff));
		}
	}
	
	public void copyFloat(float f) {
		int fData = (int)(f*1000);
		copyInt(fData);
	}
	
	public void copyData(byte[] bytes) {
		for (int i = 0; i < bytes.length; ++i) {
			copyByte(bytes[i]);
		}
	}
}
