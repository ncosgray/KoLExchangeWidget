package com.nathanatos.kolexchangewidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.RemoteViews;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.concurrent.TimeUnit;

public class KoLExchangeWidget extends AppWidgetProvider {

	private static final String KOLEXCHANGE_WS_URL = "https://www.nathanatos.com/kol/ws_getrate.php";
	private static final String KOLEXCHANGE_WS_NODE = "rate";
	private static final String KOLEXCHANGE_LABEL = "$1 US = ";
	private static final String KOLEXCHANGE_CLICK_URL = "https://www.nathanatos.com/kol-exchange-rate/";
	private static final String KOLEXCHANGE_CLICK = "KoLWidgetClicked";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		
		// set up the click intent
		ComponentName thisWidget = new ComponentName(context, KoLExchangeWidget.class);
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.main);
		Intent intent = new Intent(context, KoLExchangeWidget.class);
		intent.setAction(KOLEXCHANGE_CLICK);
		views.setOnClickPendingIntent(R.id.widget_rootview,
				PendingIntent.getBroadcast(context,
						0,
						intent,
						Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_CANCEL_CURRENT));
		appWidgetManager.updateAppWidget(thisWidget, views);
		
		// do the data update in another thread
		new WidgetUpdateTask().execute(context);
		
	}

	@Override
	public void onReceive(Context context, Intent intent) {
	    super.onReceive(context, intent);

	    // process a user click on the widget
	    if (intent.getAction() != null && intent.getAction().equals(KOLEXCHANGE_CLICK)) {
	        try {
	            Intent webIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(KOLEXCHANGE_CLICK_URL));
	            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            context.startActivity(webIntent);
	        } catch (RuntimeException e) {
	            e.printStackTrace();
	        }
	        
			// do an extra data update in another thread
			new WidgetUpdateTask().execute(context);
			
		}
	}
	
	private class WidgetUpdateTask extends AsyncTask<Context, Void, String> {
		
		private Context context;
		
		protected String doInBackground(Context... params) {

			// get context
			context = params[0];
			
			// load data from web service, with up to 3 retries
			String updateText = null;
	    	XMLParser parser = new XMLParser();
			int retries = 3;
			while (updateText == null && retries > 0){
				try {
					String xml = parser.getXmlFromUrl(KOLEXCHANGE_WS_URL);
					if (xml != null) {
						Document doc = parser.getDomElement(xml);
						if (doc != null) {
							NodeList nl = doc.getElementsByTagName(KOLEXCHANGE_WS_NODE);
							if (nl.getLength() > 0) {
								updateText = KOLEXCHANGE_LABEL + parser.getElementValue(nl.item(0));
							}
						}
					}
					if (updateText == null) {
						// pause before retrying
						TimeUnit.SECONDS.sleep(1);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				retries--;
			}
			return updateText;
			
	    }

	    protected void onPostExecute(String resultText) {
	    	
			// update the widget text only if a value was received
	    	if(resultText != null) {
	    		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	    		RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.main);
	    		ComponentName thisWidget = new ComponentName(context, KoLExchangeWidget.class);
	    		updateViews.setTextViewText(R.id.widget_textview, resultText);
	    		appWidgetManager.updateAppWidget(thisWidget, updateViews);
	    	}
	    
	    }

	}
}

