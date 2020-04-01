package fr.tabbya.instantvisio;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.functions.FirebaseFunctions;
import com.tbruyelle.rxpermissions2.RxPermissions;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

/**
 * Copyright (c) 2019 - Stéphane Luçon 20/03/2020
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private static final String SMS_SENT = "SMS_SENT";
    public static final String VISIO_URL_EXTRA = "visioUrl";
    public static final String VISIO_INVITE_PHONE_NUMBER = "VISIO_INVITE_PHONE_NUMBER";
    public static final String VISIO_INVITE_SMS_BODY = "VISIO_INVITE_SMS_BODY";

    private static boolean mIsSimSupported;
    private Button button;
    private TextView phoneTitle;
    private EditText nameField;
    private EditText phoneField;
    private EditText emailField;
    private FirebaseFunctions mFunctions;
    private FirebaseService mFirebaseService;
    private RxPermissions rxPermissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFunctions = FirebaseFunctions.getInstance();
        mFirebaseService = new FirebaseService(mFunctions);
        rxPermissions = new RxPermissions(this);

        SharedPreferencesManager.initializePreferences(MainActivity.this);
        if (!SharedPreferencesManager.getDisclaimerDone()) {
            Intent newActivity = new Intent(MainActivity.this, Disclaimer.class);
            newActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newActivity.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            newActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(newActivity);
        }

        setContentView(R.layout.main_activity);

        nameField = findViewById(R.id.name_edit);
        phoneField = findViewById(R.id.phone_edit);
        emailField = findViewById(R.id.email_edit);
        phoneTitle = findViewById(R.id.phone_title);

        mIsSimSupported = isSimSupported();
        if (!mIsSimSupported) {
            /** we hide SMS sending option on non SIM device */
            phoneTitle.setVisibility(View.INVISIBLE);
            phoneField.setVisibility(View.INVISIBLE);
        }

        button = findViewById(R.id.button);
        button.setOnClickListener(v -> launchVisio());
    }

    public void launchVisio() {
        if (!hasSms() && !hasEmail()) {
            Toast.makeText(MainActivity.this, R.string.toast_missing_data, Toast.LENGTH_SHORT).show();
        } else {
            String phone = getFieldValue(phoneField);
            String email = getFieldValue(emailField);
            String name = getFieldValue(nameField);

            askPermissions()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(granted -> {
                    Log.d("MainActivity: ", "Permissions request: " + (granted ? "granted" : "denied"));
                    return granted ? Single.just(true) : Single.error(new Throwable("Permission missing"));
                })
                .flatMap(granted -> {
//                    return Single.just("https://www.google.com");
                    return mFirebaseService.getVisioUrl(name, phone, email);
                })
                .subscribe(visionUrl -> {
                    String message = getMessageToSend(visionUrl);
                    Log.d(TAG, "sms receiver phone number: " + phone);
                    Log.d(TAG, "message to send : " + message);
                    Log.d("VISION_URL", visionUrl);

                    if(hasSms()) inviteUserAndOpenVisio(phone, message, visionUrl);
                    else {
                        showToastMessage(R.string.toast_launching);
                        openVisionOnWebview(visionUrl);
                    }
                }, error -> {
                    Log.d("MainActivity", "Permissions denied: " + error);
                });
        }
    }

    public Single<Boolean> askPermissions() {
        return Single.fromObservable(rxPermissions
            .request(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.SEND_SMS
            ));
    }

    public String getFieldValue(EditText textField) {
        return textField.getText().toString();
    }

    public String getMessageToSend(String url) {
        String message = getString(R.string.message_beginning);
        String person = "";
        if (!String.valueOf(nameField.getText()).equals(""))
            person = getString(R.string.has_name) + " " + nameField.getText();

        message = message + person + getString(R.string.message_end) + url;
        return message;
    }

    public void openVisionOnBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    public void openVisionOnWebview(String visioUrl) {
        Intent videoCallActivityIntent = new Intent(this, VideoCallActivity.class);
        Bundle params = new Bundle();
        params.putString(VISIO_URL_EXTRA, visioUrl);
        videoCallActivityIntent.putExtras(params);
        startActivity(videoCallActivityIntent);
    }

    public void inviteUserAndOpenVisio(String phoneNumber, String message, String visioUrl) {
        waitForSmsInviteToBeSent(phoneNumber, message, visioUrl);
    }

    public boolean hasSms() {
        return !String.valueOf(phoneField.getText()).equals("");
    }

    public boolean hasEmail() {
        return !String.valueOf(emailField.getText()).equals("");
    }

    public void waitForSmsInviteToBeSent(String phoneNumber, String message, String visioUrl) {
        SmsManager smsManager = SmsManager.getDefault();
        PendingIntent sentPI = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(SMS_SENT), PendingIntent.FLAG_UPDATE_CURRENT);
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        showToastMessage(R.string.toast_invite_and_launch_visio);
                        openVisionOnWebview(visioUrl);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Log.d("[SMS-SEND]", "No service");
                        showToastMessage("No service");
                        break;
                    default:
                        Log.d("[SMS-SEND]", "Generic sms error");
                        showToastMessage("Sms could not be sent");
                        break;
                }
            }
        }, new IntentFilter(SMS_SENT));
        smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null);
    }

    public void showToastMessage(String message) {
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void showToastMessage(int messageId) {
        String message = getResources().getString(messageId);
        Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
    }

    public boolean isSimSupported() {
        /** we need to check if we have a SIM device or not (tablet/phone without a SIM)*/
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        Log.d(TAG, "SIM_STATE CHECK : state int is " + simState);
        boolean isSimSupported;
        switch (simState) {
            case TelephonyManager.SIM_STATE_NOT_READY:
                isSimSupported = false;
                Log.d(TAG, "SIM_STATE_NOT_READY -> notSimDevice is " + isSimSupported);
                // value is 6, this is the one actually returned on a wifi tab...
                break;
            case TelephonyManager.SIM_STATE_ABSENT:
                isSimSupported = false;
                Log.d(TAG, "SIM_STATE_ABSENT -> notSimDevice is " + isSimSupported);
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                isSimSupported = false;
                Log.d(TAG, "SIM_STATE_NETWORK_LOCKED -> notSimDevice is " + isSimSupported);
                break;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                isSimSupported = false;
                Log.d(TAG, "SIM_STATE_PIN_REQUIRED -> notSimDevice is " + isSimSupported);

                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                isSimSupported = false;
                Log.d(TAG, "SIM_STATE_PUK_REQUIRED -> notSimDevice is " + isSimSupported);
                break;
            case TelephonyManager.SIM_STATE_READY:
                isSimSupported = true;
                Log.d(TAG, "SIM_STATE_READY -> notSimDevice is " + isSimSupported);
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                isSimSupported = false;
                Log.d(TAG, "SIM_STATE_UNKNOWN -> notSimDevice is " + isSimSupported);
                break;
            default:
                isSimSupported = true;
        }

        return isSimSupported;
    }
}
