package com.tigerfarmpress.voice.owlcall;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;
import com.twilio.voice.RegistrationException;
import com.twilio.voice.RegistrationListener;
import com.twilio.voice.Voice;
import com.twilio.voice.owlcall.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VoiceActivity extends AppCompatActivity {

    private static final String TAG = "VoiceActivity";

    private static final int SNACKBAR_DURATION = 4000;

    private static final int MIC_PERMISSION_REQUEST_CODE = 1;
    private AudioManager audioManager;
    private int savedAudioMode = AudioManager.MODE_INVALID;
    private boolean isReceiverRegistered = false;
    private VoiceBroadcastReceiver voiceBroadcastReceiver;

    private static String TWILIO_ACCESS_TOKEN = "";
    HashMap<String, String> twiMLParams = new HashMap<>();

    private CoordinatorLayout coordinatorLayout;
    private FloatingActionButton callActionFab, callActionRefresh, hangupActionFab, speakerActionFab;
    private Chronometer chronometer;
    private SoundPoolManager soundPoolManager;

    public static final String INCOMING_CALL_INVITE = "INCOMING_CALL_INVITE";
    public static final String INCOMING_CALL_NOTIFICATION_ID = "INCOMING_CALL_NOTIFICATION_ID";
    public static final String ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL";
    public static final String ACTION_FCM_TOKEN = "ACTION_FCM_TOKEN";

    private NotificationManager notificationManager;
    private AlertDialog alertDialog;
    private CallInvite activeCallInvite;
    private Call activeCall;

    RegistrationListener registrationListener = registrationListener();
    Call.Listener callListener = callListener();

    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText formPhoneNumber;
    private static TextView labelContactName;
    // For contacts
    ListView listView ;
    ArrayList<String> StoreContacts ;
    ArrayAdapter<String> arrayAdapter ;
    Cursor cursor ;
    String name, phonenumber ;
    public static final int RequestPermissionCode = 2;

    private AccountCredentials accountCredentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);

        accountCredentials = new AccountCredentials(this);
        String theTokenUrl = accountCredentials.getTokenUrl();
        if (theTokenUrl.isEmpty()) {
            accountCredentials.setTokenUrl( "hello" );
            Snackbar.make(coordinatorLayout, "+ Token URL set to :" + accountCredentials.getTokenUrl() + ":", SNACKBAR_DURATION).show();
            // Intent intent = new Intent(this, SettingsActivity.class);
            // startActivity(intent);
        }

        // ---------------------------------------------------------------------------------------------
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        callActionFab = (FloatingActionButton) findViewById(R.id.call_action_fab);
        hangupActionFab = (FloatingActionButton) findViewById(R.id.hangup_action_fab);
        speakerActionFab = (FloatingActionButton) findViewById(R.id.speakerphone_action_fab);
        chronometer = (Chronometer) findViewById(R.id.chronometer);

        labelContactName = (TextView)findViewById(R.id.labelContactName);
        formPhoneNumber = (EditText)findViewById(R.id.formPhoneNumber);
        labelContactName.setText(accountCredentials.getToContactName());
        formPhoneNumber.setText(accountCredentials.getToPhoneNumber());

        // hide keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(formPhoneNumber.getWindowToken(), 0);

        callActionRefresh = (FloatingActionButton) findViewById(R.id.action_refresh);
        callActionRefresh.setOnClickListener(callActionRefreshClickListener());

        callActionFab.setOnClickListener(callActionFabClickListener());
        hangupActionFab.setOnClickListener(hangupActionFabClickListener());
        speakerActionFab.setOnClickListener(speakerphoneActionFabClickListener());

        // ---------------------------------------------------------------------------------------------
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        soundPoolManager = SoundPoolManager.getInstance(this);
        // Setup the broadcast receiver to be notified of FCM Token updates or incoming call invite in this Activity.
        voiceBroadcastReceiver = new VoiceBroadcastReceiver();

        registerReceiver();

        // Needed for setting/abandoning audio focus during a call
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        // Enable changing the volume using the up/down keys during a conversation
        setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        // ---------------------------------------------------------------------------------------------
        resetUI();

        // Displays a call dialog if the intent contains a call invite
        handleIncomingCallIntent(getIntent());

        // Ensure the microphone permission is enabled
        if (!checkPermissionForMicrophone()) {
            requestPermissionForMicrophone();
        } else {
            registerForCallInvites();
        }

        // ---------------------------------------------------------------------------------------------
        // Load Contacts into ListView.
        StoreContacts = new ArrayList<String>();
        listView = (ListView)findViewById(R.id.listview1);
        EnableContactPermission();
        LoadContacts();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // https://stackoverflow.com/questions/20032270/why-my-android-setonitemclicklistener-doesnt-work
            // This may fix: https://stackoverflow.com/questions/14332409/custom-listview-is-not-responding-to-the-click-event/14333069#14333069
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition     = position;
                String  itemValue    = (String) listView.getItemAtPosition(position);
                // Snackbar.make(coordinatorLayout, "+ Position: "+itemPosition+" : " +itemValue, Snackbar.LENGTH_LONG).show();
                // String toCall = itemValue.toString().trim();
                // Snackbar.make(coordinatorLayout, "+ "+toCall+" "+toCall.indexOf("+")+1+" "+toCall.length(), Snackbar.LENGTH_LONG).show();
                if (itemValue.lastIndexOf("+")<0){
                    // Example: "stacyhere : sip:stacyhere@sdt.sip.us1.twilio.com"
                    // Example: "stacyhere : client:stacyhere"
                    // Example: "stacyhere : conference:stacyhere"
                    formPhoneNumber.setText( itemValue.substring(itemValue.lastIndexOf(" : ")+" : ".length()+1, itemValue.trim().length()));
                    if ( itemValue.lastIndexOf(":") > 3) {
                        labelContactName.setText( itemValue.substring(0, itemValue.indexOf(":")));
                    }
                } else {
                    // Example: "David : Mobile + 12223331234" -> "David"
                    formPhoneNumber.setText( itemValue.substring(itemValue.lastIndexOf("+"), itemValue.trim().length()));
                    if ( itemValue.lastIndexOf(":") > 3) {
                        labelContactName.setText( itemValue.substring(0, itemValue.lastIndexOf(":")));
                    }
                }
            }
        });

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                LoadContacts();
                swipeRefreshLayout.setRefreshing(false);
            }
        });

    }
