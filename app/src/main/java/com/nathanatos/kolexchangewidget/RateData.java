/*
 *******************************************************************************
 Package:  com.nathanatos.kolexchangewidget
 Class:    RateData.java
 Author:   Nathan Cosgray | https://www.nathanatos.com
 -------------------------------------------------------------------------------
 Copyright (c) 2013-2025 Nathan Cosgray. All rights reserved.
 This source code is licensed under the BSD-style license found in LICENSE.txt.
 *******************************************************************************
*/

// Exchange rate data object

package com.nathanatos.kolexchangewidget;

import android.util.Log;

import androidx.annotation.NonNull;

import java.text.NumberFormat;

public class RateData {

    private final long mallPrice;
    private final long rate;
    private final long iotmId;
    private final String iotmName;
    private final boolean iotmIsFamiliar;
    private final String gameDate;
    private final String now;

    public RateData(long mallPrice, long rate, long iotmId, String iotmName,
                    boolean iotmIsFamiliar, String gameDate, String now) {
        this.mallPrice = mallPrice;
        this.rate = rate;
        this.iotmId = iotmId;
        this.iotmName = iotmName;
        this.iotmIsFamiliar = iotmIsFamiliar;
        this.gameDate = gameDate;
        this.now = now;
    }

    public long getMallPrice() {
        return mallPrice;
    }

    public long getRate() {
        return rate;
    }

    public String getFormattedRate() {

        final String logTag = "getFormattedRate";
        NumberFormat formatter = NumberFormat.getInstance();

        // Build a full exchange rate string including units and formatted rate
        try {
            formatter.setGroupingUsed(true);
            formatter.setMinimumFractionDigits(0);
            formatter.setMaximumFractionDigits(0);
            return Constants.KOLEXCHANGE_LABEL + formatter.format(rate) + Constants.KOLEXCHANGE_UNIT;
        }
        catch (NumberFormatException e) {
            Log.e(logTag, e.getMessage());
            return Long.toString(rate);
        }
    }

    public long getIotmId() {
        return iotmId;
    }

    public String getIotmName() {
        return iotmName;
    }

    public boolean isIotmFamiliar() {
        return iotmIsFamiliar;
    }

    public String getGameDate() {
        return gameDate;
    }

    public String getNow() {
        return now;
    }

    @NonNull
    @Override
    public String toString() {
        return "RateData{" +
                "mallPrice=" + mallPrice +
                ", rate=" + rate +
                ", iotmId=" + iotmId +
                ", iotmName='" + iotmName + '\'' +
                ", iotmIsFamiliar=" + iotmIsFamiliar +
                ", gameDate='" + gameDate + '\'' +
                ", now='" + now + '\'' +
                '}';
    }

}
