package com.example.androidremote;

public abstract class RunnableOneArg implements Runnable {
	private Object arg;
	public RunnableOneArg(Object arg) {
		this.arg = arg;
	}
	public Object getArg() {
		return arg;
	}
	public abstract void run();
}
