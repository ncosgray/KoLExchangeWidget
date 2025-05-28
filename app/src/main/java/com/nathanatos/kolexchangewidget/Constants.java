/*
 *******************************************************************************
 Package:  com.nathanatos.kolexchangewidget
 Class:    Constants.java
 Author:   Nathan Cosgray | https://www.nathanatos.com
 -------------------------------------------------------------------------------
 Copyright (c) 2013-2025 Nathan Cosgray. All rights reserved.
 This source code is licensed under the BSD-style license found in LICENSE.txt.
 *******************************************************************************
*/

// App constants

package com.nathanatos.kolexchangewidget;

import java.time.Duration;

public class Constants {

    // Exchange data constants
    public static final String KOLEXCHANGE_WS_URL = "https://4hea44d1a5.execute-api.us-east-1.amazonaws.com/getrate";
    public static final String KOLEXCHANGE_LABEL = "$1 US = ";
    public static final String KOLEXCHANGE_UNIT = " Meat";
    public static final String KOLEXCHANGE_GRAPH_URL = "https://kol-exchange-web.s3.amazonaws.com/rate_history_1mo.png";
    public static final int KOLEXCHANGE_TIMEOUT = 5000; // milliseconds
    public static final int KOLEXCHANGE_RETRIES = 3;

    // Main activity
    public static final String KOLEXCHANGE_CLICK_URL = "https://www.nathanatos.com/kol-exchange-rate/";

    // Widget configuration
    public static final String KOLEXCHANGE_CLICK_ACTION = "KoLWidgetClicked";
    public static final int KOLEXCHANGE_CLICK_REQUEST = 0;
    public static final Duration KOLEXCHANGE_UPDATE_DURATION = Duration.ofMinutes(60);
    public static final int KOLEXCHANGE_UPDATE_REQUEST = 1;

}
