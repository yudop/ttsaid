package com.ttsaid;

import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.provider.ContactsContract.PhoneLookup;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;

public class TTSService extends Service {
	private	enum			playType	{flush,skip,add};
	private					HashMap<String,String>	myHash;
	private final IBinder mBinder = new LocalBinder();

	private SharedPreferences prefs;
	private TextToSpeech mTTS;

	public void playCallerId(TextToSpeech mTTS, Context context,String str,String incomingMessage,int repeat,String language) {
		Cursor cursor;

		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,Uri.encode(str));
		cursor = context.getContentResolver().query(uri,new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		if (cursor.getCount() > 0) {
			cursor.moveToNext();
			str = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}
		/* append incoming call message */
		str = String.format("%s %s",incomingMessage,str);
		playSound(mTTS, context, str, playType.flush,language);
		for(int x=1;x < repeat;x++) {
			playSilence(mTTS, context,400);
			playSound(mTTS, context,str,playType.add,language);
		}
	}

	/* play silence with defined duration in milliseconds */
	public void playSilence(TextToSpeech mTTS,Context context,int duration) {
		mTTS.playSilence(duration,TextToSpeech.QUEUE_ADD,myHash);
	}

	public void playSMS(TextToSpeech mTTS, Context context, String str,String number,String language,int repeat,String smsDefaultText)
	{
		Cursor cursor;

		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,Uri.encode(number));
		cursor = context.getContentResolver().query(uri,new String[] { PhoneLookup.DISPLAY_NAME }, null, null, null);
		if (cursor.getCount() > 0) {
			cursor.moveToNext();
			number = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
		}
		str = String.format("%s %s. %s",smsDefaultText,number,str);  
		playSound(mTTS, context, str, playType.flush,language);
		for(int x=1;x < repeat;x++) {
			playSilence(mTTS, context, 400);
			playSound(mTTS, context, str,  playType.add,language);
		}
	}
	
	public void playSound(TextToSpeech mTTS,Context context, String str,playType type,String language) {
		Locale	loc;
		int		mode;
		int		avail;

		if (mTTS == null) {
			return;
		}
		/* check language */
		loc = new Locale(language);
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
		try {
			Log.d(this.getPackageName(),"Speaking: " + str);
			mTTS.speak(str, mode,myHash);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/* set current stream */
	public void setStream(int stream)
	{
		myHash = new HashMap<String, String>();
		switch(stream) {
		case 1:
		    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
		    break;
		case 2:
		    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_NOTIFICATION));
		    break;
		case 3:
		    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_RING));
		    break;
		case 4:
		    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_SYSTEM));
		    break;
		case 0:
		default:
		    myHash.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
		    break;
		}
	}
	
	/* return formatted date & time according to locale */
	public String getTimeString(Context context, Locale l,int hour,int minute)
	{
		String str;
			
		if(l.getLanguage().startsWith("pt")) {
			str = String.format(context.getString(R.string.lang_dt_pt),hour,minute);
		} else if(l.getLanguage().startsWith("en")) {
			str = String.format(context.getString(R.string.lang_dt_en),hour,minute);
		} else if(l.getLanguage().startsWith("es")) {
			str = String.format(context.getString(R.string.lang_dt_es),hour,minute);
		} else if(l.getLanguage().startsWith("it")) {
			str = String.format(context.getString(R.string.lang_dt_it),hour,minute);
		} else if(l.getLanguage().startsWith("ja")) {
			str = String.format(context.getString(R.string.lang_dt_ja),hour,minute);
		} else if(l.getLanguage().startsWith("ch")) {
			str = String.format(context.getString(R.string.lang_dt_ch),hour,minute);
		} else if(l.getLanguage().startsWith("de")) {
			str = String.format(context.getString(R.string.lang_dt_de),hour,minute);
		} else if(l.getLanguage().startsWith("hi")) {
			str = String.format(context.getString(R.string.lang_dt_hi),hour,minute);
		} else if(l.getLanguage().startsWith("ko")) {
			str = String.format(context.getString(R.string.lang_dt_ko),hour,minute);
		} else if(l.getLanguage().startsWith("ru")) {
			str = String.format(context.getString(R.string.lang_dt_ru),hour,minute);
		} else if(l.getLanguage().startsWith("el")) {
			str = String.format(context.getString(R.string.lang_dt_el),hour,minute);
		} else if(l.getLanguage().startsWith("fr")) {
			str = String.format(context.getString(R.string.lang_dt_fr),hour,minute);
		} else if(l.getLanguage().startsWith("ar")) {
			str = String.format(context.getString(R.string.lang_dt_ar),hour,minute);
		} else if(l.getLanguage().startsWith("nl")) {
			str = String.format(context.getString(R.string.lang_dt_nl),hour,minute);
		} else { 
			str = String.format("%02d:%02d",hour,minute);
		}
		return(str);
	}

	/* play current date & time */
	public void playTime(TextToSpeech mTTS,Context context,boolean userpress,String language,int timeFormat,int fromPeriod,int toPeriod) {
		
		if(!userpress) {
			Time	t = new Time();
			int		tn;
			
			t.setToNow();
			tn = new Integer(t.format("%H%M"));
			if(tn < fromPeriod || tn > toPeriod) {
				return;
			}
		}
		Locale loc = new Locale(language);
		StringBuilder sb = new StringBuilder();
		Date date = new Date(System.currentTimeMillis());
		
		Formatter formatter = new Formatter(sb,loc);
		formatter.format(loc,"%tA, ",date);		// localized weekday name
		formatter.format(loc,"%tB ",date);		// localized month name
		formatter.format(loc,"%te. ",date);		// day of month
		if(timeFormat == 12) {
			sb.append(getTimeString(context,loc,(date.getHours() % 12 == 0) ? 12 : date.getHours()%12,date.getMinutes()));
			formatter.format(loc," %Tp",date);	// am or pm
		} else {
			sb.append(getTimeString(context,loc,date.getHours(),date.getMinutes()));
		}
		playSound(mTTS,context, sb.toString(), playType.skip,language);
	}

	public void enQueue(Context context,int interval) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

		Intent play = new Intent((interval != 0) ? TTSWidget.PLAY_AND_ENQUEUE : TTSWidget.DO_NOTHING);
		PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, play,PendingIntent.FLAG_CANCEL_CURRENT);
		Calendar ct = Calendar.getInstance();
		ct.setTimeInMillis(System.currentTimeMillis());
		ct.add(Calendar.MINUTE,(interval < TTSWidget.ALARM_MIN_INTERVAL) ? TTSWidget.ALARM_MIN_INTERVAL : interval);
		alarmManager.set(AlarmManager.RTC_WAKEUP, ct.getTimeInMillis(),alarmIntent);
	}
	
	/* constructor */
	public TTSService() {
	}
	
	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		TTSService getService() {
			return TTSService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public int onStartCommand(final Intent intent, int flags, int startId) {
		AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		if(mAudioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
			return Service.START_NOT_STICKY;
		}
		try {
			prefs = getSharedPreferences(TTSWidget.PREFS_DB, 0);
			mTTS = new TextToSpeech(this, new OnInitListener() {
				public void onInit(int status) {
					BroadcastReceiver receiver = null;

					if(status == TextToSpeech.SUCCESS) {
						if(TTSWidget.PLAY_TIME.equals(intent.getAction())) {
							setStream(prefs.getInt("STREAM",0));
							Log.d(TTSService.this.getPackageName(),"screen event == " + intent.getBooleanExtra("screenEvent",false));
							if(!intent.getBooleanExtra("screenEvent",false) || prefs.getBoolean("SET_SCREEN_EVENT",true)) {
								playTime(mTTS,TTSService.this,intent.getBooleanExtra("enqueue",false) == false,prefs.getString("SET_LANGUAGE", "en"),prefs.getInt("TIME_FORMAT",12),prefs.getInt("FROM_PERIOD",TTSWidget.FROM_PERIOD),prefs.getInt("TO_PERIOD",TTSWidget.TO_PERIOD));
							}
							if(intent.getBooleanExtra("enqueue",false)) {
								enQueue(TTSService.this,prefs.getInt("SET_INTERVAL",0));
							}
						} else if(TTSWidget.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
							playSMS(mTTS, TTSService.this,intent.getStringExtra("message"),intent.getStringExtra("phoneNumber"),prefs.getString("SET_LANGUAGE", "en"),prefs.getInt("REPEAT_SMS",2),prefs.getString("SMS_MESSAGE","SMS Received!"));
						} else if(TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
							playCallerId(mTTS,TTSService.this,intent.getStringExtra("phoneNumber"),prefs.getString("INCOMING_MESSAGE","Incoming Call!"),prefs.getInt("REPEAT_CALLER_ID",2),prefs.getString("SET_LANGUAGE", "en"));
							receiver = new BroadcastReceiver() {
								@Override
								public void onReceive(Context context, Intent intent) {
									if(TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
										if(intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_OFFHOOK) ||
												intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_IDLE)) {
													playSound(mTTS, context,"",playType.flush,prefs.getString("SET_LANGUAGE", "en"));
										}
									}
								}
							};
							registerReceiver(receiver,new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
						}
						while(mTTS.isSpeaking()) {
							try {
								Thread.sleep(100);
							} catch (Exception e) {
							}
						}
						if(receiver != null) unregisterReceiver(receiver);
						mTTS.shutdown();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Service.START_NOT_STICKY;
	}
}
