package com.foggyciti.macremote;

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