/*
        String number = "494498498";
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" +number));
        startActivity(intent);

    public Fragment getItem(int position) {
        // Java Code Examples for com.android.contacts.dialpad.DialpadFragment
        // return new DialpadFragment();
    }
*/
    private View.OnClickListener callActionRefreshClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBeforeLeaving("refresh");
                Snackbar.make(coordinatorLayout, "+ Reload contacts", Snackbar.LENGTH_LONG).show();
                LoadContacts();
            }
        };
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_voice, menu);

        LoadContacts();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        // if (id == R.id.menu_refresh) { return true; }

        if (!checkBeforeLeaving("")) {
            return true;
        }

        /*
        if (id == R.id.action_lookup) {
            doLookup();
            return true;
        } else
        */
        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------------------------------------
    public boolean checkBeforeLeaving(String type) {

        String callPhoneNumber = formPhoneNumber.getText().toString();
        String callContactName = labelContactName.getText().toString();
        if (!callPhoneNumber.isEmpty()) {
            accountCredentials.setToPhoneNumber(callPhoneNumber);
            accountCredentials.setToContactName(callContactName);
        }

        return true;
    }

    // ---------------------------------------------------------------------------------------------

    public void LoadContacts(){
        StoreContacts.clear();
        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);
        while (cursor.moveToNext()) {
            String theType = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));
            if (theType == null || theType.equalsIgnoreCase("com.google")) {
                // null is the value for the emulator.
                // Don't add WhatsApp contacts ("com.whatsapp") because it duplicates the phone number.
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phonenumber;
                String typeLabel = "";
                if (name.toLowerCase().startsWith("sip:")){
                    // sip:stacyhere@sdt.sip.us1.twilio.com
                    // stacyhere : sip:stacyhere@sdt.sip.us1.twilio.com
                    phonenumber = name;
                    name = name.substring("sip:".length(),name.indexOf("@"));
                } else if (name.toLowerCase().startsWith("client:")){
                    // client:stacyhere
                    // stacyhere : client:stacyhere
                    phonenumber = name;
                    phonenumber = name.substring("client:".length(),name.length());
                } else if (name.toLowerCase().startsWith("conference:")){
                    // conference:stacyhere
                    // conference : conference:stacyhere
                    phonenumber = name;
                    name = "Conference";
                } else {
                    phonenumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                    int phoneType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    switch (phoneType)
                    {
                        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                            typeLabel = "Home";
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                            typeLabel = "Mobile";
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                            typeLabel = "Work";
                            break;
                        case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
                            // For custom label, example: Work office or Work mobile
                            typeLabel = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL));
                            if (typeLabel.toLowerCase().contains("mobile")) {
                                typeLabel = "Mobile";
                            }
                            break;
                        default:
                            typeLabel = "phoneType+"+phoneType+":";
                    }
                }
                // StoreContacts.add(name + " : " + phonenumber + " : " + theType);
                StoreContacts.add(name + " : " + typeLabel + " " + phonenumber);
            }
        }
        cursor.close();
        Collections.sort(StoreContacts);
        // Simple:
        // arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, StoreContacts);
        // With more options:
        // arrayAdapter = new ArrayAdapter<String>( VoiceActivity.this, R.layout.list_item_contacts, R.id.row01, StoreContacts );
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, StoreContacts);

        listView.setAdapter(arrayAdapter);
    }

    public void EnableContactPermission(){
        if ( ActivityCompat.shouldShowRequestPermissionRationale( VoiceActivity.this, Manifest.permission.READ_CONTACTS) ) {
            Snackbar.make(coordinatorLayout, "+ CONTACTS permission allows us to Access CONTACTS app.", Snackbar.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions( VoiceActivity.this, new String[] {
                    Manifest.permission.READ_CONTACTS}, RequestPermissionCode );
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Access permissions

    private boolean checkPermissionForMicrophone() {
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return resultMic == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionForMicrophone() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            Snackbar.make(coordinatorLayout,
                    "Microphone permissions needed. Please allow in your application settings.",
                    SNACKBAR_DURATION).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    MIC_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // Check if microphone permissions are granted
        if (requestCode == MIC_PERMISSION_REQUEST_CODE && permissions.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(coordinatorLayout,
                        "Microphone permissions needed. Please allow in your application settings.",
                        SNACKBAR_DURATION).show();
            } else {
                registerForCallInvites();
            }
        }

        // Check if contacts access permissions are granted
        if (requestCode == RequestPermissionCode && permissions.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(coordinatorLayout, "+ Permission Canceled, your application cannot access CONTACTS.", Snackbar.LENGTH_LONG).show();
            } else {
                // Snackbar.make(coordinatorLayout, "+ Permission Granted, Now your application can access CONTACTS.", Snackbar.LENGTH_LONG).show();
            }
        }

    }

    // ---------------------------------------------------------------------------------------------
    private void getAccessToken() {
        // Snackbar.make(coordinatorLayout, "+ Get Access Token...", SNACKBAR_DURATION).show();
        OkHttpClient client = new OkHttpClient.Builder()
                .build();
        Request request = new Request.Builder()
                .url(accountCredentials.getTokenUrl())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Snackbar.make(coordinatorLayout, "- Error: Network failure, try again.", SNACKBAR_DURATION).show();
                call.cancel();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                final String jsonResponse = response.body().string();
                VoiceActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ( jsonResponse.contains("\"code\": 20003") || jsonResponse.contains("\"status\": 404") ) {
                            Snackbar.make(coordinatorLayout, "+ Logging into your Twilio account failed. Go to Settings.", Snackbar.LENGTH_LONG).show();
                        } else {
                            // Snackbar.make(coordinatorLayout, "+ Got the Access Token.", Snackbar.LENGTH_LONG).show();
                            //
                            // {"accesstoken":"eyJhb ... eLEQ8"}
                            // 01234567890123456789
                            TWILIO_ACCESS_TOKEN = jsonResponse.substring(16, jsonResponse.length()-2);
                            //
                            // Example call-to phone number or addresses:
                            // twiMLParams.put("To", "+12223331234");
                            // twiMLParams.put("To", "client:stacytest");
                            // twiMLParams.put("To", "sip:stacytest@owlvc.sip.us1.twilio.com");
                            String callPhoneNumber = formPhoneNumber.getText().toString();
                            twiMLParams.put("To", callPhoneNumber);
                            activeCall = Voice.call(VoiceActivity.this, TWILIO_ACCESS_TOKEN, twiMLParams, callListener);
                            setCallUI();
                            registerForCallInvites();
                        }
                    }
                });
            }
        });
    }

    private void getAccessTokenForReceiveCalls() {
        // Snackbar.make(coordinatorLayout, "+ Get Access Token...", SNACKBAR_DURATION).show();
        OkHttpClient client = new OkHttpClient.Builder()
                .build();
        Request request = new Request.Builder()
                .url(accountCredentials.getTokenUrl())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Snackbar.make(coordinatorLayout, "- Error: Network failure, try again.", SNACKBAR_DURATION).show();
                call.cancel();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                final String jsonResponse = response.body().string();
                VoiceActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if ( jsonResponse.contains("\"code\": 20003") || jsonResponse.contains("\"status\": 404") ) {
                            Snackbar.make(coordinatorLayout, "+ Logging into your Twilio account failed. Go to Settings.", Snackbar.LENGTH_LONG).show();
                        } else {
                            // Snackbar.make(coordinatorLayout, "+ Got the Access Token.", Snackbar.LENGTH_LONG).show();
                            //
                            // {"accesstoken":"eyJhb ... eLEQ8"}
                            // 01234567890123456789
                            TWILIO_ACCESS_TOKEN = jsonResponse.substring(16, jsonResponse.length()-2);
                            //
                            // Example call-to phone number or addresses:
                            // twiMLParams.put("To", "+12223331234");
                            // twiMLParams.put("To", "client:stacytest");
                            // twiMLParams.put("To", "sip:stacytest@owlvc.sip.us1.twilio.com");
                            String callPhoneNumber = formPhoneNumber.getText().toString();
                            twiMLParams.put("To", callPhoneNumber);
                            activeCall = Voice.call(VoiceActivity.this, TWILIO_ACCESS_TOKEN, twiMLParams, callListener);
                            setCallUI();
                            registerForCallInvites();
                        }
                    }
                });
            }
        });
    }    // ---------------------------------------------------------------------------------------------

    // The UI state when there is an active call
    private void setCallUI() {
        callActionFab.hide();
        hangupActionFab.show();
        speakerActionFab.show();
        chronometer.setVisibility(View.VISIBLE);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
    }

    // Reset UI elements
    private void resetUI() {
        if (!audioManager.isSpeakerphoneOn()) {
            toggleSpeakerPhone();
        }
        speakerActionFab.hide();
        callActionFab.show();
        hangupActionFab.hide();
        chronometer.setVisibility(View.INVISIBLE);
        chronometer.stop();
    }

    @Override
    public void onDestroy() {
        soundPoolManager.release();
        super.onDestroy();
    }

    // ---------------------------------------------------------------------------------------------

    private class VoiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_INCOMING_CALL)) {
                /*
                 * Handle the incoming call invite
                 */
                handleIncomingCallIntent(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver();
    }

    private void registerReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_INCOMING_CALL);
            intentFilter.addAction(ACTION_FCM_TOKEN);
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    voiceBroadcastReceiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(voiceBroadcastReceiver);
            isReceiverRegistered = false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Register your FCM token with Twilio to receive incoming call invites

    /*
     * If a valid google-services.json has not been provided or the FirebaseInstanceId has not been
     * initialized the fcmToken will be null.
     *
     * In the case where the FirebaseInstanceId has not yet been initialized the
     * VoiceFirebaseInstanceIDService.onTokenRefresh should result in a LocalBroadcast to this
     * activity which will attempt registerForCallInvites again.
     *
     */
    private void registerForCallInvites() {
        final String fcmToken = FirebaseInstanceId.getInstance().getToken();
        if (fcmToken != null) {
            Log.i(TAG, "Registering with FCM");
                Voice.register(this, TWILIO_ACCESS_TOKEN, Voice.RegistrationChannel.FCM, fcmToken, registrationListener);
        }
    }

    // ------------
    // Registration

    private RegistrationListener registrationListener() {
        return new RegistrationListener() {
            @Override
            public void onRegistered(String accessToken, String fcmToken) {
                Log.d(TAG, "Successfully registered FCM " + fcmToken);
            }

            @Override
            public void onError(RegistrationException error, String accessToken, String fcmToken) {
                String message = String.format("Registration Error: %d, %s", error.getErrorCode(), error.getMessage());
                Log.e(TAG, message);
                Snackbar.make(coordinatorLayout, message, SNACKBAR_DURATION).show();
            }
        };
    }

    private View.OnClickListener callActionFabClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(formPhoneNumber.getWindowToken(), 0);

                if (!formPhoneNumber.getText().toString().isEmpty()) {
                    Snackbar.make(coordinatorLayout, "+ Making a call...", SNACKBAR_DURATION).show();
                    getAccessToken();
                } else if (accountCredentials.getTokenUrl().endsWith("hello")) {
                        Snackbar.make(coordinatorLayout, "+ Making a test call...", SNACKBAR_DURATION).show();
                        getAccessToken();
                } else {
                    Snackbar.make(coordinatorLayout, "+ Enter or select a phone number to call.", SNACKBAR_DURATION).show();
                }
            }
        };
    }

    private View.OnClickListener hangupActionFabClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                soundPoolManager.playDisconnect();
                resetUI();
                disconnect();
            }
        };
    }

    private View.OnClickListener speakerphoneActionFabClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSpeakerPhone();
            }
        };
    }

    /*
     * Accept an incoming Call
     */
    private void answer() {
        activeCallInvite.accept(this, callListener);
    }

    /*
     * Disconnect from Call
     */
    private void disconnect() {
        if (activeCall != null) {
            activeCall.disconnect();
            activeCall = null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // handle Incoming Call

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIncomingCallIntent(intent);
    }

    private Call.Listener callListener() {
        return new Call.Listener() {
            @Override
            public void onConnectFailure(Call call, CallException error) {
                setAudioFocus(false);
                Log.d(TAG, "Connect failure");
                String message = String.format("Call Error: %d, %s", error.getErrorCode(), error.getMessage());
                Log.e(TAG, message);
                Snackbar.make(coordinatorLayout, message, SNACKBAR_DURATION).show();
                resetUI();
            }

            @Override
            public void onConnected(Call call) {
                setAudioFocus(true);
                Log.d(TAG, "Connected");
                activeCall = call;
            }

            @Override
            public void onDisconnected(Call call, CallException error) {
                setAudioFocus(false);
                Log.d(TAG, "Disconnected");
                if (error != null) {
                    String message = String.format("Call Error: %d, %s", error.getErrorCode(), error.getMessage());
                    Log.e(TAG, message);
                    Snackbar.make(coordinatorLayout, message, SNACKBAR_DURATION).show();
                }
                resetUI();
            }
        };
    }

    private void handleIncomingCallIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ACTION_INCOMING_CALL)) {
                activeCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
                if (activeCallInvite != null && (activeCallInvite.getState() == CallInvite.State.PENDING)) {
                    soundPoolManager.playRinging();
                    alertDialog = createIncomingCallDialog(VoiceActivity.this,
                            activeCallInvite,
                            answerCallClickListener(),
                            cancelCallClickListener());
                    alertDialog.show();
                    notificationManager.cancel(intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, 0));
                } else {
                    if (alertDialog != null && alertDialog.isShowing()) {
                        soundPoolManager.stopRinging();
                        alertDialog.cancel();
                    }
                }
            } else if (intent.getAction().equals(ACTION_FCM_TOKEN)) {
                registerForCallInvites();
            }
        }
    }

    // --------------------

    private DialogInterface.OnClickListener answerCallClickListener() {
        return new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                soundPoolManager.stopRinging();
                answer();
                setCallUI();
                alertDialog.dismiss();
            }
        };
    }

    private DialogInterface.OnClickListener cancelCallClickListener() {
        return new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                soundPoolManager.stopRinging();
                activeCallInvite.reject(VoiceActivity.this);
                alertDialog.dismiss();
            }
        };
    }

    public static AlertDialog createIncomingCallDialog(
            Context context,
            CallInvite callInvite,
            DialogInterface.OnClickListener answerCallClickListener,
            DialogInterface.OnClickListener cancelClickListener) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setIcon(R.drawable.ic_call_black_24dp);
        alertDialogBuilder.setTitle("Incoming Call");
        alertDialogBuilder.setPositiveButton("Accept", answerCallClickListener);
        alertDialogBuilder.setNegativeButton("Reject", cancelClickListener);
        alertDialogBuilder.setMessage(callInvite.getFrom() + " is calling.");
        return alertDialogBuilder.create();
    }

    // ---------------------------------------------------------------------------------------------
    private void toggleSpeakerPhone() {
        if (audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(false);
            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(VoiceActivity.this, R.drawable.ic_volume_mute_white_24px));
        } else {
            audioManager.setSpeakerphoneOn(true);
            speakerActionFab.setImageDrawable(ContextCompat.getDrawable(VoiceActivity.this, R.drawable.ic_volume_down_white_24px));
        }
    }

    private void setAudioFocus(boolean setFocus) {
        if (audioManager != null) {
            if (setFocus) {
                savedAudioMode = audioManager.getMode();
                // Request audio focus before making any device switch.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build();
                    AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .setAudioAttributes(playbackAttributes)
                            .setAcceptsDelayedFocusGain(true)
                            .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                                @Override
                                public void onAudioFocusChange(int i) { }
                            })
                            .build();
                    audioManager.requestAudioFocus(focusRequest);
                } else {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }
                /*
                 * Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
                 * required to be in this mode when playout and/or recording starts for
                 * best possible VoIP performance. Some devices have difficulties with speaker mode
                 * if this is not set.
                 */
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            } else {
                audioManager.setMode(savedAudioMode);
                audioManager.abandonAudioFocus(null);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
}
