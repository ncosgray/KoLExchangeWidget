/*
 *******************************************************************************
 Package:  com.nathanatos.kolexchangewidget
 Class:    XMLParser.java
 Author:   Nathan Cosgray | https://www.nathanatos.com
 -------------------------------------------------------------------------------
 Copyright (c) 2013-2024 Nathan Cosgray. All rights reserved.
 This source code is licensed under the BSD-style license found in LICENSE.txt.
 *******************************************************************************
*/

// XMLParser class
// - Load XML from a web address
// - Parse XML to get value from requested node

package com.nathanatos.kolexchangewidget;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class XMLParser {

    // Cribbed from:
    // http://www.androidhive.info/2011/11/android-xml-parsing-tutorial/
    // and http://androidcookbook.com/Recipe.seam?recipeId=79

    // Load XML from web address into a string
    public String getXmlFromUrl(String xmlUrl, int timeoutSeconds) {

        String xml = null;

        try {
            // Use URLConnection to fetch data
            URL url = new URL(xmlUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutSeconds);
            conn.setReadTimeout(timeoutSeconds);
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
            xml = sb.toString();

        } catch (IOException e) {
            Log.e("getXmlFromUrl", e.getMessage());
        }
        return xml;

    }

    // Get DOM from XML string
    public Document getDomElement(String xml) {

        Document doc;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {

            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);

        } catch (ParserConfigurationException | SAXException | IOException e) {
            Log.e("getDomElement", e.getMessage());
            return null;
        }
        return doc;

    }

    // Get value from a node
    public final String getElementValue(Node elem) {

        Node child;

        if (elem != null) {
            if (elem.hasChildNodes()) {
                for (child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        return child.getNodeValue();
                    }
                }
            }
        }
        return "";

    }

}
