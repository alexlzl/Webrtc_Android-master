package com.android.webrtc;


public class Constant {
    /**
     * server服务器地址
     */
    public static final String URL = "ws://10.1.59.115:8080/webrtc/websocket";
    /**
     * 穿透服务器
     */
    public static final String STUN = "stun:stun.l.google.com:19302";
    public static final String CHANNEL = "channel";

    public static final String OPEN = "open";
    /**
     * 注册用户信息
     */
    public static final String REGISTER = "register";
    /**
     * 注册回复
     */
    public static final String REGISTER_RESPONSE = "register_response";
    /**
     * 1=====成功
     */
    public static final int RESPONSE_SUCCEED = 1;
    /**
     * 2============失败
     */
    public static final int RESPONSE_FAILURE = 2;
    /**
     * 发起音视频通讯
     */
    public static final String CALL = "call";
    /**
     * 发起音视频通讯回复
     */
    public static final String CALL_RESPONSE = "call_response";
    public static final String INCALL = "incall";//接听
    public static final String INCALL_RESPONSE = "incall_response";//接听回复
    /**
     * 发送或者接收SDP
     */
    public static final String OFFER = "offer";
    public static final String CANDIDATE = "candidate";//ice互传
    public static final int VOLUME = 10;//声音调节

    public static final String VIDEO_TRACK_ID = "videtrack";
    public static final String AUDIO_TRACK_ID = "audiotrack";
    public static final String LOCAL_VIDEO_STREAM = "localVideoStream";
    public static final String LOCAL_AUDIO_STREAM = "localAudioStream";

    public static final int VIDEO_RESOLUTION_WIDTH = 320;
    public static final int VIDEO_RESOLUTION_HEIGHT = 240;
    public static final int VIDEO_FPS = 60;

    public static final String SHARE_PREFERENCE_NAME = "webrtc_sp";
}
