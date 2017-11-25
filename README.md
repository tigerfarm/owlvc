# Owl Call

This document contains the set up steps to make voice calls using the Owl Call Android app.

This project is based on the getting started with Voice on Android project.

Owl Call's enhancements to the original:

    + An Edit text field to enter the call-to phone number (PSTN), Twilio Client id, or Twilio SIP address.
    + The call-to value is passed to the Twilio Function that connects the app to the other caller.
    + Contact list is displayed in a refreshable ListView.
    + A new access token is retreived before making a call.
    + Access codes are generated from a Twilio Function.

Future features:

    + Add Twilio account information to Setting, same as Owl SMS. This will allow:
    ++ Adding call logs.
    ++ Adding Twilio Lookup to get phone number information.
    + Handle incoming calls.
    + Notification of an incoming call.

### Step-by-step guide to set up and generate voice access tokens

Following are the steps to create the values and infrastructure components to generate access tokens which are used to make outgoing calls.

In the following, example data is used, not live data.

1. [Create a Subaccount.](#bullet1)
2. [Create API Key and secret key string.](#bullet2)
3. [Configure a server to generate an access token to use in the app](#bullet3)
4. [Create a TwiML Application using the above Twilio Function URL.](#bullet4)
5. [Create Twilio Function to generate access tokens using the above values.](#bullet5)
6. [Test using Owl Call.](#bullet6)

### <a name="bullet1"></a>Create a Subaccount

To keep this sample separate from your main account, create a subaccount.

    Subaccount name: owlvc
    Example (not actual) account SID = "ACrt0e356hksr34d16d8d4t8l390284a3".

### <a name="bullet2"></a>Create API Key and secret key string.

    [https://www.twilio.com/console/voice/runtime/api-keys](https://www.twilio.com/console/voice/runtime/api-keys)
    https://www.twilio.com/console/voice/runtime/api-keys/create
    Friendly name: owlvcapp
    API key SID: SKe0b13kwe94wir04ofoq5d4bd9f8b2c
    API key Secret: SuwkWen6Q5zNFvAkwlk49wMsXpDKOQ1bo

Note, the  account SID, API key and secret are the authentication keys. They must be from the same account or subaccount (not a combination of each).

### <a name="bullet3"></a>Create a Twilio Function to say a message to the sample voice app when it makes its first test calls.

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

### <a name="bullet4"></a>Create a TwiML Application using the above Twilio Function URL.

Go to the Create page:

    [https://www.twilio.com/console/voice/runtime/twiml-apps](https://www.twilio.com/console/voice/runtime/twiml-apps)

    Enter the following:
    Friendly name: HelloWorldApp 
    Voice, Request URL: https://about-time-6360.twil.io/sayhello
    After clicking Save, go back into the app entry to get the app SID. Example: APeb4627655a2a4be5ae1ba962fc9576cf

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
       //
       // Authorization parameters
       const twilioAccountSid = 'ACrt0e356hksr34d16d8d4t8l390284a3';
       const twilioApiKey = 'SKe0b13kwe94wir04ofoq5d4bd9f8b2c';
       const twilioApiSecret = 'SuwkWen6Q5zNFvAkwlk49wMsXpDKOQ1bo';
       // Outgoing application parameters
       const identity = 'stacydavid'; // callerid (Twilio Function: event.From)
       const outgoingApplicationSid = 'APeb4627655a2a4be5ae1ba962fc9576cf';
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

Note, for the identity constant, user your own identity name, no spaces (use: a..z).

Generate an access token.

    URL request: https://about-time-6360.twil.io/at

Response:

    {"accesstoken":"eyJhbJciOiJIUzI1NiIsInR5cCI6IkpXVCIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTSzc0Y2JjOTAzN2QyMWM3YmMzNWU0NWE4OTFkNGZiZTEzLTE1MTAwNzgxNzUiLCJncmFudHMiOnsiaWRlbnRpdHkiOiJzdGFjeWRhdmlkIiwidm9pY2UiOnsib3V0Z29kackdOnsiYXBwbGljYXRpb25fc2lkIjoiQVBlYjQ2Mjc2NTVhMmE0YmU1YWUxYmE5NjJmYzk1NzZjZiJ9fX0sImlhdCI6MTUxMDA3ODE3NSwiZXhwIjoxNTEwMDgxNzc1LCJpc3MiOiJTSzc0Y2JjOTAzN2QyMWM3YmMzNWU0NWE4OTFkNGZiZTEzIiwic3ViIjoiQUNlMmFkODFkNmEwYzQxZmMwZTllZWViNWQxOWYxMGY2MyJ9.PYO9Kje1qDjitjdvJJon90IEilvN9njp2YGuJZr8nTI"}

The above is a sample token that is used by the sample Twilio Android Voice SDK sample app.

### <a name="bullet5"></a>Test using Owl Call.

Go to Owl Call Settings.

    Enter your access token URL into Settings:
    https://about-time-6360.twil.io/at
    Click Update.


   
