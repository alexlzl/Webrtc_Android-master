package com.android.webrtc;


import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;


public class Model {
    /**
     * 消息类型
     */
    private String messageType;
    private String from;
    private String fromName;
    private String to;
    private String toName;
    private SessionDescription sessionDescription;
    private IceCandidate iceCandidate;
    /**
     * 1成功 2失败
     */
    private int isSucceed;


    public Model(String messageType, String fromName, String from, String toName, String to) {
        this.messageType = messageType;
        this.fromName = fromName;
        this.from = from;
        this.toName = toName;
        this.to = to;
    }

    public Model(String fromName, String from, String toName, String to) {
        this.fromName = fromName;
        this.from = from;
        this.toName = toName;
        this.to = to;
    }


    public IceCandidate getIceCandidate() {
        return iceCandidate;
    }

    public void setCandidate(IceCandidate iceCandidate) {
        this.iceCandidate = iceCandidate;
    }

    public int getIsSucceed() {
        return isSucceed;
    }

    public void setIsSucceed(int isSucceed) {
        this.isSucceed = isSucceed;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public SessionDescription getSessionDescription() {
        return sessionDescription;
    }

    public void setSessionDescription(SessionDescription sessionDescription) {
        this.sessionDescription = sessionDescription;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
