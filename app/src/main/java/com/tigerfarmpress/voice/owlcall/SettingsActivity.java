package com.tigerfarmpress.voice.owlcall;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.twilio.voice.owlcall.R;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private AccountCredentials accountCredentials;
    ArrayAdapter<String> adapter;
    ArrayAdapter<String> adapterValues;

    private Button updateButton;
    private EditText tokenUrl, clientId;
    private TextView showResults;

    private SwipeRefreshLayout swipeRefreshLayout;

    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);

        if (!checkPermissionForWriteStorage()) {
            Snackbar.make(swipeRefreshLayout, "+ Request Permission For Storage.", Snackbar.LENGTH_LONG).show();
            requestPermissionForStorage();
        }

        // -----------------------
        // Send message form objects:
        updateButton = (Button) findViewById(R.id.updateButton);
        updateButton.setOnClickListener(this);
        tokenUrl = (EditText)findViewById(R.id.tokenUrl);
        clientId = (EditText)findViewById(R.id.clientId);
        showResults = (TextView)findViewById(R.id.showResults);

        accountCredentials = new AccountCredentials(this);
        String theTokenUrl = accountCredentials.getTokenUrl();
        if (!theTokenUrl.isEmpty()) {
            tokenUrl.setText(theTokenUrl);
        }
        String theClientId = accountCredentials.getClientId();
        if (!theClientId.isEmpty()) {
            clientId.setText(theClientId);
        }
    }

    // ---------------------------------------------------------------------------------------------
    private boolean checkPermissionForWriteStorage() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    private void requestPermissionForStorage() {
        Snackbar.make(swipeRefreshLayout, "+ requestPermissionForStorage", Snackbar.LENGTH_LONG).show();
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Snackbar.make(swipeRefreshLayout, "External file storage permissions: please allow in your application settings.", Snackbar.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions( this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(swipeRefreshLayout, "+ Permission Canceled, your application cannot access STORAGE.", Snackbar.LENGTH_LONG).show();
            } else {
                // Snackbar.make(coordinatorLayout, "+ Permission Granted, Now your application can access CONTACTS.", Snackbar.LENGTH_LONG).show();
            }
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

                String theClientId = clientId.getText().toString().trim();
                if (theClientId.isEmpty()) {
                    showResults.setText("+ Client Identity is required.");
                    return;
                }
                accountCredentials.setClientId( theClientId );

                Snackbar.make(swipeRefreshLayout, "+ Validate token value...", Snackbar.LENGTH_LONG).show();
                // hide keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(tokenUrl.getWindowToken(), 0);
                try {
                    String theRequirementsMessage = "";
                    String theTokenUrl = tokenUrl.getText().toString().trim();
                    if (theTokenUrl.isEmpty()) {
                        theRequirementsMessage = theRequirementsMessage + "\n++ " + getString(R.string.labelTokenUrl) + " required.";
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
    private void checkSetTokenUrl(final String theTokenUrl) {
        // Snackbar.make(swipeRefreshLayout, "+ Get a sample access token...", Snackbar.LENGTH_LONG).show();
        OkHttpClient client = new OkHttpClient.Builder()
                .build();
        accountCredentials.setTokenUrl( theTokenUrl );
        // Snackbar.make(swipeRefreshLayout, "+ Token URL: " + accountCredentials.getCallTokenUrl(), Snackbar.LENGTH_LONG).show();
        Request request = new Request.Builder()
                .url(accountCredentials.getCallTokenUrl())
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                String theMsg = "- Failed to connect to the access token host. Settings NOT updated.";
                // Cause program to error out: showResults.setText(theMsg);
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
                        if ( jsonResponse.startsWith("{") ) {
                            // JSON error message.
                            theMsg = "- Failed to get an access token. Settings NOT updated.";
                        } else {
                            theMsg = "+ Got the access token. Settings updated.";
                            accountCredentials.setCredentials();
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