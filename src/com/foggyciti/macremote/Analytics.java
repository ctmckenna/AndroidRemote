package com.foggyciti.macremote;

import com.google.analytics.tracking.android.EasyTracker;

public class Analytics {
	
	private enum Category {
		control("remote_control");
		private String name;
		Category(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
	}
	
	private enum Action {
		connection("connection_status"),
		event("event");
		private String name;
		Action(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
	}
	
	private enum Label {
		connected("connected"),
		disconnected("disconnected");
		private String name;
		Label(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
	}
	
	public static void sendConnected() {
		EasyTracker.getTracker().sendEvent(Category.control.getName(), Action.connection.getName(), Label.connected.getName(), 0L);
	}
	
	public static void sendDisconnected() {
		EasyTracker.getTracker().sendEvent(Category.control.getName(), Action.connection.getName(), Label.connected.getName(), 0L);
	}
	
	private static RemoteEvent lastEvent = null;
	public static void remoteControlEvent(RemoteEvent ev) {
		if (ev == lastEvent)
			return;
		lastEvent = ev;
		EasyTracker.getTracker().sendEvent(Category.control.getName(), Action.event.getName(), ev.name(), 0L);
	}
}
