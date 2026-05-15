package com.xinyu.app.model;

public class SupportMessage {
    private int id;
    private int matchId;
    private String senderDeviceId;
    private String content;
    private boolean isRead;
    private long createdAt;

    public SupportMessage() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public String getSenderDeviceId() { return senderDeviceId; }
    public void setSenderDeviceId(String senderDeviceId) { this.senderDeviceId = senderDeviceId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
