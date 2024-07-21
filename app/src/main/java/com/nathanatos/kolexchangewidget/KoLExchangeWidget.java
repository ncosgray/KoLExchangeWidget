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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KoLExchangeWidget extends AppWidgetProvider {

    // Widget configuration constants
    private static final String KOLEXCHANGE_WS_URL = "https://www.nathanatos.com/kol/ws_getrate.php";
    private static final int KOLEXCHANGE_WS_TIMEOUT = 2500;
    private static final String KOLEXCHANGE_WS_NODE = "rate";
    private static final String KOLEXCHANGE_LABEL = "$1 US = ";
    private static final String KOLEXCHANGE_CLICK_URL = "https://www.nathanatos.com/kol-exchange-rate/";
    private static final String KOLEXCHANGE_CLICK = "KoLWidgetClicked";
    private static final int KOLEXCHANGE_CLICK_NO = 0;
    private static final int KOLEXCHANGE_CLICK_YES = 1;

    // Update widget data
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // Update all widgets
        Log.i("onUpdate", "Starting all widgets update");
        for (int appWidgetId : appWidgetIds) {
            doWidgetUpdate(context, appWidgetManager, appWidgetId);
        }

    }

    // Handle widget click
    @Override
    public void onReceive(Context context, Intent intent) {

        super.onReceive(context, intent);

        if (intent != null) {

            // Get the intent details
            int didClick = KOLEXCHANGE_CLICK_NO;
            int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            Bundle extras = intent.getExtras();
            if (extras != null) {
                didClick = extras.getInt(KOLEXCHANGE_CLICK,
                        KOLEXCHANGE_CLICK_NO);
                appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
            }

            // Process a user click on the widget
            if (didClick == KOLEXCHANGE_CLICK_YES) {
                try {
                    // Open the KoL Exchange rate website in browser
                    Intent webIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(KOLEXCHANGE_CLICK_URL));
                    webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(webIntent);
                } catch (RuntimeException e) {
                    Log.e("onReceive", e.getMessage());
                }
            }

            // Update this widget
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                Log.i("onReceive", "Starting single widget update");
                doWidgetUpdate(context, AppWidgetManager.getInstance(context), appWidgetId);
            }
        }

    }

    // Asynchronously update the widget data and intent
    private void doWidgetUpdate(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {

            String updateText = null;

            // Load data from web service
            @Override
            public void run() {

                // Load exchange rate from web service, with up to 3 retries
                XMLParser parser = new XMLParser();
                int retries = 3;
                while (updateText == null && retries > 0) {
                    try {

                        String xml = parser.getXmlFromUrl(KOLEXCHANGE_WS_URL, KOLEXCHANGE_WS_TIMEOUT);
                        if (xml != null) {
                            Document doc = parser.getDomElement(xml);
                            if (doc != null) {
                                NodeList nl = doc.getElementsByTagName(KOLEXCHANGE_WS_NODE);
                                if (nl.getLength() > 0) {
                                    // New value for widget text
                                    updateText = KOLEXCHANGE_LABEL + parser.getElementValue(nl.item(0));
                                }
                            }
                        }
                        if (updateText == null) {
                            // Pause before retrying
                            TimeUnit.MILLISECONDS.sleep(KOLEXCHANGE_WS_TIMEOUT);
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
                    Intent intent = new Intent(context, KoLExchangeWidget.class);
                    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    intent.putExtra(KOLEXCHANGE_CLICK, KOLEXCHANGE_CLICK_YES);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {appWidgetId});
                    views.setOnClickPendingIntent(R.id.widget_rootview,
                            PendingIntent.getBroadcast(context,
                                    0,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

                    // Apply updates
                    appWidgetManager.updateAppWidget(appWidgetId, views);

                });
            }
        });

    }
}

