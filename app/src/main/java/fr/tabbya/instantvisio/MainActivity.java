package fr.tabbya.instantvisio;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Copyright (c) 2019 - Stéphane Luçon 20/03/2020
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class MainActivity extends Activity {
    private static String TAG = "MainActivity";
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 125;
    private static boolean simDevice;
    private Button button;
    private TextView phoneTitle;
    private EditText name;
    private EditText phone;
    private EditText email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferencesManager.initializePreferences(MainActivity.this);
        if (!SharedPreferencesManager.getDisclaimerDone()) {
            Intent newActivity = new Intent(MainActivity.this, Disclaimer.class);
            newActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            newActivity.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            newActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(newActivity);
        }

        setContentView(R.layout.main_activity);
        isSimSupport();

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            /** Permission SEND_SMS is not granted let's ask for it */
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        }

        name = findViewById(R.id.name_edit);
        phone = findViewById(R.id.phone_edit);
        email = findViewById(R.id.email_edit);
        phoneTitle = findViewById(R.id.phone_title);

        if (!simDevice) {
            /** we hide SMS sending option on non SIM device */
            phoneTitle.setVisibility(View.INVISIBLE);
            phone.setVisibility(View.INVISIBLE);
        }

        button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                /** Permission SEND_SMS is not granted let's ask for it */
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
            } else launchVisio();
        });
    }

    public void launchVisio() {
        long now = System.currentTimeMillis();
        String uniqueID = UUID.randomUUID().toString();
        String url = SharedPreferencesManager.getVisioUrl() /*+"/"*/ + now + uniqueID;

        String message = getString(R.string.message_beginning);
        String person = "";
        if (!String.valueOf(name.getText()).equals(""))
            person = getString(R.string.has_name) + " " + name.getText();

        message = message + person + getString(R.string.message_end) + url;

        Log.d(TAG, "message to send : " + message);

        /** check that we have something to send */
        if (String.valueOf(phone.getText()).equals("") && String.valueOf(email.getText()).equals("")) {
            Toast.makeText(MainActivity.this, R.string.toast_missing_data, Toast.LENGTH_SHORT).show();
        } else {
            /** send SMS */
            if (!String.valueOf(phone.getText()).equals("")) {

                SmsManager smsManager = SmsManager.getDefault();

                ArrayList<String> parts = smsManager.divideMessage(message);
                int messageCount = parts.size();

                Log.d("Message Count", "Message Count: " + messageCount);

                ArrayList<PendingIntent> deliveryIntents = new ArrayList<>();
                ArrayList<PendingIntent> sentIntents = new ArrayList<>();

                for (int i = 0; i < parts.size(); i++) {
                    PendingIntent sentPI = PendingIntent.getBroadcast(getApplicationContext(), i, new Intent("SMS_SENT"), i);
                    PendingIntent deliveredPI = PendingIntent.getBroadcast(getApplicationContext(), i, new Intent("SMS_DELIVERED"), i);

                    sentIntents.add(sentPI);
                    deliveryIntents.add(deliveredPI);
                }
                Log.d(TAG, "ready to sendMultipartMessage " + parts);

                smsManager.sendMultipartTextMessage(String.valueOf(phone.getText()), null, parts, sentIntents, deliveryIntents);
            }

            /** send email */
            if (!String.valueOf(email.getText()).equals("")) {
                sendMail(MainActivity.this, message);
            }

            /** then open URL*/
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        }
    }

    public void isSimSupport() {
        /** we need to check if we have a SIM device or not (tablet/phone without a SIM)*/
        TelephonyManager telMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        Log.d(TAG, "SIM_STATE CHECK : state int is " + simState);

        switch (simState) {
            case TelephonyManager.SIM_STATE_NOT_READY:
                simDevice = false;
                Log.d(TAG, "SIM_STATE_NOT_READY -> notSimDevice is " + simDevice);
                // value is 6, this is the one actually returned on a wifi tab...
                break;
            case TelephonyManager.SIM_STATE_ABSENT:
                simDevice = false;
                Log.d(TAG, "SIM_STATE_ABSENT -> notSimDevice is " + simDevice);
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                simDevice = false;
                Log.d(TAG, "SIM_STATE_NETWORK_LOCKED -> notSimDevice is " + simDevice);
                break;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                simDevice = false;
                Log.d(TAG, "SIM_STATE_PIN_REQUIRED -> notSimDevice is " + simDevice);

                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                simDevice = false;
                Log.d(TAG, "SIM_STATE_PUK_REQUIRED -> notSimDevice is " + simDevice);
                break;
            case TelephonyManager.SIM_STATE_READY:
                simDevice = true;
                Log.d(TAG, "SIM_STATE_READY -> notSimDevice is " + simDevice);
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                simDevice = false;
                Log.d(TAG, "SIM_STATE_UNKNOWN -> notSimDevice is " + simDevice);
                break;
        }
    }

    public void sendMail(final Context context, String message) {
        final String requestURL = SharedPreferencesManager.getMailServiceUrl();
        final JSONObject payload = new JSONObject();

        try {
            payload.put("name", "Demande URGENTE de visiophonie de votre proche");
            payload.put("mail", String.valueOf(email.getText()));
            payload.put("html", message);

            //TODO: check if this N/A breaks everything
            payload.put("uuid", "N/A");
        } catch (Exception ignored) {
        }

        Thread executionThread = new Thread(new Runnable() {
            String response;

            @Override
            public void run() {
                try {
                    final URL url = new URL(requestURL);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept", "application/json");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    Log.i(TAG, payload.toString());

                    DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                    dos.write(payload.toString().getBytes(StandardCharsets.UTF_8));

                    dos.flush();
                    dos.close();

                    Log.i(TAG, String.valueOf(conn.getResponseCode()));
                    response = conn.getResponseMessage();
                    Log.i(TAG, response);

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        executionThread.start();
    }
}