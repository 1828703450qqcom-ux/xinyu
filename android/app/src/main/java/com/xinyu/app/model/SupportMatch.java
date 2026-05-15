package com.xinyu.app.model;

public class SupportMatch {
    private int matchId;
    private String partnerName;
    private String partnerDeviceId;
    private String lastMessage;
    private String lastTime;
    private String createdAt;

    public SupportMatch() {}

    public int getMatchId() { return matchId; }
    public void setMatchId(int matchId) { this.matchId = matchId; }

    public String getPartnerName() { return partnerName; }
    public void setPartnerName(String partnerName) { this.partnerName = partnerName; }

    public String getPartnerDeviceId() { return partnerDeviceId; }
    public void setPartnerDeviceId(String partnerDeviceId) { this.partnerDeviceId = partnerDeviceId; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastTime() { return lastTime; }
    public void setLastTime(String lastTime) { this.lastTime = lastTime; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
