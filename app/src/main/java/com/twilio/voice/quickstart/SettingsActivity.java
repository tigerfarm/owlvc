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
    ArrayAdapter<String> adapter;
    ArrayAdapter<String> adapterValues;

    private Button updateButton;
    private EditText tokenUrl;
    private TextView showResults;

    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);

        // -----------------------
        // Send message form objects:
        updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(this);
        tokenUrl = (EditText)findViewById(R.id.tokenUrl);
        showResults = (TextView)findViewById(R.id.showResults);

        accountCredentials = new AccountCredentials(this);
        String theTokenUrl = accountCredentials.getTokenUrl();
        if (!theTokenUrl.isEmpty()) {
            tokenUrl.setText(accountCredentials.getTokenUrl());
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        return true;
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.updateButton:
                showResults.setText("");
                // hide keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(tokenUrl.getWindowToken(), 0);
                try {
                    String theRequirementsMessage = "";
                    String theTokenUrl = tokenUrl.getText().toString();
                    if (!theTokenUrl.startsWith("https://")) {
                        theRequirementsMessage = theRequirementsMessage + "\n++ " + getString(R.string.labelTokenUrl) + " must start with: " + "https://";
                    }
                    if (!theRequirementsMessage.isEmpty()) {
                        showResults.setText("+ Update errors: "  + theRequirementsMessage);
                        return;
                    }
                    checkSetTokenUrl(theTokenUrl);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    }

    // ---------------------------------------------------------------------------------------------
    private void checkSetTokenUrl(String theTokenUrl) {
        // Snackbar.make(swipeRefreshLayout, "+ Get a sample access token...", Snackbar.LENGTH_LONG).show();
        OkHttpClient client = new OkHttpClient.Builder()
                .build();
        Request request = new Request.Builder()
                .url(theTokenUrl)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                String theMsg = "- Failed to connect to the access token host. Settings NOT updated.";
                // showResults.setText(theMsg);
                Snackbar.make(swipeRefreshLayout, theMsg, Snackbar.LENGTH_LONG).show();
                call.cancel();
            }
            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                final String jsonResponse = response.body().string();
                SettingsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String theMsg;
                        if ( jsonResponse.startsWith("{\"accesstoken\":") ) {
                            // {"accesstoken":"eyJhb ... eLEQ8"}
                            accountCredentials.setTokenUrl( tokenUrl.getText().toString() );
                            accountCredentials.setCredentials();
                            theMsg = "+ Got the access token. Settings updated.";
                        } else {
                            theMsg = "- Failed to get an access token. Settings NOT updated.";
                        }
                        showResults.setText(theMsg);
                        // Snackbar.make(swipeRefreshLayout, theMsg, Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
    // ---------------------------------------------------------------------------------------------
}