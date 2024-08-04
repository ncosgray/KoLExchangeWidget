/*
 *******************************************************************************
 Package:  com.nathanatos.kolexchangewidget
 Class:    KoLExchangeWidget.java
 Author:   Nathan Cosgray | https://www.nathanatos.com
 -------------------------------------------------------------------------------
 Copyright (c) 2013-2024 Nathan Cosgray. All rights reserved.
 This source code is licensed under the BSD-style license found in LICENSE.txt.
 *******************************************************************************
*/

// KoLExchangeWidget class
// - Widget configuration
// - Handle widget clicks

package com.nathanatos.kolexchangewidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KoLExchangeWidget extends AppWidgetProvider {

    // Update widget data
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        final String logTag = "onUpdate";

        // Wait for active network, with retries
        int retries = Constants.KOLEXCHANGE_RETRIES;
        try {
            while (!KoLExchangeData.isNetworkConnected(context) && retries > 0) {
                Log.w(logTag, "Waiting for network connection");
                retries--;

                // Pause before retrying
                TimeUnit.MILLISECONDS.sleep(Constants.KOLEXCHANGE_TIMEOUT);
            }
        } catch (Exception e) {
            Log.e(logTag, e.getMessage());
        }

        // Update all widgets
        Log.i(logTag, "Starting all widgets update");
        for (int appWidgetId : appWidgetIds) {
            doWidgetUpdate(context, appWidgetManager, appWidgetId);
        }

        // Schedule the next update
        scheduleUpdate(context);

    }

    // Handle widget click
    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        final String logTag = "onReceive";

        // Process a user click on the widget
        if (intent != null && intent.getAction().equals(Constants.KOLEXCHANGE_CLICK_ACTION)) {

            // Get the intent details
            int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            Bundle extras = intent.getExtras();
            if (extras != null) {
                appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
            }

            // Update this widget
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.i(logTag, "Starting single widget update");
                doWidgetUpdate(context, AppWidgetManager.getInstance(context), appWidgetId);
            }

            // Open the main activity
            Intent mainIntent = new Intent (context, MainActivity.class);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainIntent);

        }

    }

    // Last instance of widget deleted
    @Override
    public void onDisabled(Context context) {

        final String logTag = "onDisabled";

        Log.i(logTag, "Canceling widget updates");
        cancelUpdate(context);

    }

    // Fetch active widget IDs
    private int[] getActiveWidgetIds(Context context) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, KoLExchangeWidget.class);

        return appWidgetManager.getAppWidgetIds(componentName);

    }

    // Generate a click intent for a widget
    private PendingIntent getClickPendingIntent(Context context, int appWidgetId) {

        // Set up the click intent
        Intent intent = new Intent(context, KoLExchangeWidget.class);
        intent.setAction(Constants.KOLEXCHANGE_CLICK_ACTION);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        return PendingIntent.getBroadcast(context,
                Constants.KOLEXCHANGE_CLICK_REQUEST,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

    }

    // Generate an update intent
    private PendingIntent getUpdatePendingIntent(Context context) {

        // Set up the update intent
        int[] widgetIds = getActiveWidgetIds(context);
        Intent intent = new Intent(context, KoLExchangeWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);

        return PendingIntent.getBroadcast(context,
                Constants.KOLEXCHANGE_UPDATE_REQUEST,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // Use alarm manager to schedule widget update
    private void scheduleUpdate(Context context) {

        if (getActiveWidgetIds(context).length > 0) {

            // Set inexact alarm for next update
            ZonedDateTime nextUpdate = ZonedDateTime.now().plus(Constants.KOLEXCHANGE_UPDATE_DURATION);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        nextUpdate.toInstant().toEpochMilli(),
                        getUpdatePendingIntent(context));
            }

        }
    }

    // Cancel any pending widget update intent
    private void cancelUpdate(Context context) {

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(getUpdatePendingIntent(context));
        }

    }

    // Asynchronously update the widget data and set click intent
    private void doWidgetUpdate(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {

            String updateText = null;

            // Load data from web service
            @Override
            public void run() {

                // Load exchange rate
                updateText = KoLExchangeData.getExchangeRate();

                // After loading data, apply updates to the widget
                handler.post(() -> {

                    // Update the widget text only if a value was received
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_main);
                    if (updateText != null) {
                        views.setTextViewText(R.id.widget_textview, updateText);
                    }

                    // Set up the click intent
                    views.setOnClickPendingIntent(R.id.widget_rootview,
                            getClickPendingIntent(context, appWidgetId));

                    // Apply updates
                    appWidgetManager.updateAppWidget(appWidgetId, views);

                });
            }
        });

    }

}
