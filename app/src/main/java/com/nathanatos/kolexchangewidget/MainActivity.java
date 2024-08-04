/*
 *******************************************************************************
 Package:  com.nathanatos.kolexchangewidget
 Class:    MainActivity.java
 Author:   Nathan Cosgray | https://www.nathanatos.com
 -------------------------------------------------------------------------------
 Copyright (c) 2013-2024 Nathan Cosgray. All rights reserved.
 This source code is licensed under the BSD-style license found in LICENSE.txt.
 *******************************************************************************
*/

// MainActivity class
// - Get current exchange rate and graph
// - Link to website
// - Widget pinning action

package com.nathanatos.kolexchangewidget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.nathanatos.kolexchangewidget.databinding.ActivityMainBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String logTag = "onCreate";

        // Set up binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        // Add click listeners to UI elements
        binding.contentActivityButtonLink.setOnClickListener(getWebClickListener());
        binding.fabActivityAction.setOnClickListener(getPinClickListener());

        // Download the exchange rate data
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // Load exchange rate and graph image and apply to layout
                String rateText = KoLExchangeData.getExchangeRate();
                Bitmap graphImage = KoLExchangeData.getExchangeGraph();

                handler.post(() -> {
                    binding.contentActivityTextviewRate.setText(rateText);
                    binding.contentActivityImageviewGraph.setImageBitmap(graphImage);
                });
            } catch (Exception e) {
                Log.e(logTag, e.getMessage());
            }
        });
    }

    // Create a listener for widget pinning action
    private View.OnClickListener getPinClickListener() {

        return view -> {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(MainActivity.this);
            ComponentName myProvider = new ComponentName(MainActivity.this, KoLExchangeWidget.class);

            // Ask user to pin the widget
            if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                appWidgetManager.requestPinAppWidget(myProvider, null, null);
            }
        };

    }

    // Create a listener for opening the website
    private View.OnClickListener getWebClickListener() {

        return view -> {
            // Open the KoL Exchange rate website in browser
            Intent webIntent = new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(Constants.KOLEXCHANGE_CLICK_URL));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(webIntent);
        };

    }
}