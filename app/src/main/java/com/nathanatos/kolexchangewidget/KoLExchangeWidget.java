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
// - Do widget data updates asynchronously
// - Handle widget clicks

package com.nathanatos.kolexchangewidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KoLExchangeWidget extends AppWidgetProvider {

    // Widget configuration constants
    private static final String KOLEXCHANGE_WS_URL = "https://www.nathanatos.com/kol/ws_getrate.php";
    private static final String KOLEXCHANGE_WS_NODE = "rate";
    private static final String KOLEXCHANGE_LABEL = "$1 US = ";
    private static final String KOLEXCHANGE_CLICK_URL = "https://www.nathanatos.com/kol-exchange-rate/";
    private static final String KOLEXCHANGE_CLICK_ACTION = "KoLWidgetClicked";
    private static final int KOLEXCHANGE_CLICK_REQUEST = 0;
    private static final Duration KOLEXCHANGE_UPDATE_DURATION = Duration.ofMinutes(60);
    private static final int KOLEXCHANGE_UPDATE_REQUEST = 1;
    private static final int KOLEXCHANGE_TIMEOUT = 1000; // milliseconds
    private static final int KOLEXCHANGE_RETRIES = 3;

    // Update widget data
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // Update all widgets
        Log.i("onUpdate", "Starting all widgets update");
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

        // Process a user click on the widget
        if (intent != null && intent.getAction().equals(KOLEXCHANGE_CLICK_ACTION)) {

            // Open the KoL Exchange rate website in browser
            try {
                Intent webIntent = new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(KOLEXCHANGE_CLICK_URL));
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webIntent);
            } catch (RuntimeException e) {
                Log.e("onReceive", e.getMessage());
            }

            // Get the intent details
            int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            Bundle extras = intent.getExtras();
            if (extras != null) {
                appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
            }

            // Update this widget
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.i("onReceive", "Starting single widget update");
                doWidgetUpdate(context, AppWidgetManager.getInstance(context), appWidgetId);
            }
        }

    }

    // Last instance of widget deleted
    @Override
    public void onDisabled(Context context) {

        Log.i("onDisabled", "Canceling widget updates");
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
        intent.setAction(KOLEXCHANGE_CLICK_ACTION);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        return PendingIntent.getBroadcast(context,
                KOLEXCHANGE_CLICK_REQUEST,
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
                KOLEXCHANGE_UPDATE_REQUEST,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // Use alarm manager to schedule widget update
    private void scheduleUpdate(Context context) {

        if (getActiveWidgetIds(context).length > 0) {

            // Set inexact alarm for next update
            ZonedDateTime nextUpdate = ZonedDateTime.now().plus(KOLEXCHANGE_UPDATE_DURATION);
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

                // Load exchange rate from web service, with retries
                XMLParser parser = new XMLParser();
                int retries = KOLEXCHANGE_RETRIES;
                while (updateText == null && retries > 0) {
                    try {

                        // Fetch XML data
                        String xml = parser.getXmlFromUrl(KOLEXCHANGE_WS_URL, KOLEXCHANGE_TIMEOUT);
                        if (xml != null) {
                            Document doc = parser.getDomElement(xml);
                            if (doc != null) {
                                NodeList nl = doc.getElementsByTagName(KOLEXCHANGE_WS_NODE);
                                if (nl.getLength() > 0) {
                                    // New value for widget text
                                    updateText = KOLEXCHANGE_LABEL +
                                            parser.getElementValue(nl.item(0));
                                }
                            }
                        }
                        if (updateText == null) {
                            // Pause before retrying
                            TimeUnit.MILLISECONDS.sleep(KOLEXCHANGE_TIMEOUT);
                        }

                    } catch (Exception e) {
                        Log.e("doWidgetUpdate", e.getMessage());
                    }

                    retries--;
                }
                Log.i("doWidgetUpdate", "Got " + updateText);

                // After loading data, apply updates to the widget
                handler.post(() -> {

                    // Update the widget text only if a value was received
                    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
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

