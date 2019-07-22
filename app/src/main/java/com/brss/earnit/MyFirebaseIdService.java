package com.brss.earnit;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class MyFirebaseIdService extends FirebaseInstanceIdService {

    public static final String REG_TOKEN = "REG_TOKEN";

    @Override
    public void onTokenRefresh(){

        String recent_token = FirebaseInstanceId.getInstance().getToken();
        Common.currentToken = recent_token;
        Log.d(REG_TOKEN,recent_token);
    }

}
