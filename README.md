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
3. [Create a Twilio Function to provide TwiML to make phone calls.](#bullet3)
4. [Create a TwiML Application entry to call the above Twilio Function.](#bullet4)
5. [Create an API Key and secret key string.](#bullet5)
6. [Twilio Function Configuration](#bullet6)

Valid access tokens can now be generated for use to make phone calls.

7. [Test using Owl Call.](#bullet7)

8. [Set up to receive incoming phone calls.](#bullet8)

### <a name="bullet1"></a>Create a Subaccount

This is optional, however it recommended that you use (create) a subaccount as a method to keep this sample separate from your main account.

    Subaccount name: owlvc
    Example (not actual) account SID = "ACrt0e356hksr34d16d8d4t8l390284a3".
    
### <a name="bullet2"></a>Create a Twilio Function to generate access tokens.

In the following, you will need to replace the sample domain name, "about-time-6360.twil.io," with your Runtime Domain name.
You can view your Runtime Domain name at this link:

[https://www.twilio.com/console/runtime/overview](https://www.twilio.com/console/runtime/overview)

Now, create the Function. In the Twilio Console, go to the Functions page:

    [https://www.twilio.com/console/runtime/functions](https://www.twilio.com/console/runtime/functions)

1. Click the Create Function icon (circle with plus sign in the middle).
2. Click Blank. Click Create.
   - Properties, Function Name: Generate Voice Access Token
   - URL Path: https://about-time-6360.twil.io /generateVoiceToken (note, your domain is display here)
   - For testing, uncheck Configuration, Access Control to allow accessible from a browser.
   - Copy and paste the contents of [generateVoiceToken.js](generateVoiceToken.js) into the Code box.
3. Click Save.

### <a name="bullet3"></a>Create a Twilio Function to provide TwiML to make phone calls.

In the Console, go to:

[https://www.twilio.com/console/runtime/functions](https://www.twilio.com/console/runtime/functions)
    
1. Click the Create Function icon (circle with plus sign in the middle).
2. Click Blank. Click Create.
   - Properties, Function Name: Make a call
   - URL Path: https://about-time-6360.twil.io /makecall (note, use your domain here)
   - For testing, uncheck Configuration, Access Control to allow accessible from a browser.
   - Copy and paste the contents of [makecall.js](https://github.com/tigerfarm/OwlClient/blob/master/makecall.js) into the Code box.
3. Click Save.
4. Test by using your browser to go to:

    https://about-time-6360.twil.io/makecall
    Replace "about-time-6360.twil.io" with your Twilio Function domain name.

The response:

    <?xml version="1.0" encoding="UTF-8"?><Response> ... </Response>

In the Twilio Console, under the Hello Voice World code section, a log entry is made:

    + Call from, identity: undefined.

In your browser, go to:

    https://about-time-6360.twil.io/sayhello?From=here.
    In the Twilio Console, a log entry is made: + Call from, identity: here.
    This is debugging tool when testing the sample voice app.

### <a name="bullet4"></a>Create a TwiML Application entry to call the above Twilio Function.

In the Twilio Console, go to the Create TWiML Application page:

    [https://www.twilio.com/console/voice/runtime/twiml-apps](https://www.twilio.com/console/voice/runtime/twiml-apps)

    Enter the following:
    Friendly name: HelloWorldApp 
    Voice, Request URL: https://about-time-6360.twil.io/makecall (note, use your domain here)
    After clicking Save, go back into the application entry to view the TwiML application API SID.
    Example: APxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    
Note, the TwiML application API SID will be used in a later step. The API SID will be used when generating access tokens.

### <a name="bullet5"></a>Create API Key and secret key string.

In the Twilio Console, go to:

    [https://www.twilio.com/console/voice/runtime/api-keys](https://www.twilio.com/console/voice/runtime/api-keys)
    https://www.twilio.com/console/voice/runtime/api-keys/create
    Friendly name: owlvcapp
    API key SID: SKxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
    API key Secret: yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy

Note, the API key SID and Secret will be used in the next step. The API key SID and Secret will be used when generating access tokens. The  account SID, API key and secret are the authentication keys. They must be from the same account or subaccount (not a combination of account).

### <a name="bullet6"></a>Twilio Function Configuration

Configure your account's Twilio Functions settings.
In Twilio Console, go to:
    
    https://www.twilio.com/console/runtime/functions/configure
    
Check: Enable ACCOUNT_SID and AUTH_TOKEN.
- This allows your Functions to access your account SID and auth token as environment variables.

Create Function Environment Variables.

    (Key : value)
    CLIENT_ID : Example, owluser (Your default Client identity attribute, no spaces)
    CLIENT_PHONE_NUMBER : Example, +12223331234 (Your Twilio phone number)
    
    OUTGOING_APPLICATION_SID : APxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx (AP value created above)
    VOICE_API_KEY : SKxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx (SK value created above)
    VOICE_API_SECRET : yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy (secret key string value created above)
    
    Click Save, to save the environment variables.

Generate an test access token.

    URL request: https://about-time-6360.twil.io/generateVoiceToken

Response sample:

    {"accesstoken":"eyJhbJciOiJIUzI1NiIsInR5cCI6IkpXVCIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTSzc0Y2JjOTAzN2QyMWM3YmMzNWU0NWE4OTFkNGZiZTEzLTE1MTAwNzgxNzUiLCJncmFudHMiOnsiaWRlbnRpdHkiOiJzdGFjeWRhdmlkIiwidm9pY2UiOnsib3V0Z29kackdOnsiYXBwbGljYXRpb25fc2lkIjoiQVBlYjQ2Mjc2NTVhMmE0YmU1YWUxYmE5NjJmYzk1NzZjZiJ9fX0sImlhdCI6MTUxMDA3ODE3NSwiZXhwIjoxNTEwMDgxNzc1LCJpc3MiOiJTSzc0Y2JjOTAzN2QyMWM3YmMzNWU0NWE4OTFkNGZiZTEzIiwic3ViIjoiQUNlMmFkODFkNmEwYzQxZmMwZTllZWViNWQxOWYxMGY2MyJ9.PYO9Kje1qDjitjdvJJon90IEilvN9njp2YGuJZr8nTI"}

Note, the above token will not work to make phone calls. You will need to create values in the following steps, and replace them into this Function.

### <a name="bullet7"></a>Test using Owl Call.

Go to Owl Call Settings.

    Enter your access token URL into Settings:
    https://about-time-6360.twil.io/generateVoiceToken
    Click Update.
    From the Owl Call home panel, click the icon to make a call.
    Owl Call will get an Access Token and make the call using the test TwiML application (Say Hello).
    You will here your Hello message from the Twilio service.

Owl Call is configured to make phone calls.

--------------------------------------------------

### <a name="bullet7"></a>Set up to Receive Incoming Phone Calls.

#### Configure Notifications.

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

Step 2: Get the Legacy server key.

In the Firebase Console, click your project.

https://console.firebase.google.com

    Click the project icon, example: owlvc.
    Beside the Project Overview, click the gear icon and then click Project Settings.
    Click Cloud Messaging.
    Legacy server key, sample:
      AAAALNIhpF4:APA9 ... 2n_1-tmYb772fs

    The Legacy server key is the value to copy into the Twilio Console, steps following.

Step 3: Use the Firebase Legacy server key to Create a Twilio Push Creditional for Notifications

    Goto the following and click Create:
    
https://www.twilio.com/console/voice/credentials

    Properties, Friendly Name: Owl Call
    Type: FCM
    FCM Secret: use the Firebase FCM Server API Key, sample:
      AAAALNIhpF4:APA9 ... 2n_1-tmYb772fs
    Click Save.
    
The Credential SID is now displayed, sample:

    Credential SID: CR1c259383fdcd12f14krm957358373f02

Use the Push Credential SID to generate access token code.

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
