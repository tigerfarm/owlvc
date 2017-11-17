package com.twilio.voice.quickstart;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AccountCredentials implements Interceptor {

    private EncDecString EncDec;

    //                                 12345678901234567890123 (max 23 char)
    private static final String TAG = "AccountCredentials";

    private Context mContext;
    //
    private String accountSid = "";
    private String authToken;
    private String credentials;
    //
    private String twilioPhoneNumber;
    private String toPhoneNumber;
    private int localTimeOffset;
    private boolean showContacts = true;
    //
    // ---------------------------------------------------------------------------------------------
    private SharedPreferences sharedPreferences;

    public AccountCredentials(Context context) {
        this.mContext = context;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        EncDec = new EncDecString();
        getAccountSid();
        this.authToken = getAccountToken();
        this.credentials = Credentials.basic(accountSid, authToken);
    }

    public void setCredentials() {
        this.credentials = Credentials.basic(accountSid, authToken);
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials).build();
        return chain.proceed(authenticatedRequest);
    }

    private String getDecrypted(String attributeName) {
        String encValue = sharedPreferences.getString(attributeName, "");
        String decyrptedValue = "";
        try {
            decyrptedValue = EncDec.decryptBase64String(encValue);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return decyrptedValue;
    }
    private void setEncrypted(String attributeName, String theValue) {
        this.authToken = theValue;
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        try {
            prefEditor.putString(attributeName, EncDec.encryptBase64String(theValue));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        prefEditor.apply();
        prefEditor.commit();
        return;
    }
    // ---------------------------------------------------------------------------------------------
    // Encrypt the account token when stored on the phone.
    // Decrypt for use in the application.

    public String getAccountSid() {
        this.accountSid = getDecrypted("account_sid"); // sharedPreferences.getString("account_sid", "");
        return this.accountSid;
    }
    public void setAccountSid(String aParam) {
        setEncrypted("account_sid", aParam);
        this.accountSid = aParam;
    }
    public boolean existAccountSid() {
        if (accountSid.isEmpty()) {
            return false;
        }
        return true;
    }

    public String getAccountToken() {
        this.authToken = getDecrypted("auth_token");
        return this.authToken;
    }
    public void setAccountToken(String aParam) {
        setEncrypted("auth_token", aParam);
        this.authToken = aParam;
    }

    // ----------------------------------------------------
    // Needs to be set, in the Settings panel. Or calculate difference from GMT to local time.
    public void setLocalTimeOffset(String aParam) {
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        prefEditor.putString("local_time_offset", aParam);
        prefEditor.apply();
        prefEditor.commit();
    }
    public int getLocalTimeOffset() {
        return Integer.parseInt( sharedPreferences.getString("local_time_offset", "") );
    }
    public double getLocalTimeOffsetDouble() {
        return Double.parseDouble( sharedPreferences.getString("local_time_offset", "") );
    }
    public String getLocalTimeOffsetString() {
        return sharedPreferences.getString("local_time_offset", "");
    }

    // ---------------------------------------------------------------------------------------------
    // Application information maintained between panels and between app stopping and starting.

    public void setTwilioPhoneNumber(String aParam) {
        setEncrypted("twilio_phone_number", aParam);
    }
    public String getTwilioPhoneNumber() {
        return getDecrypted("twilio_phone_number");
    }

    public void setToPhoneNumber(String aParam) {
        setEncrypted("to_phone_number", aParam);
    }
    public String getToPhoneNumber() {
        return getDecrypted("to_phone_number");
    }

    public void setToContactName(String aParam) {
        setEncrypted("to_contact_name", aParam);
    }
    public String getToContactName() {
        return getDecrypted("to_contact_name");
    }

    public void setAccPhoneNumberList(String aParam) {
        setEncrypted("account_phone_number_list", aParam);
    }
    public String getAccPhoneNumberList() {
        return getDecrypted("account_phone_number_list");
    }

    public void setSendToList(String aParam) {
        setEncrypted("send_to_list", aParam);
    }
    public String getSendToList() {
        return getDecrypted("send_to_list");
    }

    public void setShowContacts(boolean aParam) {
        SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        prefEditor.putBoolean("show_contacts", aParam);
        prefEditor.apply();
        prefEditor.commit();
    }
    public boolean getShowContacts() {
        return sharedPreferences.getBoolean("show_contacts", true);
    }

    // ----------------------------------------------------
    // Future use when improving preformance.
    private String accNumberList = "";
    public void setAccNumberList(String aParam) {
        setEncrypted("account_phone_number_list", aParam);
    }
    public String getAccNumberList() {
        return getDecrypted("account_phone_number_list");
    }

    // ---------------------------------------------------------------------------------------------
    String localDateTime(String theGmtDate) {
        // :Tue, 26 Sep 2017 00:49:31 +0000:
        //  012345678901234567890123456789
        int numDateStart = 5;
        int numDateEnd = 25;
        if (theGmtDate.length() < numDateEnd) {
            return "01 Jan 1980 00:00:00";      // return a default value
        }
        //                                                        "26 Sep 2017 00:49:31"
        SimpleDateFormat readDateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        Date gmtDate = new Date();
        try {
            gmtDate = readDateFormatter.parse(theGmtDate.substring(numDateStart, numDateEnd));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(gmtDate);
        // -------------------------
        String theOffset = getLocalTimeOffsetString();
        // 123
        // 5.5
        int ie = theOffset.indexOf(".");
        if (ie >= 0) {
            theOffset = theOffset.substring(0, ie);
            cal.add(Calendar.HOUR, Integer.parseInt(theOffset)); // from GMT to PST
            cal.add(Calendar.MINUTE, 30 ); // India and Newfounland use half hour offset.
        } else {
            cal.add(Calendar.HOUR, Integer.parseInt(theOffset)); // from GMT to PST
        }
        // -------------------------
        SimpleDateFormat writeDateformatter = new SimpleDateFormat("MMM dd, yyyy hh:mm a");
        return writeDateformatter.format(cal.getTime());
    }

    // ---------------------------------------------------------------------------------------------
}
