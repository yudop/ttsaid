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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Formatter;

import com.ttsaid.R;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class LocalService extends Service {

	/* show debug messages */

	final private boolean showDebugMsg = false;

	/* static */

	public static final String	PLAY_SOUND = "com.ttsaid.intent.action.PLAY_SOUND";
	public static final String	PLAY_AND_ENQUEUE = "com.ttsaid.intent.action.PLAY_AND_ENQUEUE";
	public static final String	PREFS_DB = "com.ttsaid.prefs.db";
	public static final int		ALARM_MIN_INTERVAL = 15;

	private final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	private enum playType {flush,skip,add};
	private static final int SERVICE_ID = 0x00674656;

	/* private */
	private int interval = 0;
	private AlarmManager alarmManager;
	private boolean started = false;

	/* private */
	
	private NotificationManager		mNM;
	private TextToSpeech			mTTS = null;
	private SharedPreferences		prefs;
	private BroadcastReceiver		receiver;
	private PendingIntent			alarmIntent;
	private boolean					duringCall = false;
	private HashMap<String,String>	myHash = new HashMap<String, String>();
	private boolean					ringMute = false;

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		LocalService getService() {
			return LocalService.this;
		}
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showToast("TTSAid onCreate() service");
		// showNotification();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		/* create handle when message is starting */
		if (!started) {
			AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
			if(mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
				ringMute=true;
			}			
		    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
			alarmIntent = null;
			prefs = getSharedPreferences(LocalService.PREFS_DB, 0);
			alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			mTTS = new TextToSpeech(this, new OnInitListener() {
				public void onInit(int status) {
				}
			});
			receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					showToast("receiving broadcast: " + intent.getAction());
					if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
						if (prefs.getBoolean("SET_SCREEN_EVENT", false)) {
							playTime();
						}
					} else if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
						showToast("incoming call");
						if (intent.hasExtra(TelephonyManager.EXTRA_STATE)) {
							if(intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING) && prefs.getBoolean("SET_PHONE_NUMBER", false)) {
								playPhoneNumber(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER));
							} else if(intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
								playSound("",playType.flush);
								duringCall = true;
								showToast("during call");
							} else if(intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
								duringCall = false;
								showToast("call disconnected");
							}
						}
					} else if(SMS_RECEIVED_ACTION.equals(intent.getAction()) && prefs.getBoolean("SET_SMS_RECEIVE", false)) {
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
							playSMS(messages[0].getMessageBody(),messages[0].getOriginatingAddress());
						}
					} else if(AudioManager.RINGER_MODE_CHANGED_ACTION.equals(intent.getAction())) {
						if(intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE,-1) == AudioManager.RINGER_MODE_NORMAL) {
							showToast("phone unmuted");
							ringMute=false;
						} else {
							showToast("phone muted");
							ringMute=true;
						}
					}
				}
			};
			registerReceiver(receiver, new IntentFilter(
					TelephonyManager.ACTION_PHONE_STATE_CHANGED));
			registerReceiver(receiver,
					new IntentFilter(Intent.ACTION_SCREEN_ON));
			registerReceiver(receiver,
					new IntentFilter(SMS_RECEIVED_ACTION));
			registerReceiver(receiver,
					new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION));
			started = true;
		}
		/* adjust interval in quarters of hour */
		int x = prefs.getInt("SET_INTERVAL", interval);
		if (x != interval) {
			interval = x;
			enQueue();
		}
		/* verify if we want to speak the current date&time */
		if (intent.getBooleanExtra("PLAY_SOUND", false)) {
			showToast("playing current date&time");
			playTime();
		}
		/* verify if this is an automatic play time event */
		if (intent.getBooleanExtra("PLAY_AND_ENQUEUE", false)) {
			showToast("playing enqueued event");
			playTime();
			enQueue();
		}
		/*
		 * We want this service to continue running until it is explicitly
		 * stopped, so return sticky.
		 */
		return START_STICKY;
	}

	public void enQueue() {
		if (!started || alarmManager == null) {
			return;
		}
		if(interval == 0) {
			if(alarmIntent != null) {
				showToast("removing previous alarm: " + alarmIntent.toString());
				alarmManager.cancel(alarmIntent);				
			}
			alarmIntent=null;
			return;
		}
		Intent play = new Intent(LocalService.PLAY_AND_ENQUEUE);
		alarmIntent = PendingIntent.getBroadcast(LocalService.this, 0, play,PendingIntent.FLAG_UPDATE_CURRENT);
		Calendar ct = Calendar.getInstance();

		/* calculate number of minutes */
		long minutes = (long) ((float) System.currentTimeMillis() / 1000. / 60. + interval * ALARM_MIN_INTERVAL);
		/* adjust for last quarter before interval */
		minutes = (minutes+1) / ALARM_MIN_INTERVAL * ALARM_MIN_INTERVAL;
		/* adjust calendar */
		ct.setTimeInMillis(minutes * 60 * 1000);
		showToast("creating enqueued alarm " + ct.getTime().getHours() + String.format(":%02d", ct.getTime().getMinutes()));
		alarmManager.set(AlarmManager.RTC_WAKEUP, ct.getTimeInMillis(),alarmIntent);
	}

	public void playSMS(String str,String number)
	{
		Cursor cursor;

		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,Uri.encode(number));
		cursor = getContentResolver().query(uri,new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		if (cursor.getCount() > 0) {
			cursor.moveToNext();
			number = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}
		str = String.format("%s %s. %s",prefs.getString("SMS_MESSAGE","SMS Received!"),number,str);  
		playSound(str, playType.flush);
		playSound(str,  playType.add);
	}
	
	public void playPhoneNumber(String str) {
		Cursor cursor;

		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(str));
		cursor = getContentResolver().query(uri,
				new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		if (cursor.getCount() > 0) {
			cursor.moveToNext();
			str = cursor.getString(cursor
					.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}
		/* append incoming call message */
		str = String.format("%s %s",prefs.getString("INCOMING_MESSAGE","Incoming Call!"),str);
		playSound(str, playType.flush);
		playSound(str,  playType.add);
	}

	public void playTime() {
		Locale loc = new Locale(prefs.getString("SET_LANGUAGE", "en"));
		StringBuilder sb = new StringBuilder();
		Date date = new Date(System.currentTimeMillis());
		
		Formatter formatter = new Formatter(sb,loc);
		formatter.format(loc,"%tA, ",date);
		formatter.format(loc,"%tB ",date);
		formatter.format(loc,"%te. ",date);
		formatter.format(loc,"%tr ",date);
		
		playSound(sb.toString(), playType.skip);
	}

	public void playSound(String str,playType type) {
		Locale	loc;
		int		mode;
		int		avail;

		if (!started || mTTS == null) {
			return;
		}
		/* check language */

		loc = new Locale(prefs.getString("SET_LANGUAGE", "en"));

		/* verify if language can be applied */
		avail = mTTS.isLanguageAvailable(loc); 
		if (avail == TextToSpeech.LANG_AVAILABLE || avail == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
			mTTS.setLanguage(loc);
		} else {
			mTTS.setLanguage(Locale.getDefault());
		}
		switch(type) {
		case skip:
			if(mTTS.isSpeaking()) {
				showToast("TTS in use, skiping");
				return;
			}
			mode=TextToSpeech.QUEUE_FLUSH;
			break;
		case add:
			mode=TextToSpeech.QUEUE_ADD;
			break;
		case flush:
		default:
			mode=TextToSpeech.QUEUE_FLUSH;
			break;
		}
		/* speak current time */
		showToast("speak " + str);
		if(!duringCall && !ringMute) {
			try {
				mTTS.speak(str, mode,myHash);
			} catch(Exception e) {
				showToast(e.toString());
			}
		}
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(SERVICE_ID);
		// Tell the user we stopped.
		if(receiver != null) unregisterReceiver(receiver);
		if(mTTS != null) mTTS.shutdown();
		if(alarmManager != null && alarmIntent != null) alarmManager.cancel(alarmIntent);
		started = false;
		showToast("closing TTSAid service");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = "testexxx";

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, TTSActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, "testeyyyy", text, contentIntent);

		// Send the notification.
		mNM.notify(SERVICE_ID, notification);
	}

	public boolean isMyServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("com.example.MyService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/* show messages when in debug mode */
	private void showToast(String msg, int duration) {

		if (showDebugMsg) {
			Log.d(this.getPackageName(), msg);
			Toast.makeText(this, msg, duration).show();
		}
	}

	private void showToast(String msg) {
		showToast(msg, Toast.LENGTH_SHORT);
	}
}
