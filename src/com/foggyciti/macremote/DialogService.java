package com.foggyciti.macremote;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogService {
	
	public static void displaySingleOptionDialog(Context c, int msgId, int buttonId, final Callback onClick) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(c);
		alertDialogBuilder.setMessage(msgId);
		alertDialogBuilder.setPositiveButton(c.getResources().getString(buttonId), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (onClick != null)
					onClick.callback();
			}
		});
		alertDialogBuilder.create().show();
	}
}
