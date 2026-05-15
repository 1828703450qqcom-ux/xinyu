package com.xinyu.app.model;

public class TestResult {
    private long id;
    private String username;
    private String testId; // phq9, gad7, stress, sleep
    private String testTitle;
    private int score;
    private String level;
    private String description;
    private long createdAt;

    public TestResult() {}

    public TestResult(String username, String testId, String testTitle, int score, String level, String description) {
        this.username = username;
        this.testId = testId;
        this.testTitle = testTitle;
        this.score = score;
        this.level = level;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
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

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestTitle() {
        return testTitle;
    }

    public void setTestTitle(String testTitle) {
        this.testTitle = testTitle;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
