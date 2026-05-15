package com.xinyu.app.model;

public class MoodRecord {
    private long id;
    private String username;
    private int moodValue; // 1-5
    private String moodLabel; // 难过/焦虑/平静/开心/超棒
    private String emoji;
    private String note;
    private long createdAt;
    private int heartRate; // -1 if not available

    public MoodRecord() {
        this.heartRate = -1;
    }

    public MoodRecord(String username, int moodValue, String moodLabel, String emoji, String note) {
        this.username = username;
        this.moodValue = moodValue;
        this.moodLabel = moodLabel;
        this.emoji = emoji;
        this.note = note;
        this.createdAt = System.currentTimeMillis();
        this.heartRate = -1;
    }

    public MoodRecord(String username, int moodValue, String moodLabel, String emoji, String note, int heartRate) {
        this.username = username;
        this.moodValue = moodValue;
        this.moodLabel = moodLabel;
        this.emoji = emoji;
        this.note = note;
        this.createdAt = System.currentTimeMillis();
        this.heartRate = heartRate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getMoodValue() {
        return moodValue;
    }

    public void setMoodValue(int moodValue) {
        this.moodValue = moodValue;
    }

    public String getMoodLabel() {
        return moodLabel;
    }

    public void setMoodLabel(String moodLabel) {
        this.moodLabel = moodLabel;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }
}
