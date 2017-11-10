# Owl Call: make voice calls using this Android app

This project is based on the get started with Voice on Android project.

Feature enhancements to the original:
+ An Edit text field to enter the call-to phone number (PSTN), Twilio Client id, or Twilio SIP address.
+ The call-to value is passed to the Twilio Function that connects the app to the other caller.
+ Contact list is displayed in a refreshable ListView.
+ A new access token is retreived before making a call.
+ Access codes are generated from a Twilio Function.

To do:
+ Make the contact list clickable. When clicked, the value becomes the Edit text field call-to value.
+ The contact list phone number value will need to formated into something like this: "+12223331234" from "(222)333-1234".

Then it will be ready to be posted on Google Play.
