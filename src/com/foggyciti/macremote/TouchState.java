package com.foggyciti.macremote;

public enum TouchState {
	holding,
	dragging_1,
	dragging_2,
	scrolling,
	moving,
	empty;
	static int holder;    /* when dragging or holding, id of pointer that initiated the event */
}
