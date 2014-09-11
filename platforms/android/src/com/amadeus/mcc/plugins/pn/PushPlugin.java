package com.amadeus.mcc.plugins.pn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * 
 * @author WJOSE
 * This class echoes a string called from JavaScript.
 *
 */
public class PushPlugin extends CordovaPlugin {

    public static final String TAG = "PushPlugin";
    public static final String INIT = "init";
    public static final String REGISTER = "register";
    public static final String UNREGISTER = "unregister";
    public static final String EXIT = "exit";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_CALLBACK = "callback";
    
    public static List<Bundle> CACHED_EXTRAS = new ArrayList<Bundle>();
    
    private static CordovaWebView WEBVIEW;
    private static String CALLBACK;
    private static String SENDER_ID;
    private static boolean FOREGROUND = false;

    private GoogleCloudMessaging gcm;
    private Context context;
    private Activity activity;
    private String regid;
    
    
    /**
     * Gets the application context from cordova's main activity.
     * @return the application context
     */
    private Context getApplicationContext() {
        return this.cordova.getActivity().getApplicationContext();
    }
    
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        boolean result = false;
        Log.v(TAG, "execute: action=" + action);
        
        if (INIT.equals(action)) {
        	
        	WEBVIEW = this.webView; //In specific scenarios, WEBVIEW becomes null
        	if ( CACHED_EXTRAS.size()>0) {
                Log.v(TAG, "sending cached extras");
                callbackContext.success("Successfully initialized and sending cached noitifications");
                sendExtras(CACHED_EXTRAS);
                CACHED_EXTRAS = new ArrayList<Bundle>();          
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    	        nm.cancel(GcmIntentService.NOTIFICATION_ID);
            } else {
            	callbackContext.success("Successfully initialized");
            }
        }
        else if (REGISTER.equals(action)) {

            try {
                JSONObject jo = data.getJSONObject(0);

                Log.v(TAG, "execute: json=" + jo.toString());

                CALLBACK = (String) jo.get("ecb");
                SENDER_ID = (String) jo.get("senderID");

                storeCallback(context, CALLBACK);
                
                Log.v(TAG, "execute: ECB=" + CALLBACK + " senderID=" + SENDER_ID);

                if (checkPlayServices()) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                    regid = getStoredRegistrationId(context);
                    if (regid.isEmpty()) {
                        registerInBackground();
                        callbackContext.success("Registering the application");
                    } else {
                        callbackContext.success("Already registered");
                    }
                    result = true;                    
                } else {
                    Log.i(TAG, "No valid Google Play Services APK found.");
                }
            } catch (JSONException e) {
                Log.e(TAG, "execute: Got JSON Exception " + e.getMessage());
                result = false;
                callbackContext.error(e.getMessage());
            }

        } else if (UNREGISTER.equals(action)) {

            unregister();

            Log.v(TAG, "UNREGISTER");
            result = true;
            regid = getStoredRegistrationId(context);
            if (!regid.isEmpty()) {
            	callbackContext.success("Un-registering the application");
            } else {
            	callbackContext.success("Already Un-registered");
            }
        } else {
            result = false;
            Log.e(TAG, "Invalid action : " + action);
            callbackContext.error("Invalid action : " + action);
        }

        return result;
    }


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    	Log.v(TAG, "inside initialize");
        super.initialize(cordova, webView);
        this.context = getApplicationContext();
        this.activity = this.cordova.getActivity();
        WEBVIEW = this.webView;        
        FOREGROUND = true;
        CALLBACK = getStoredCallback(this.context);
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        FOREGROUND = false;
        final NotificationManager notificationManager = (NotificationManager) cordova.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    /**
     * This will be invoked when app comes back to foreground (dont confuse with initialize)
     * This will also push the notifications to the JavaScript when it was in background
     * 
     * @param multitasking
     */
    @Override
    public void onResume(boolean multitasking) {
    	Log.v(TAG, "inside onResume");
        super.onResume(multitasking);
        FOREGROUND = true;
        
        if ( CACHED_EXTRAS.size()>0) {
            Log.v(TAG, "sending cached extras");
            sendExtras(CACHED_EXTRAS);
            CACHED_EXTRAS = new ArrayList<Bundle>();                
	        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	        nm.cancel(GcmIntentService.NOTIFICATION_ID);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FOREGROUND = false;
        CALLBACK = null;
        WEBVIEW = null;
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this.activity,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                this.activity.finish();
            }
            return false;
        }
        return true;
    }
    

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getStoredRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }
    
    /**
     * Fetch the stored callback string and return 
     *  
     * @param context
     * @return
     */
    private String getStoredCallback(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String callback = prefs.getString(PROPERTY_CALLBACK, "");
        if (callback.isEmpty()) {
            Log.i(TAG, "Callback not found.");
            return "";
        }
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return callback;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        return this.activity.getSharedPreferences(this.activity.getClass().getSimpleName(),
                Context.MODE_PRIVATE);
    }
    

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
    

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    Log.i(TAG, "Device registered, registration ID="+ regid);
                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                    msg = regid;
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                JSONObject json = new JSONObject();
                try {
    				if(msg.startsWith("Error")) {
                		json.put("error", msg);					            		
	                } else {
	                	json.put("event", "register");
	                	json.put("regid", msg);
	                }
                } catch (JSONException e) {
					e.printStackTrace();
				}
                sendJavascript(json);
            }
        }.execute(null, null, null);
    }


    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
    
    /**
     * Stores the callback parameter in shared preferences so that next time when app is visited, 
     * Plugin can send notifications to client.
     * 
     * @param context
     * @param callback
     */
    private void storeCallback(Context context, String callback) {
        final SharedPreferences prefs = getGcmPreferences(context);
        Log.i(TAG, "Saving callback in shared preferences");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_CALLBACK, callback);
        editor.commit();
    }
    
    /**
     * Sends the pushbundle extras to the client application.
     * If the client application isn't currently active, it is cached for later processing.
     */
    public static void sendExtras(Bundle extras) {
    	Log.v(TAG, "sendExtras :"+extras);
    	if (extras != null) {
        	if (CALLBACK != null && WEBVIEW != null) {
        		sendJavascript(convertBundleToJson(extras));
            } else {
                Log.v(TAG, "sendExtras: caching extras to send at a later time.");
                CACHED_EXTRAS.add(extras);
            }
        }
    }
    
    
    /**
     * Sends the List of pushbundle extras to the client application.
     * If the client application isn't currently active, it is cached for later processing.
     */
    public static void sendExtras(List<Bundle> extrasList) {
    	Log.v(TAG, "sendExtras List :"+extrasList.size());
    	if (extrasList.size()>0) {
        	for(Bundle extras : extrasList) {
        		sendExtras(extras);
        	}
        }
    }

    /**
     * Sends a json object to the client as parameter to a method which is defined in gECB.
     */
    public static void sendJavascript(JSONObject _json) {
        String _d = "javascript:" + CALLBACK + "(" + _json.toString() + ")";
        Log.v(TAG, "sendJavascript: " + _d);

        if (CALLBACK != null && WEBVIEW != null) {
            WEBVIEW.sendJavascript(_d);
        }
    }

	/**
	 * serializes a bundle to JSON.
	 */
    private static JSONObject convertBundleToJson(Bundle extras) {
        try {
            JSONObject json;
            json = new JSONObject().put("event", "message");

            JSONObject jsondata = new JSONObject();
            Iterator<String> it = extras.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = extras.get(key);

                // System data from Android
                if (key.equals("from") || key.equals("collapse_key")) {
                    json.put(key, value);
                }
                else if (key.equals("foreground")) {
                    json.put(key, extras.getBoolean("foreground"));
                } else if (key.equals("coldstart")) {
                    json.put(key, extras.getBoolean("coldstart"));
                } else {
                    // Maintain backwards compatibility
                    if (key.equals("message") || key.equals("msgcnt") || key.equals("soundname")) {
                        json.put(key, value);
                    }

                    if ( value instanceof String ) {
                    // Try to figure out if the value is another JSON object

                        String strValue = (String)value;
                        if (strValue.startsWith("{")) {
                            try {
                                JSONObject json2 = new JSONObject(strValue);
                                jsondata.put(key, json2);
                            } catch (Exception e) {
                                jsondata.put(key, value);
                            }
                            // Try to figure out if the value is another JSON array
                        } else if (strValue.startsWith("[")) {
                            try {
                                JSONArray json2 = new JSONArray(strValue);
                                jsondata.put(key, json2);
                            } catch (Exception e) {
                                jsondata.put(key, value);
                            }
                        } else {
                            jsondata.put(key, value);
                        }
                    }
                }
            } // while
            json.put("payload", jsondata);

            Log.v(TAG, "extrasToJSON: " + json.toString());

            return json;
        } catch( JSONException e) {
            Log.e(TAG, "extrasToJSON: JSON exception");
        }
        return null;
    }

    public static boolean isInForeground(Context context) {
      return FOREGROUND;
    }
    
    public static boolean isInstatiated(){
    	return (WEBVIEW!=null);
    }    

	public static boolean isActive() {
        return WEBVIEW != null;
    }
    
    /**
     * Un-Registers the application with GCM servers asynchronously.
     * <p>
     * Removes the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void unregister() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    gcm.unregister();
                    // Persist the regID - no need to register again.
                    removeSharedPreferences();
                    msg = "successfully unregistered the device";
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
            	JSONObject json = new JSONObject();
                try {
    				if(msg.startsWith("Error")) {
                		json.put("error", msg);					            		
	                } else {
	                	json.put("event", "unregister");
	                	json.put("message", msg);
	                }
                } catch (JSONException e) {
					e.printStackTrace();
				}
                sendJavascript(json);
            }
        }.execute(null, null, null);
    }
    

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void removeSharedPreferences() {
        final SharedPreferences prefs = getGcmPreferences(context);
        Log.i(TAG, "Clearing the shared preferences");
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }
}
