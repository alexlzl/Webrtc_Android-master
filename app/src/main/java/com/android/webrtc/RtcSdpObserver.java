package com.android.webrtc;

import android.util.Log;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;


public class RtcSdpObserver implements SdpObserver {

    private String TAG = "MySdpObserver";

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {

    }

    @Override
    public void onSetSuccess() {
        Log.e(TAG, "onSetSuccess ==  ");
    }

    @Override
    public void onCreateFailure(String s) {
        Log.e(TAG, "onCreateFailure ==  " + s);
    }

    @Override
    public void onSetFailure(String s) {
        Log.e(TAG, "onSetFailure ==  " + s);
    }
}
