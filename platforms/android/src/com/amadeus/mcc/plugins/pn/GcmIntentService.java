package com.amadeus.mcc.plugins.pn;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * 
 * @author WJOSE
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 *
 */
public class GcmIntentService extends IntentService {
    
	public static final String TAG = "PushPlugin:GcmIntentService";
	public static int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    
    public GcmIntentService() {
        super("GcmIntentService");
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            	extras.putString("message", "Message type is Error"+extras.toString());
                sendNotification(extras);
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            	extras.putString("message", "Deleted messages on server: "+ extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            	boolean isForeGround = PushPlugin.isInForeground(this);
            	extras.putBoolean("foreground", isForeGround);
            	if (isForeGround) {
            		PushPlugin.sendExtras(extras);
            	} else {
            		/*
                	 *  Display message in notification tray when app is not in foreground. 
                	 */
                    if (extras.getString("title") != null && extras.getString("title").length() != 0) {
                    	PushPlugin.CACHED_EXTRAS.add(extras);
                    	sendNotification(extras);
                    }
                    	
            	}
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    
    /*
     *  Put the message into a notification and post it.
     *  This is just one simple example of what you might choose to do with
     *  a GCM message.
     */
	protected void sendNotification(Bundle extras) {
		Log.i("Start", "sendNotification");
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	
	   /*
	    * Intent resultIntent = new Intent(this, NotificationView.class);
	    * TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
	    * stackBuilder.addParentStack(NotificationView.class);
	    * stackBuilder.addNextIntent(resultIntent);
	    * PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
	    * mBuilder.setContentIntent(resultPendingIntent);
	    */
		Intent notificationIntent = new Intent(this, PushHandlerActivity.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("pushBundle", extras);		
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		/* Invoking the default notification service */
		NotificationCompat.Builder  mBuilder = new NotificationCompat.Builder(this);	
		
		//Change the logic to print message
		int defaults = Notification.DEFAULT_ALL;
		if (extras.getString("defaults") != null) {
			try {
				defaults = Integer.parseInt(extras.getString("defaults"));
			} catch (NumberFormatException e) {		
				Log.e(TAG, "Number format exception - Error parsing defaults: " + e.getMessage());
			}
		}
		mBuilder.setDefaults(defaults)
	   			.setContentTitle(extras.getString("title"))
	   			.setTicker(extras.getString("ticker"))
	   			.setSmallIcon(this.getApplicationInfo().icon)
	   			.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setContentIntent(contentIntent);
	   
		String message = extras.getString("content");
		if (message != null) {
			mBuilder.setContentText(message);
		} else {
			mBuilder.setContentText("<missing message content>");
		}
		String msgcnt = extras.getString("msgcnt");
		if (msgcnt != null) {
			mBuilder.setNumber(Integer.parseInt(msgcnt));
		}
		//do changes till here
		
		try {
			NOTIFICATION_ID = Integer.parseInt(extras.getString("notId"));
		} catch(NumberFormatException e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID: " + e.getMessage());
		} catch(Exception e) {
			Log.e(TAG, "Number format exception - Error parsing Notification ID" + e.getMessage());
		}
		String appName = getAppName(this);		
		mNotificationManager.notify((String) appName, NOTIFICATION_ID, mBuilder.build());
	}
	
	
	protected void cancelNotification() {
	   Log.i("Cancel", "notification");
	   mNotificationManager.cancel(NOTIFICATION_ID);
	}
	
	private static String getAppName(Context context) {
		CharSequence appName =  context .getPackageManager() .getApplicationLabel(context.getApplicationInfo());		
		return (String)appName;
	}
	
}
