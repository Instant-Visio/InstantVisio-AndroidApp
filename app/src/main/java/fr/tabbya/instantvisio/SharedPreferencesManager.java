package fr.tabbya.instantvisio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private static final String TAG = "SharedPrefMan";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor sharedPreferencesEditor;
    private static String VISIOURL = "VISIOURL";
    private static String MAILSERVICEURL = "MAILSERVICEURL";
    private static String DISCLAIMER_DONE = "DISCLAIMER_DONE";

    @SuppressLint("LongLogTag")
    public static void initializePreferences(Context context) {
        //      Log.d(TAG,"INITIALIZE PREFERENCES");
        sharedPreferences = context.getSharedPreferences("fr.tabbya.instantvisio.prefs", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    synchronized public static String getVisioUrl() {
        return sharedPreferences.getString(VISIOURL, "https://talk.vasanthv.com/");
    }

    synchronized public static void setVisioUrl(String VisioUrl) {
        sharedPreferencesEditor.putString(VISIOURL, VisioUrl);
        sharedPreferencesEditor.apply();
    }

    synchronized public static String getMailServiceUrl() {
        return sharedPreferences.getString(MAILSERVICEURL, "confidential link to be provided upon request to stephane@tabbya.fr until we change the implementation");
    }

    synchronized public static void setMailServiceUrl(String MailServiceUrl) {
        sharedPreferencesEditor.putString(MAILSERVICEURL, MailServiceUrl);
        sharedPreferencesEditor.apply();
    }

    synchronized public static boolean getDisclaimerDone() {
        return sharedPreferences.getBoolean(DISCLAIMER_DONE, false);
    }

    synchronized public static void setDisclaimerDone(Boolean disclaimerDone) {
        sharedPreferencesEditor.putBoolean(DISCLAIMER_DONE, disclaimerDone);
        sharedPreferencesEditor.apply();
    }
}
