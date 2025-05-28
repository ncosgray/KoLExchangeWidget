/*
 *******************************************************************************
 Package:  com.nathanatos.kolexchangewidget
 Class:    KoLExchangeData.java
 Author:   Nathan Cosgray | https://www.nathanatos.com
 -------------------------------------------------------------------------------
 Copyright (c) 2013-2024 Nathan Cosgray. All rights reserved.
 This source code is licensed under the BSD-style license found in LICENSE.txt.
 *******************************************************************************
*/

// KoLExchangeData class
// - Do widget data updates asynchronously

package com.nathanatos.kolexchangewidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class KoLExchangeData {

    // Load exchange rate from web service, with retries
    public static RateData getExchangeRate() {

        final String logTag = "getExchangeRate";
        RateData rateData = null;

        // Load exchange rate from web service, with retries
        JsonParser parser = new JsonParser();
        int retries = Constants.KOLEXCHANGE_RETRIES;
        while (rateData == null && retries > 0) {
            try {

                // Fetch API data
                String json = parser.getApiData(Constants.KOLEXCHANGE_WS_URL, Constants.KOLEXCHANGE_TIMEOUT);
                if (json != null) {
                    rateData = JsonParser.parseApiData(json);
                }
                if (rateData == null) {
                    // Pause before retrying
                    TimeUnit.MILLISECONDS.sleep(Constants.KOLEXCHANGE_TIMEOUT);
                }

            } catch (Exception e) {
                Log.e(logTag, e.getMessage());
            }

            retries--;
        }
        Log.i(logTag, "Got " + rateData);

        return rateData;
    }

    // Download the exchange rate graph image, with retries
    public static Bitmap getExchangeGraph() {

        final String logTag = "getExchangeGraph";
        Bitmap graphImage = null;

        // Download the exchange rate graph image, with retries
        int retries = Constants.KOLEXCHANGE_RETRIES;
        while (graphImage == null && retries > 0) {
            try {
                InputStream in = new URL(Constants.KOLEXCHANGE_GRAPH_URL).openStream();
                graphImage = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e(logTag, e.getMessage());
            }

            retries--;
        }

        return graphImage;
    }

    // Return true if the device has an active network connection
    public static boolean isNetworkConnected(Context context) {

        // Get network info
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return false;
        }
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return false;
        }

        // Verify Wi-Fi or Cellular with internet access available
        return (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

    }
}
