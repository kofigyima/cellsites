package com.cellsites.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


public class AlertDialogManager extends Activity{

/**
	 * Function to display simple Alert Dialog
	 * @param context - application context
	 * @param title - alert dialog title
	 * @param message - alert message
	 * @param status - success/failure (used to set icon)
	 * 				 - pass null if you don't want icon
	 * icon - null - no icon
	 * 0 - failure
	 * 1 - success
	 * 2 - information
	 * 3 - question
	 * 4 - 
	 * */
	
	static Boolean confirmed;

	public static Boolean showAlertDialog(Context context, Activity act,String title, String message,
			String butOK,String butCan,String butNue,Integer icon) {
	    final Handler handler = new Handler() {
	        @Override
	        public void handleMessage(Message mesg) {
	            throw new RuntimeException();
	        } 
	    };
		int corient = act.getRequestedOrientation();
		act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

	AlertDialog.Builder alertdiag = new AlertDialog.Builder(context)
	.setMessage(message)
	.setTitle(title);
	//set buttons
	if(butOK!=null)
		alertdiag = alertdiag.setPositiveButton(butOK,new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog,int which) {
				confirmed=true;
				 handler.sendMessage(handler.obtainMessage());
			}
		});
	if(butCan!=null)
		alertdiag = alertdiag.setNegativeButton(butCan,new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog,int which) {
				confirmed=false;
				 handler.sendMessage(handler.obtainMessage());
			}
		});
	if(butNue!=null)
		alertdiag = alertdiag.setNegativeButton(butNue,new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog,int which) {
					//user clicked ok - do nothing
				confirmed=null;
				 handler.sendMessage(handler.obtainMessage());
			}
		});
	//set icon
	if(icon!=null)
		alertdiag = alertdiag.setIcon(icon);
	alertdiag.show();
	//loop until a button is clicked
	try { Looper.loop(); }
    catch(RuntimeException e2) {}	
	act.setRequestedOrientation(corient);
	return confirmed;
	}
}