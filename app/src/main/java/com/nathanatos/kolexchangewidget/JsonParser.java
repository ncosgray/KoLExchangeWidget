/*
 *******************************************************************************
 Package:  com.nathanatos.kolexchangewidget
 Class:    JsonParser.java
 Author:   Nathan Cosgray | https://www.nathanatos.com
 -------------------------------------------------------------------------------
 Copyright (c) 2013-2025 Nathan Cosgray. All rights reserved.
 This source code is licensed under the BSD-style license found in LICENSE.txt.
 *******************************************************************************
*/

package com.nathanatos.kolexchangewidget;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class JsonParser {

    // Parse JSON data string into a RateData object
    public static RateData parseApiData(String jsonString) {

        final String logTag = "parseApiData";

        try {
            JSONObject jsonObject = new JSONObject(jsonString);

            // Populate RateData
            long mallPrice = jsonObject.getLong("mall_price");
            long rate = jsonObject.getLong("rate");
            long iotmId = jsonObject.getLong("iotm_id");
            String iotmName = jsonObject.getString("iotm_name");
            boolean iotmIsFamiliar = jsonObject.getBoolean("iotm_is_familiar");
            String gameDate = jsonObject.getString("game_date");
            String now = jsonObject.getString("now");

            return new RateData(mallPrice, rate, iotmId, iotmName, iotmIsFamiliar, gameDate, now);
        } catch (JSONException e) {
            Log.e(logTag, e.getMessage());
            return null;
        }
    }

    // Load raw data from API into a string
    public String getApiData(String apiUrl, int timeout) {

        final String logTag = "getApiData";
        String data = null;

        try {
            // Use URLConnection to fetch data
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            // Read into string
            StringBuilder sb = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            in.close();
            conn.disconnect();
            data = sb.toString();

        } catch (IOException e) {
            Log.e(logTag, e.getMessage());
        }
        return data;
    }

}