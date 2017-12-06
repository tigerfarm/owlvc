# Owl Call

This document contains the steps to configure your Twilio Account so that you can make voice calls using the Owl Call app.

This project is based on the Twilio SDK Voice sample. Owl Call's enhancements to the original project:

    + An Edit text field to enter the call-to phone number (PSTN), Twilio Client id, or Twilio SIP address.
    + The call-to value is passed to the Twilio Function that connects the app to the other caller.
    + Google Contacts are listed and can be used to make phone calls.
    + A new access token is retreived before making a call.
    + Access codes are generated from a Twilio Function.

### Step-by-step guide to set up and generate voice access tokens

Following are the steps to create the values and infrastructure components to generate access tokens which are used to make outgoing calls.

In the following, example data is used, not live data.

1. [Create a Subaccount.](#bullet1)
2. [Create a Twilio Function to generate access tokens.](#bullet2)

This function uses the subaccount SID from the previous step, and will use the values from the next 3 steps.

3. [Create an API Key and secret key string.](#bullet3)
4. [Create a test Twilio Function to generate TwiML to say hello.](#bullet4)
5. [Create a TwiML Application entry to call the above Twilio Function.](#bullet5)

Valid access tokens can now be generated for use to make phone calls.

6. [Test using Owl Call.](#bullet6)

### <a name="bullet1"></a>Create a Subaccount

To keep this sample separate from your main account, create a subaccount.

    Subaccount name: owlvc
    Example (not actual) account SID = "ACrt0e356hksr34d16d8d4t8l390284a3".
    
### <a name="bullet5"></a>Create Twilio Function to generate access tokens using the above values.

Go to the Functions page:

    [https://www.twilio.com/console/runtime/functions](https://www.twilio.com/console/runtime/functions)
    
    Click the Create Function icon (red circle with white plus sign).
    In the New Function popup, click Blank, then click Create.
    Enter the following:
    Properties, Function Name: Generate Access Token
    URL: https://about-time-6360.twil.io/at
    Configuration, Access Control: uncheck so that it is accessible from a browser.
    Code:
    
    exports.handler = function(context, event, callback) {
       // Documentation: https://www.twilio.com/docs/api/rest/access-tokens
       
       // Authorization parameters
       const twilioAccountSid = 'ACrt0e356hksr34d16d8d4t8l390284a3';
       const twilioApiKey = 'SKe0b13kwe94wir04ofoq5d4bd9f8b2c';
       const twilioApiSecret = 'SuwkWen6Q5zNFvAkwlk49wMsXpDKOQ1bo';
       // Outgoing application parameters
       const outgoingApplicationSid = 'APeb4627655a2a4be5ae1ba962fc9576cf';
       const identity = 'stacydavid'; // When making phone calls, this is your client id, your callerid.
       
       // Generate the access token with voice grants.
       const AccessToken = require('twilio').jwt.AccessToken;
       const VoiceGrant = AccessToken.VoiceGrant;
       const voiceGrant = new VoiceGrant({
          outgoingApplicationSid: outgoingApplicationSid
       });
       const token = new AccessToken(twilioAccountSid, twilioApiKey, twilioApiSecret);
       token.addGrant(voiceGrant);
       token.identity = identity;
       // Output the token.
       console.log(token.toJwt());
       let response = { accesstoken: token.toJwt() };
       callback(null, response);
    };

Make changes,
+ Replace the twilioAccountSid value with your subaccount SID (or if not using a subaccount, your account SID).
+ Replace the identity value with your own identity name, no spaces (use: a..z).

Generate an test access token.

    URL request: https://about-time-6360.twil.io/at

Response sample:

    {"accesstoken":"eyJhbJciOiJIUzI1NiIsInR5cCI6IkpXVCIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTSzc0Y2JjOTAzN2QyMWM3YmMzNWU0NWE4OTFkNGZiZTEzLTE1MTAwNzgxNzUiLCJncmFudHMiOnsiaWRlbnRpdHkiOiJzdGFjeWRhdmlkIiwidm9pY2UiOnsib3V0Z29kackdOnsiYXBwbGljYXRpb25fc2lkIjoiQVBlYjQ2Mjc2NTVhMmE0YmU1YWUxYmE5NjJmYzk1NzZjZiJ9fX0sImlhdCI6MTUxMDA3ODE3NSwiZXhwIjoxNTEwMDgxNzc1LCJpc3MiOiJTSzc0Y2JjOTAzN2QyMWM3YmMzNWU0NWE4OTFkNGZiZTEzIiwic3ViIjoiQUNlMmFkODFkNmEwYzQxZmMwZTllZWViNWQxOWYxMGY2MyJ9.PYO9Kje1qDjitjdvJJon90IEilvN9njp2YGuJZr8nTI"}

Note, the above token will not work to make phone calls. You will need to create values in the following steps, and replace them into this Function.

### <a name="bullet3"></a>Create API Key and secret key string.

    [https://www.twilio.com/console/voice/runtime/api-keys](https://www.twilio.com/console/voice/runtime/api-keys)
    https://www.twilio.com/console/voice/runtime/api-keys/create
    Friendly name: owlvcapp
    API key SID: SKe0b13kwe94wir04ofoq5d4bd9f8b2c
    API key Secret: SuwkWen6Q5zNFvAkwlk49wMsXpDKOQ1bo

Add the API key SID and Secret into the Generate Access Token Function.
Note, the  account SID, API key and secret are the authentication keys. They must be from the same account or subaccount (not a combination of each).

### <a name="bullet4"></a>Create a Twilio Function to say hello.

Create a Twilio Function to say a message to the sample voice app when it makes its first test calls.

Go to the Functions page:

    [https://www.twilio.com/console/runtime/functions](https://www.twilio.com/console/runtime/functions)
    
    Click the Create Function icon (red circle with white plus sign).
    In the New Function popup, click Blank, then click Create.
    Enter the following:
    Properties, Function Name: Say Hello and add a log message
    URL Path: https://about-time-6360.twil.io  /sayhello
    Configuration, Access control: uncheck to allow web browser access.
    Code:
    exports.handler = function(context, event, callback) {
      console.log("+ Call from, identity: " + event.From);
      let twiml = new Twilio.twiml.VoiceResponse();
      twiml.say("Hello there, and congratulations. Your Twilio voice app is working.");
      callback(null, twiml);
    };
    
Test by using your browser to go to:

    https://about-time-6360.twil.io/sayhello
    Replace "about-time-6360.twil.io" with your Twilio Function host name.

The response:

    <?xml version="1.0" encoding="UTF-8"?><Response><Say>Hello World, and congratulations. Your Twilio voice app is working.</Say></Response>

In the Twilio Console, under the Hello Voice World code section, a log entry is made:

    + Call from, identity: undefined.

In your browser, go to:

    https://about-time-6360.twil.io/sayhello?From=here.
    In the Twilio Console, a log entry is made: + Call from, identity: here.
    This is debugging tool when testing the sample voice app.

### <a name="bullet5"></a>Create a TwiML Application entry to call the above Twilio Function.

Go to the Create TWiML Application page:

    [https://www.twilio.com/console/voice/runtime/twiml-apps](https://www.twilio.com/console/voice/runtime/twiml-apps)

    Enter the following:
    Friendly name: HelloWorldApp 
    Voice, Request URL: https://about-time-6360.twil.io/sayhello
    After clicking Save, go back into the application entry to get the TwiML application API SID.
    Example: APeb4627655a2a4be5ae1ba962fc9576cf
    
Add the TwiML application API SID into the Generate Access Token Function.

Your Generate Access Token Function now has all the requirements to make a test phone call.

### <a name="bullet6"></a>Test using Owl Call.

Go to Owl Call Settings.

    Enter your access token URL into Settings:
    https://about-time-6360.twil.io/at
    Click Update.
    From the Owl Call home panel, click the icon to make a call.
    Owl Call will get an Access Token and make the call using the test TwiML application (Say Hello).
    You will here your Hello message from the Twilio service.

Owl Call is configured to make test calls. The next step is use a TwiML application that will place phone calls.

### <a name="bullet7"></a>Create a Twilio Function to Place Phone Calls.

This Twilio Function generates TwiML to place a phone call.

Go to the Functions page:

    [https://www.twilio.com/console/runtime/functions](https://www.twilio.com/console/runtime/functions)
    
    Click the Create Function icon (red circle with white plus sign).
    In the New Function popup, click Blank, then click Create.
    Enter the following:
    Properties, Function Name: Say Hello and add a log message
    URL Path: https://about-time-6360.twil.io  /callandlog
    Configuration, Access control: uncheck to allow web browser access.
    Code:
    exports.handler = function (context, event, callback) {
    let twiml = new Twilio.twiml.VoiceResponse();
    console.log("---------------------------------------------------------");
    let callTo = event.To || null;
    if (callTo === null) {
        twiml.say({voice: 'alice', language: 'en-CA', }, 'Error placing the call. The To-caller is required.');
        callback(null, twiml);
        return;
    }
    console.log("+ Call To: " + callTo);
    let callFrom = event.From || null;
    if (callFrom === null) {
        twiml.say({voice: 'alice', language: 'en-CA', }, 'Error placing the call. The From-caller is required.');
        callback(null, twiml);
        return;
    }
    console.log("+ Call From: " + callFrom);
    //
    // Set callerid phone number based on the Client id.
    // Note, callerid must be a verified phone number, or in the same Twilio account.
    if (callFrom === "client:stacymobile") {
        callFrom = "+12223331234";
    } else if (callFrom === "client:stacydavid") {
        callFrom = "+17778887890";
    } else if (callTo.startsWith("client:")) {
      	// Leave as is because this is a Client to Client call.
    } else {
        console.log("- Error: Client id not in the list.");
        twiml.say({voice: 'alice', language: 'en-CA', }, 'Error placing the call. Unknown client id.');
        callback(null, twiml);
        return;
    }
    console.log("+ Call From, caller id: " + callFrom);
    //
    let dialParams = {};
    dialParams.callerId = callFrom
    dialParams.record = "do-not-record"
    if (callTo.startsWith("sip:")) {
        // Example SIP address: "sip:myuser@mydomain.sip.us1.twilio.com"
        console.log("+ Make a SIP call.");
        twiml.dial(dialParams).sip(callTo);
    } else if (callTo.startsWith("client:")) {
        // Example Client address: client:stacyhere
        // Remove "client:"        01234567
        console.log("+ Make a Client call.");
        twiml.dial(dialParams).client(callTo.substr(7));
    } else if (callTo.startsWith("conference:")) {
        // Example Client address: conference:weeklymeeting
        console.log("+ Make a Conference call.");
        twiml.dial(dialParams).conference(callTo.substr(7));
    } else {
        console.log("+ Make a PSTN call.");
        twiml.dial(dialParams, callTo);
    }
    callback(null, twiml);
    };
    
Test by using your browser to go to:

    https://satisfying-quartz-1625.twil.io/callandlog?To=client:stacydavid&From=client:mycat
Replace "about-time-6360.twil.io" with your Twilio Function host name.

The response, which is a Client to Client example:

    <?xml version="1.0" encoding="UTF-8"?><Response><Dial callerId="client:mycat" record="do-not-record"><Client>stacydavid</Client></Dial></Response>

In the Twilio Console, log message are displayed: + Call To: client:stacydavid, + Call From: client:mycat, + Call From, caller id: client:mycat, + Make a Client call.

--------------------------------------------------

### <a name="bullet7"></a>Configuration to Recieve Phone Calls.

Step 1: Use Firebase to generate a google-services.json file

    In Android Studio, select Tools/Firebase.
    In Firebase Assistant, use Notifications to click Connect which redirects to the browser.
    In the browser, log in to your Google account.
    This brings you back to Android Studio which has a Firebase popup.
    Your app is checked: Create new Firebase project. Click Connect to Firebase.
    Studio runs for a while. At the base, is the message: Firebase project created...
    Further configurations are available, which I'm not going to do:
    https://console.firebase.google.com/?utm_source=studio&pli=1
    https://developer.android.com/studio/login.html?success=true#

Step 2: Configure Notifications.

In the Firebase Console, click your project.

https://console.firebase.google.com

    Beside the Project Overview, click the gear icon and then click Project Settings.
    Click Cloud Messaging.
    The Legacy server key is the value to copy into the Twilio Console, steps following.

    Goto the following and click Create:
    
https://www.twilio.com/console/voice/credentials

    Properties, Friendly Name: Owl Call
    Type: FCM
    FCM Secret: use the Firebase FCM Server API Key, sample:
AAAALNIhpF4:APA9 ... 2n_1-tmYb772fs
    Click Save.
    
The Credential SID is now displayed:

    Credential SID: CR1c244322fdcd12f14faed95731399f02
   
Use the Credential SID in the generate access token code.

    ...
    // Incoming application parameter
    const pushCredentialSid = 'CR1c244322fdcd12f14faed95731399f02';
    // Generate the access token with voice grants.
    const AccessToken = require('twilio').jwt.AccessToken;
    const VoiceGrant = AccessToken.VoiceGrant;
    const voiceGrant = new VoiceGrant({
       pushCredentialSid: pushCredentialSid,
       outgoingApplicationSid: outgoingApplicationSid
    });
    ...

Notifications are now configured.

Configure Twilio phone number to call the Client.

    <?xml version="1.0" encoding="UTF-8"?>
    <Response>
        <Dial>
            <Client>owlcalluser</Client>
        </Dial>
    </Response>

--------------------------------------------------

Future features:

    + Document how to configure for incoming calls, which includes notifications.
    + Add Twilio account information to the Setting panel, same as Owl SMS. This will allow:
    ++ Listing call logs.
    ++ Using Twilio Lookup to get phone number information.
