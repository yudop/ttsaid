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
import android.util.Log;
import android.widget.RemoteViews;

public class TTSWidget extends AppWidgetProvider {

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);

		/* stop service */
		Intent intent = new Intent(context, LocalService.class);
		context.stopService(intent);
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {
		super.onReceive(context, intent);

		Log.d("TTSWidget onreceive", "receiving action: " + intent.getAction());
		if (LocalService.PLAY_SOUND.equals(intent.getAction())) {
			final Intent play = new Intent(context, LocalService.class);
			play.putExtra("PLAY_SOUND", true);
			Thread t = new Thread(new Runnable() {
				
				public void run() {
					context.startService(play);
				}
			});
			t.start();
		} else if (LocalService.PLAY_AND_ENQUEUE.equals(intent.getAction())) {
			final Intent play = new Intent(context, LocalService.class);
			play.putExtra("PLAY_AND_ENQUEUE", true);
			Thread t = new Thread(new Runnable() {
				
				public void run() {
					context.startService(play);
				}
			});
			t.start();
		}
	}

	@Override
	public void onUpdate(final Context context, AppWidgetManager appWidgetManager,int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		final int N = appWidgetIds.length;
		RemoteViews views = new RemoteViews(context.getPackageName(),R.layout.main);

		/* start service */

		final Intent intent = new Intent(context, LocalService.class);
		Thread t = new Thread(new Runnable() {
			
			public void run() {
				context.startService(intent);
			}
		});
		t.start();

		Log.d("TTSWidget", "on update ");
		/* update every app from this provider */
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];
			/* set play sound action on widget canvas */
			Intent play = new Intent(LocalService.PLAY_SOUND);
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
