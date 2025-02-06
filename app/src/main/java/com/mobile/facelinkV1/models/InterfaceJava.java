package com.mobile.facelinkV1.models;

import android.webkit.JavascriptInterface;

import com.mobile.facelinkV1.activities.CallActivity;

public class InterfaceJava {
    CallActivity callActivity;

    public InterfaceJava(CallActivity callActivity){
        this.callActivity = callActivity;

    }

    @JavascriptInterface
    public void onPeerConnected(){
        callActivity.onPeerConnected();
    }
}
