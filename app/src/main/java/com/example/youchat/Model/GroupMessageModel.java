package com.example.youchat.Model;

public class GroupMessageModel {
    private String msgId;
    private String senderId;
    private String message;
    private long time;
    private boolean isImage;

    public GroupMessageModel(String msgId, String senderId, String message, long time, boolean isImage) {
        this.msgId = msgId;
        this.senderId = senderId;
        this.message = message;
        this.time = time;
        this.isImage = isImage;
    }

    public GroupMessageModel() {
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }
}
