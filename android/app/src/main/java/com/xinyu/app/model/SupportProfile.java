package com.xinyu.app.model;

import java.util.List;

public class SupportProfile {
    private String deviceId;
    private String anonymousName;
    private List<String> moodTags;
    private List<String> interestTags;
    private String bio;
    private boolean isActive;

    public SupportProfile() {}

    public SupportProfile(String deviceId, List<String> moodTags, List<String> interestTags, String bio) {
        this.deviceId = deviceId;
        this.moodTags = moodTags;
        this.interestTags = interestTags;
        this.bio = bio;
        this.isActive = true;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getAnonymousName() { return anonymousName; }
    public void setAnonymousName(String anonymousName) { this.anonymousName = anonymousName; }

    public List<String> getMoodTags() { return moodTags; }
    public void setMoodTags(List<String> moodTags) { this.moodTags = moodTags; }

    public List<String> getInterestTags() { return interestTags; }
    public void setInterestTags(List<String> interestTags) { this.interestTags = interestTags; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
