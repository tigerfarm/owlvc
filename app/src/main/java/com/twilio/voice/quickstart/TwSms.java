package com.twilio.voice.quickstart;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class TwSms {

    // ---------------------------------------------------------------------------------------------
    // Twilio SMS API Requests

    private String setSmsRequest;
    // SMS Send: https://www.twilio.com/docs/api/messaging/send-messages
    // SMS Messages: https://www.twilio.com/docs/api/messaging/message

    private AccountCredentials accountCredentials;

    public TwSms(AccountCredentials accountCredentials) {
        this.accountCredentials = accountCredentials;
        this.setSmsRequest = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountCredentials.getAccountSid());
    }

    private RequestBody postParams;
    public RequestBody getPostParams() throws Exception {
        return postParams;
    }

    private String requestUrl;
    public String getRequestUrl() {
        return requestUrl;
    }

    public void setSmsRequestLogs(String fromPhoneNumber, String toPhoneNumber) {
        requestUrl = setSmsRequest + "?From="+fromPhoneNumber + "&To="+toPhoneNumber;
    }
    public void setSmsRequestLogsTo(String phoneNumber) {
        requestUrl = setSmsRequest + "?To="+phoneNumber;
    }

    public void setSmsSend(String phoneNumTo, String twilioNumber, String theMessage) {
        postParams = new FormBody.Builder()
                .add("From", twilioNumber)
                .add("To", phoneNumTo)
                .add("Body", theMessage)
                .build();
        requestUrl = setSmsRequest;
    }

    public String rmSmsMessages(String messageSid) {
        // https://api.twilio.com/2010-04-01/Accounts/your_account_SID/Messages/SM1cacf80fb168403da49512ee7aa3ca16.json'
        return String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages/%s.json", accountCredentials.getAccountSid(), messageSid);
    }

    public void setAccPhoneNumbers() {
        // https://api.twilio.com/2010-04-01/Accounts/your_account_SID/IncomingPhoneNumbers.json
        requestUrl =  String.format("https://api.twilio.com/2010-04-01/Accounts/%s/IncomingPhoneNumbers.json", accountCredentials.getAccountSid());
    }

    public String getLookup(String thePhoneNumber) {
        // http://lookups.twilio.com/v1/PhoneNumbers/+12093539979?Type=carrier
        return "https://lookups.twilio.com/v1/PhoneNumbers/+" + thePhoneNumber + "?Type=carrier";
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
        String theOffset = accountCredentials.getLocalTimeOffsetString();
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
    // GET Hello World (for testing)
    // TwilioSms.setUrlHello();
    // textString.setText("+ GET Hello World text file: "+TwilioSms.getRequestUrl());
    // getRequest();
    private String urlHello = "http://tigerfarmpress.com/hello.txt";
    public void setUrlHello() throws Exception {
        requestUrl = urlHello;
    }


}
