package edu.stanford.muse.email.GmailAuth;

/*
 * Copyright (c) 2013-2019 Amuse Labs Pvt Ltd
 */


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;

/**
 * Verification with google auth service. Verify the integrity of the ID token and use the user information
 * contained in the token to establish a session or create a new account. For more information, please check:
 * https://developers.google.com/identity/sign-in/web/backend-auth
 * https://developers.google.com/identity/sign-in/web/sign-in
 * Created by jaya on 16/01/19.
 */
public class GoogleLoginVerifier {
    private static final Log log = LogFactory.getLog(GoogleLoginVerifier.class);

    //private static final String CLIENT_ID = "606753072852-c2aoi9qu516lvo05gscdb017062a83hm.apps.googleusercontent.com";
    private static final String CLIENT_ID = "893886069594-hsbcr0elt6u0vgr2qa7qei7boop3jgu2.apps.googleusercontent.com";

    /*
     * Verifies that the id token that the client supplied after google authentication is valid and retrieves user
     */
    public static AuthenticatedUserInfo verify(String idTokenString,String accessTokenString) {
        AuthenticatedUserInfo authInfo = null;
        try {
            HttpTransport transport = new NetHttpTransport(); // Assuming that google uses https connection to verifiy the token
            JsonFactory jsonFactory = new JacksonFactory();
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)

                    // Specify the CLIENT_ID of the app that accesses the backend:
                    .setAudience(Collections.singletonList(CLIENT_ID))
                    // Or, if multiple clients access the backend:
                    //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
                    .build();
            GoogleIdToken idToken = verifier.verify(idTokenString); // checks Google's signature, client ID, whether the token was issued by google.com domain, and the expiry date of the token

            if (idToken != null) {
                Payload payload = idToken.getPayload();
                 /* Here is a sample of payload message, it can be checked at: https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=<idToken>
                     {
                      "iss": "accounts.google.com",
                      "azp": "606753072852-c2aoi9qu516lvo05gscdb017062a83hm.apps.googleusercontent.com",
                      "aud": "606753072852-c2aoi9qu516lvo05gscdb017062a83hm.apps.googleusercontent.com",
                      "sub": "google auth id",
                      "hd": "amuselabs.com",
                      "email": "xxx@amuselabs.com",
                      "email_verified": "true",
                      "at_hash": "t5T8aOfCxG5fc8tVpVD0mQ",
                      "name": "given_name family_name",
                      "picture": "https://lh4.googleusercontent.com/-QCP5OXZJ5Zk/AAAAAAAAAAI/AAAAAAAAAAA/AKxrwcbXL-x9IN9rHOqyGqJkJyGEh49z5Q/s96-c/photo.jpg",
                      "given_name": "xxx",
                      "family_name": "xxx",
                      "locale": "en",
                      "iat": "1547620434",
                      "exp": "1547624034",
                      "jti": "2505067eb8e62056f0c3b4545be18ff27ed33fdd",
                      "alg": "RS256",
                      "kid": "08d3245c62f86b6362afcbbffe1d069826dd1dc1",
                      "typ": "JWT"
                    }
                  */

                // Get profile information from payload
                String googleUserId = payload.getSubject();
                String email = payload.getEmail();
                boolean emailVerified = payload.getEmailVerified(); // XXX should we do something if email is not verified ?
                String name = (String) payload.get("name");
                String familyName = (String) payload.get("family_name");
                String givenName = (String) payload.get("given_name");
                String hostedDomain = payload.getHostedDomain(); // for logging purpose, can be null

                String authToken = Util.hash(googleUserId + '|' + "Google"); // hash of the unique user id, we don't want to generate a new id for the user.
                JSPHelper.log.info("Google accessToken verification passed, logged in google userId:" + googleUserId + ", name:" + name + ", email:" + email +
                        ", emailVerified:" + emailVerified + ", familyName:" + familyName + ", givenName:" + givenName + ", hostedDomain:" + hostedDomain);

                // for storing in auth_info table
                authInfo = new AuthenticatedUserInfo(idTokenString, accessTokenString, /*AuthMethod.GOOGLE,*/ authToken, googleUserId, email, name);
            }
        } catch (Exception e) {
            Util.print_exception("Exception in Google token verifier token id:" + idTokenString, e, log);
        }
        return authInfo;
    }
}
