package com.twilio.voice.quickstart;


import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    private Spinner spinnerGmtOffset;
    private String[] spinnerValuesGmtOffset;
    ArrayAdapter<String> adapter;
    ArrayAdapter<String> adapterValues;

    private Button updateButton;
    private EditText accountSid;
    private TextView showResults;

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // To return to MainActivity
        // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);

        // -----------------------
        // Send message form objects:
        updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(this);
        accountSid = (EditText)findViewById(R.id.accountSid);
        showResults = (TextView)findViewById(R.id.showResults);

        // showResults.setText("+ Settings started.");
        // Snackbar.make(swipeRefreshLayout, "+ Settings started.", Snackbar.LENGTH_LONG).show();

        accountCredentials = new AccountCredentials(this);
        accountSid.setText(accountCredentials.getAccountSid());

        // -----------------------
        // Set spinnerGmtOffset

        spinnerValuesGmtOffset = getResources().getStringArray(R.array.gmt_offset_values); // Arrary Values
        adapterValues = new ArrayAdapter<>(this, R.layout.gmt_offset_spinner_item_value, Arrays.asList(spinnerValuesGmtOffset));

        // For testing:
        // String[] spinnerLabels = new String[ 3 ];
        // spinnerLabels[0] = "1";
        // spinnerLabels[1] = "2";
        // spinnerLabels[2] = "3";
        //
        String[] spinnerLabels = getResources().getStringArray(R.array.gmt_offset_labels);
        //
        adapter = new ArrayAdapter<>(this, R.layout.gmt_offset_spinner_item, Arrays.asList(spinnerLabels));
        //
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGmtOffset = (Spinner)findViewById(R.id.spinnerGmtOffset);
        spinnerGmtOffset.setAdapter(adapter);

        // accountCredentials.getLocalTimeOffsetString();   // "-2" or "-7: PT: California"
        String theLabel = accountCredentials.getLocalTimeOffsetString();
        int is = theLabel.indexOf(":");
        if (is > 0) {
            theLabel = theLabel.substring(0, is+1);
        }
        int thePosition = adapterValues.getPosition( theLabel );
        // showResults.setText("+ theLabel :" + theLabel + ": " + thePosition );
        if (thePosition >= 0) {
            spinnerGmtOffset.setSelection( thePosition );
        } else {
            spinnerGmtOffset.setSelection( 0 ); // default initialization
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Adds 3-dot option menu in the action bar.
        // getMenuInflater().inflate(R.menu.menu_sendsms, menu);

        // loadSpinnerAccPhoneNumbers();

        return true;
    }

    @Override
    public void onClick(View view) {

        // String accountSid = accountSid.getText();
        // Snackbar.make(swipeRefreshLayout, "+ Update clicked.", Snackbar.LENGTH_LONG).show();

        switch (view.getId()) {
            case R.id.updateButton:
                showResults.setText("");

                // hide keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(accountSid.getWindowToken(), 0);

                try {
                    String theRequirementsMessage = "";
                    String theValue = accountSid.getText().toString();
                    if (theValue.length()!=34) {
                        theRequirementsMessage = theRequirementsMessage + "\n++ " + getString(R.string.labelSid) + " length must be 34, but is " + theValue.length();
                    }
                    if (!theRequirementsMessage.isEmpty()) {
                        showResults.setText("+ Update: "  + theRequirementsMessage);
                        return;
                    }
                    accountCredentials.setAccountSid( accountSid.getText().toString() );
                    accountCredentials.setCredentials();
                    //
                    // showResults.setText("+ spinnerGmtOffset position: " + spinnerGmtOffset.getSelectedItemPosition()
                    //         + " adapterValues: " + spinnerValuesGmtOffset[ spinnerGmtOffset.getSelectedItemPosition() ]);
                    accountCredentials.setLocalTimeOffset( spinnerValuesGmtOffset[ spinnerGmtOffset.getSelectedItemPosition() ] );

                    Snackbar.make(swipeRefreshLayout, "+ Settings updated.", Snackbar.LENGTH_LONG).show();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    }

    // ---------------------------------------------------------------------------------------------
    private String currentLocalTime() {
        //                                                        :Tue, 26 Sep 2017 00:49:31 +0000: format for accountCredentials.localDateTime
        SimpleDateFormat readDateFormatter = new SimpleDateFormat("     dd MMM yyyy HH:mm:ss      ");
        readDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String currentGmtTime = readDateFormatter.format(new Date())+"";
        return accountCredentials.localDateTime( currentGmtTime ).trim();
    }

}