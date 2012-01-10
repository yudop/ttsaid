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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import com.ttsaid.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.CheckBox;
import android.widget.TimePicker;

public class TTSActivity extends Activity {
	private int MY_DATA_CHECK_CODE = 0x0001;
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private int interval;
	private boolean screenEvent;
	private boolean callerId;
	private boolean smsReceive;
	private SharedPreferences prefs;
	private TextToSpeech mTTS;
	private String incomingMessage,smsMessage;
	private int toPeriod,fromPeriod,repeatCallerId,repeatSMS,currentStream;

	/* get result from activities */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == MY_DATA_CHECK_CODE) {
			if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				// missing data, install it
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			} else {
				mTTS = new TextToSpeech(TTSActivity.this,new OnInitListener() {
					public void onInit(int status) {
					}
				});
			}
		}
	}

	/* list activity creation */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();

		/* set current view */
		setContentView(R.layout.config);
		String versionName;
		try {
			versionName = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			versionName="1";
		}
		setTitle(String.format("%s v. %s",getString(R.string.app_name),versionName));
		
		/* Set the result to CANCELED. This will cause the widget host to cancel
		   out of the widget placement if they press the back button. */
		setResult(RESULT_CANCELED);

		/* verify if TTS data is up to date */
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
		
		/* preferences database */
		prefs = getSharedPreferences(LocalService.PREFS_DB, 0);

		/* get parameters */

		incomingMessage = prefs.getString("INCOMING_MESSAGE","Incoming Call!");
		smsMessage = prefs.getString("SMS_MESSAGE","SMS Received from");
		repeatCallerId = prefs.getInt("REPEAT_CALLER_ID",2);
		repeatSMS = prefs.getInt("REPEAT_SMS",1);
		currentStream = prefs.getInt("STREAM",0);
		interval = prefs.getInt("SET_INTERVAL", interval);
		fromPeriod = prefs.getInt("FROM_PERIOD",LocalService.FROM_PERIOD);
		toPeriod = prefs.getInt("TO_PERIOD",LocalService.TO_PERIOD);
		screenEvent = prefs.getBoolean("SET_SCREEN_EVENT", false);
		callerId = prefs.getBoolean("SET_CALLER_ID", false);
		smsReceive = prefs.getBoolean("SET_SMS_RECEIVE", false);

		/* get widget id */
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		((TextView) findViewById(R.id.language)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final AlertDialog.Builder dlg = new AlertDialog.Builder(TTSActivity.this);
				Locale [] loclist = Locale.getAvailableLocales();
				final ArrayList<String> list = new ArrayList<String>();
				
				if(mTTS == null) {
					Toast.makeText(TTSActivity.this,"Text to Speech languages not loaded",Toast.LENGTH_LONG).show();
					return;
				}
				
				for(int x=0;x < loclist.length;x++) {
					if(mTTS.isLanguageAvailable(loclist[x]) == TextToSpeech.LANG_COUNTRY_AVAILABLE || (loclist[x].getCountry().length() == 0 && mTTS.isLanguageAvailable(loclist[x]) == TextToSpeech.LANG_AVAILABLE)) {
						String str;
						
						if(loclist[x].getCountry().length() > 0) {
							str = String.format("%s %s (%s_%s)",loclist[x].getDisplayLanguage(),loclist[x].getDisplayCountry(),loclist[x].getLanguage(),loclist[x].getCountry());
						} else {
							str = String.format("%s %s (%s)",loclist[x].getDisplayLanguage(),loclist[x].getDisplayCountry(),loclist[x].getLanguage());
						}
						int y;
		
						for(y=0;y < list.size();y++) {
							if(((String) list.get(y)).equals(str)) {
								break;
							}
						}
						if(y >= list.size()) {
							list.add(str);
						}
					}
				}
				final ArrayAdapter<String> adapter = new ArrayAdapter<String>(TTSActivity.this,R.layout.rowlayout,list);
				adapter.sort(new Comparator<String>() {
					public int compare(String a, String b) {
						return(a.compareTo(b));
					}
				});
				dlg.setAdapter(adapter,new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						String str = adapter.getItem(which);
						((EditText) findViewById(R.id.language)).setText(str.substring(str.indexOf("(")+1,str.indexOf(")")));
					}
				});
				dlg.show();
			}
		});
		
		/* time interval - on click */
		((TextView) findViewById(R.id.dateTimePlayback)).setOnClickListener(new OnClickListener() {			
			public void onClick(View arg0) {
				final AlertDialog.Builder dlg = new AlertDialog.Builder(TTSActivity.this);
				final LayoutInflater layoutInflater = LayoutInflater.from(TTSActivity.this);
				final View dateTimeView = layoutInflater.inflate(R.layout.interval, null);
				dlg.setView(dateTimeView);

				/* set current screen event mode */
				((CheckBox) dateTimeView.findViewById(R.id.screenEvent)).setChecked(screenEvent);

				/* set current date & time */

				((TextView) dateTimeView.findViewById(R.id.fromTime)).setText(String.format("%02d:%02d",fromPeriod/100,fromPeriod%100));
				((TextView) dateTimeView.findViewById(R.id.toTime)).setText(String.format("%02d:%02d",toPeriod/100,toPeriod%100));

				/* set interval event */
				((SeekBar) dateTimeView.findViewById(R.id.interval)).setMax(8);
				((SeekBar) dateTimeView.findViewById(R.id.interval)).setKeyProgressIncrement(1);
				((SeekBar) dateTimeView.findViewById(R.id.interval)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

							public void onStopTrackingTouch(SeekBar seekBar) {
							}

							public void onStartTrackingTouch(SeekBar seekBar) {
							}

							public void onProgressChanged(SeekBar seekBar,
									int progress, boolean fromUser) {
								interval = setTimeInterval(dateTimeView.findViewById(R.id.intervalValue),progress);
							}
						});
				
				/* screen event - on click */
				((CheckBox) dateTimeView.findViewById(R.id.screenEvent))
						.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

							public void onCheckedChanged(CompoundButton buttonView,
									boolean isChecked) {
								screenEvent = isChecked;
							}
						});
				
				/* from time - on click */
				((EditText) dateTimeView.findViewById(R.id.fromTime)).setFocusable(false);
				((EditText) dateTimeView.findViewById(R.id.fromTime)).setOnClickListener(new OnClickListener() {
					public void onClick(View view) {

						/* listener for 'from' time picker */
						TimePickerDialog.OnTimeSetListener timeFromListener = new TimePickerDialog.OnTimeSetListener() {
							public void onTimeSet(TimePicker view, int hour, int minute) {
								if(hour*100+minute > toPeriod) {
									Toast.makeText(TTSActivity.this,"To period must be equal or greater than From period",Toast.LENGTH_LONG).show();
								} else {
									fromPeriod=hour*100+minute;
									((TextView) dateTimeView.findViewById(R.id.fromTime)).setText(String.format("%02d:%02d",hour,minute));
								}
							}
						};
						final TimePickerDialog tm = new TimePickerDialog(TTSActivity.this,timeFromListener,fromPeriod/100,fromPeriod%100,true);
						tm.setTitle("Select time");
						tm.show();
					}
				});

				/* to time - on click */
				((EditText) dateTimeView.findViewById(R.id.toTime)).setFocusable(false);
				((EditText) dateTimeView.findViewById(R.id.toTime)).setOnClickListener(new OnClickListener() {
					public void onClick(View view) {
						/* listener for 'to' time picker */
						TimePickerDialog.OnTimeSetListener timeToListener = new TimePickerDialog.OnTimeSetListener() {
							public void onTimeSet(TimePicker view, int hour, int minute) {
								if(fromPeriod > hour*100+minute) {
									Toast.makeText(TTSActivity.this,"To period must be equal or greater than From period",Toast.LENGTH_LONG).show();
								} else {
									toPeriod=hour*100+minute;
									((TextView) dateTimeView.findViewById(R.id.toTime)).setText(String.format("%02d:%02d",hour,minute));
								}
							}
						};
						final TimePickerDialog tm = new TimePickerDialog(TTSActivity.this,timeToListener,toPeriod/100,toPeriod%100,true);
						tm.setTitle("Select time");
						tm.show();
					}
				});	
				
				/* set current interval */
				((SeekBar) dateTimeView.findViewById(R.id.interval)).setProgress(interval);
				dlg.show();
			}
		});

		((TextView) findViewById(R.id.incomingCall)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final AlertDialog.Builder dlg = new AlertDialog.Builder(TTSActivity.this);
				final LayoutInflater layoutInflater = LayoutInflater.from(TTSActivity.this);
				final View callView = layoutInflater.inflate(R.layout.incomingcall, null);
				dlg.setView(callView);
				
				((CheckBox) callView.findViewById(R.id.callerIdEvent)).setChecked(callerId);
				((CheckBox) callView.findViewById(R.id.callerIdEvent)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
						callerId = isChecked;
					}
				});
				((SeekBar) callView.findViewById(R.id.setRepeatCallerId)).setMax(3);
				((SeekBar) callView.findViewById(R.id.setRepeatCallerId)).setKeyProgressIncrement(1);
				((SeekBar) callView.findViewById(R.id.setRepeatCallerId)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					
					public void onStopTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub
						
					}
					
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub
						
					}
					
					public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
						repeatCallerId = progress + 1;
						((TextView) callView.findViewById(R.id.repeatCallerId)).setText(""+repeatCallerId);
					}
				});
				dlg.setOnCancelListener(new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						incomingMessage = ((EditText) callView.findViewById(R.id.incomingMessage)).getText().toString();
					}
				});
				((SeekBar) callView.findViewById(R.id.setRepeatCallerId)).setProgress(repeatCallerId-1);
				((EditText) callView.findViewById(R.id.incomingMessage)).setText(incomingMessage);
				dlg.show();
			}
		});

		/* incoming SMS */
		((TextView) findViewById(R.id.incomingSMS)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final AlertDialog.Builder dlg = new AlertDialog.Builder(TTSActivity.this);
				final LayoutInflater layoutInflater = LayoutInflater.from(TTSActivity.this);
				final View smsView = layoutInflater.inflate(R.layout.sms, null);
				dlg.setView(smsView);
				
				((CheckBox) smsView.findViewById(R.id.smsReceive)).setChecked(smsReceive);
				/* sms message event - on click */
				((CheckBox) smsView.findViewById(R.id.smsReceive)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						smsReceive = isChecked;
					}
				});
				((SeekBar) smsView.findViewById(R.id.setRepeatSMS)).setMax(3);
				((SeekBar) smsView.findViewById(R.id.setRepeatSMS)).setKeyProgressIncrement(1);
				((SeekBar) smsView.findViewById(R.id.setRepeatSMS)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
					
					public void onStopTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub
						
					}
					
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub
						
					}
					
					public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
						repeatSMS = progress + 1;
						((TextView) smsView.findViewById(R.id.repeatSMS)).setText(""+repeatSMS);
					}
				});
				dlg.setOnCancelListener(new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						smsMessage = ((EditText) smsView.findViewById(R.id.smsMessage)).getText().toString();
					}
				});
				((SeekBar) smsView.findViewById(R.id.setRepeatSMS)).setProgress(repeatSMS-1);
				((EditText) smsView.findViewById(R.id.smsMessage)).setText(smsMessage);
				dlg.show();
			}
		});
		
		((TextView) findViewById(R.id.streaming)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final AlertDialog.Builder dlg = new AlertDialog.Builder(TTSActivity.this);
				String [] streamList = {"Media","Alarm","Notification","Ringer","System"};
				final ArrayAdapter<String> adapter = new ArrayAdapter<String>(TTSActivity.this,android.R.layout.select_dialog_singlechoice,streamList);
				dlg.setSingleChoiceItems(adapter,currentStream,new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						currentStream = which;
						dialog.dismiss();
					}
				});
				dlg.show();
			}
		});
		
		/* set current values */

		((EditText) findViewById(R.id.language)).setText(prefs.getString("SET_LANGUAGE","en"));
	}

	private int setTimeInterval(View view,int progress)
	{
		String hour = "";
		int	m;
		
		if (progress == 0) {
			((TextView) view).setText(getString(R.string.off));
			return(0);
		}
		if (progress > 3) {
			hour = new Integer(progress / 4).toString();
		}
		m = new Integer(progress % 4 * LocalService.ALARM_MIN_INTERVAL);
		if (hour.length() > 0) {
			hour = hour + "h:" + String.format("%02d", m) + "m";
		} else {
			hour = m + " min";
		}
		((TextView) view).setText(hour);
		return(progress);
	}

	@Override
	public void onBackPressed() {
		// super.onBackPressed();

		RemoteViews views = new RemoteViews(TTSActivity.this.getPackageName(),
				R.layout.main);
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(TTSActivity.this);

		/* set play action on widget canvas */

		Intent play = new Intent(LocalService.PLAY_SOUND);
		play.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(TTSActivity.this, 0, play, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.play, pendingIntent);

		Intent config = new Intent(TTSActivity.this, TTSActivity.class);
		config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		PendingIntent pendingConfig = PendingIntent.getActivity(TTSActivity.this, 0, config, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.config, pendingConfig);

		/* update view */

		appWidgetManager.updateAppWidget(mAppWidgetId, views);

		/* set new values on service */

		Intent intent = new Intent(TTSActivity.this, LocalService.class);
		startService(intent);

		/* set result */

		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);

		/* save preferences */

		SharedPreferences.Editor prefset = prefs.edit();
		prefset.putInt("REPEAT_CALLER_ID", repeatCallerId);
		prefset.putInt("STREAM",currentStream);
		prefset.putInt("REPEAT_SMS", repeatSMS);
		prefset.putInt("SET_INTERVAL", interval);
		prefset.putBoolean("SET_SCREEN_EVENT", screenEvent);
		prefset.putBoolean("SET_CALLER_ID", callerId);
		prefset.putBoolean("SET_SMS_RECEIVE", smsReceive);
		prefset.putString("INCOMING_MESSAGE",incomingMessage);		
		prefset.putString("SMS_MESSAGE",smsMessage);
		prefset.putInt("FROM_PERIOD",fromPeriod);
		prefset.putInt("TO_PERIOD",toPeriod);
		String lang =  ((EditText) findViewById(R.id.language)).getText().toString();
		if(lang.length() == 0) lang = "en";
		prefset.putString("SET_LANGUAGE",lang);
		prefset.commit();

		/* start service */

		intent = new Intent(TTSActivity.this, LocalService.class);
		startService(intent);

		finish();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(mTTS != null) mTTS.shutdown();
	}
}
