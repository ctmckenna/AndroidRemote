package com.example.androidremote;

public enum RemoteEvent {
	CLICK,
	MOVE,
	DRAG,
	UP,
	PING;
	
	public byte getId() {
		return (byte)(ordinal()+1);
	}
}
