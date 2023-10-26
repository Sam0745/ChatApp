package com.example.youchat.Model;

    public class MessageModel {
        private String msgId;
        private String senderId;
        private String receiverId;
        private String receiverName;
        private String message;
        private long time;
        private String imageUrl;
        private boolean isImage;
        private boolean isAudio;

        public MessageModel() {
        }

        public MessageModel(String msgId, String senderId, String receiverId, String receiverName, String message, long time, boolean isImage) {
            this.msgId = msgId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.receiverName = receiverName;
            this.message = message;
            this.time = time;
            this.isImage = isImage;
        }
        public MessageModel(String messageId, String senderId, String receiverId, String senderName, String message, long timestamp, boolean isImage,boolean isAudio) {
            this.msgId = messageId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.receiverName = senderName;
            this.message = message;
            this.time = timestamp;
            this.isImage = isImage;
            this.isAudio=isAudio;
        }

        /*public MessageModel(String messageId, String senderId, String receiverId, String receiverName, String audioUrl, long timestamp) {
            this.msgId = messageId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.receiverName = receiverName;
            this.audioUrl = audioUrl;
            this.time = timestamp;
        }*/

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

        public String getReceiverId() {
            return receiverId;
        }

        public void setReceiverId(String receiverId) {
            this.receiverId = receiverId;
        }

        public String getReceiverName() {
            return receiverName;
        }

        public void setReceiverName(String receiverName) {
            this.receiverName = receiverName;
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

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public boolean isImage() {
            return isImage;
        }

        public void setImage(boolean image) {
            isImage = image;
        }

        public boolean isAudio() {
            return isAudio;
        }

        public void setAudio(boolean audio) {
            isAudio = audio;
        }
    }