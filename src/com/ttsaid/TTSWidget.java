/*
 * This file is part of the TTSAid project.
 *
 * Copyright (C) 2011-2012 Carlos Barcellos <carlosbar@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301 USA
 */

package com.ttsaid;

import com.ttsaid.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class TTSWidget extends AppWidgetProvider {
	public	static	final	String					PREFS_DB = "com.ttsaid.prefs.db";
	public	static	final	String					PLAY_TIME = "com.ttsaid.intent.action.PLAY_TIME";
	public	static	final	String					PLAY_AND_ENQUEUE = "com.ttsaid.intent.action.PLAY_AND_ENQUEUE";
	public	static	final	String					DO_NOTHING = "com.ttsaid.intent.action.DO_NOTHING";
	public	static	final	String					CONFIG_CHANGED = "com.ttsaid.intent.action.CONFIG_CHANGED";
	public	static	final	int						FROM_PERIOD = 800; // default 'from' period 8:00am
	public	static	final	int						TO_PERIOD = 1900; // default 'to' period 7:00pm
	public	static	final	int						ALARM_MIN_INTERVAL = 15;
	public	static	final	String					SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
	}
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		super.onReceive(context, intent);

		Log.d("TTSWidget onreceive", "receiving action: " + intent.getAction());
		if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction()) || TTSWidget.CONFIG_CHANGED.equals(intent.getAction()) || TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction()) || PLAY_TIME.equals(intent.getAction()) || PLAY_AND_ENQUEUE.equals(intent.getAction()) || SMS_RECEIVED_ACTION.equals(intent.getAction()) || Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
			SharedPreferences prefs =  context.getSharedPreferences(PREFS_DB, 0);

			if(AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
				SharedPreferences.Editor prefset = prefs.edit();
				prefset.putInt("LAST_SET_INTERVAL",-1);
				prefset.commit();
			} else if (TTSWidget.CONFIG_CHANGED.equals(intent.getAction())) {
				if(prefs.getInt("LAST_SET_INTERVAL",-1) != prefs.getInt("SET_INTERVAL",0)) {
					SharedPreferences.Editor prefset = prefs.edit();
					prefset.putInt("LAST_SET_INTERVAL",prefs.getInt("SET_INTERVAL",0));
					prefset.commit();
					final Intent si = new Intent(context,TTSService.class);
					si.setAction(CONFIG_CHANGED);
					context.startService(si);
				}
			} else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
				if (intent.hasExtra(TelephonyManager.EXTRA_STATE)) {
					if(intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
						if(prefs.getBoolean("PHONE_FLASH", true)) flashLight(context);
						if(prefs.getBoolean("SET_CALLER_ID", false)) {
							final Intent si = new Intent(context,TTSService.class);
							si.setAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
							si.putExtra("phoneNumber",intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
							context.startService(si);
						}
					}
				}
			} else if (PLAY_TIME.equals(intent.getAction()) || PLAY_AND_ENQUEUE.equals(intent.getAction()) || Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				final Intent si = new Intent(context,TTSService.class);
				si.setAction(PLAY_TIME);
				if(PLAY_AND_ENQUEUE.equals(intent.getAction())) {
					si.putExtra("enqueue",true);
				}
				if(Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
					si.putExtra("screenEvent",true);
				}
				context.startService(si);
			} else if(SMS_RECEIVED_ACTION.equals(intent.getAction())) {
				if(prefs.getBoolean("SMS_FLASH", true)) flashLight(context);
				if(prefs.getBoolean("SET_SMS_RECEIVE", false)) {
					Bundle bundle = intent.getExtras();
					if(bundle == null) {
						return;
					}
					Object[] pdus = (Object[])bundle.get("pdus");
					final SmsMessage[] messages = new SmsMessage[pdus.length];
					for (int i = 0; i < pdus.length; i++) {
						messages[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
					}
					if (messages.length > -1) {
						final Intent si = new Intent(context,TTSService.class);
						si.setAction(SMS_RECEIVED_ACTION);
						si.putExtra("message",messages[0].getMessageBody());
						si.putExtra("phoneNumber",messages[0].getOriginatingAddress());
						context.startService(si);
					}
				}
			}
		}
	}
	
	/**
	 * flash the camera light
	 */
	public void flashLight(Context context) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				for(int i=0;i < 3;i++) {
					try {
						Camera mCamera = Camera.open();
						Parameters params = mCamera.getParameters();
						for(int x=0;x < 3;x++) {
				            params.setFlashMode( Parameters.FLASH_MODE_TORCH);
				            mCamera.setParameters(params);
				            mCamera.startPreview();
				            try {
				            	Thread.sleep(10);
			                } catch (InterruptedException e) {
			                	e.printStackTrace();
			                }
				            params.setFlashMode( Parameters.FLASH_MODE_OFF);
							mCamera.setParameters(params);
							mCamera.stopPreview();
						}
						mCamera.release();
					} catch (Exception e) {
					}
		            try {
		            	Thread.sleep(50);
	                } catch (InterruptedException e) {
	                	e.printStackTrace();
	                }
				}
			}
		});
		t.start();
	}

	@Override
	public void onUpdate(final Context context, AppWidgetManager appWidgetManager,int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		final int N = appWidgetIds.length;
		RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.main);

		Log.d("TTSWidget", "on update ");
		/* update every app from this provider */
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			/* set play sound action on widget canvas */
			Intent play = new Intent(PLAY_TIME);
			play.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0, play, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.play, pendingIntent);
			/* set config activity for small icon */
			Intent config = new Intent(context, TTSActivity.class);
			config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent pendingConfig = PendingIntent.getActivity(context, 0,config, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.config, pendingConfig);
			/* update widget */
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
}


