exports.handler = function(context, event, callback) {
       // Documentation: https://www.twilio.com/docs/api/rest/access-tokens
       // Generate the access token with voice grants.
       const AccessToken = require('twilio').jwt.AccessToken;
       const VoiceGrant = AccessToken.VoiceGrant;
       const voiceGrant = new VoiceGrant({
          outgoingApplicationSid: context.OUTGOING_APPLICATION_SID
       });
       const token = new AccessToken(context.ACCOUNT_SID, context.VOICE_API_KEY, context.VOICE_API_SECRET);
       token.addGrant(voiceGrant);
       token.identity = context.CLIENT_ID;
       // Output the token.
       console.log(token.toJwt());
       let response = { accesstoken: token.toJwt() };
       callback(null, response);
};
