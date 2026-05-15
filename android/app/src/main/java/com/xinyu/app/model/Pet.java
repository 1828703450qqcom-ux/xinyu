package com.xinyu.app.model;

public class Pet {
    private String name;
    private int level;           // 1-10
    private int exp;             // current exp
    private int expToNext;       // exp needed for next level
    private int hunger;          // 0-100
    private int happiness;       // 0-100
    private int energy;          // 0-100
    private int stage;           // 0=egg, 1=baby, 2=child, 3=adult
    private long lastFeedTime;
    private long lastPlayTime;
    private long createdAt;

    // Evolution stages
    public static final String[][] STAGES = {
            {"🥚", "蛋蛋"},      // stage 0
            {"🐱", "小猫"},      // stage 1
            {"🐈", "猫咪"},      // stage 2
            {"😺", "大猫"},      // stage 3
    };

    public static final String[] MOODS = {
            "😊 开心", "😐 平静", "😢 难过", "😠 生气", "😴 困了", "🎉 兴奋"
    };

    public Pet() {
        this.name = "小屿";
        this.level = 1;
        this.exp = 0;
        this.expToNext = 100;
        this.hunger = 80;
        this.happiness = 80;
        this.energy = 80;
        this.stage = 0;
        this.lastFeedTime = 0;
        this.lastPlayTime = 0;
        this.createdAt = System.currentTimeMillis();
    }

    public String getEmoji() {
        if (stage >= STAGES.length) stage = STAGES.length - 1;
        return STAGES[stage][0];
    }

    public String getStageName() {
        if (stage >= STAGES.length) stage = STAGES.length - 1;
        return STAGES[stage][1];
    }

    public String getMoodText() {
        int avg = (hunger + happiness + energy) / 3;
        if (avg >= 80) return MOODS[0];
        if (avg >= 60) return MOODS[1];
        if (avg >= 40) return MOODS[4];
        if (avg >= 20) return MOODS[2];
        return MOODS[3];
    }

    public boolean isDead() {
        return hunger <= 0 && happiness <= 0 && energy <= 0;
    }

    public void addExp(int amount) {
        exp += amount;
        while (exp >= expToNext && level < 10) {
            exp -= expToNext;
            level++;
            expToNext = level * 100;
            // Evolve at certain levels
            if (level == 3 && stage == 0) stage = 1;
            else if (level == 6 && stage == 1) stage = 2;
            else if (level == 9 && stage == 2) stage = 3;
        }
    }

    public void feed() {
        hunger = Math.min(100, hunger + 30);
        happiness = Math.min(100, happiness + 5);
        lastFeedTime = System.currentTimeMillis();
        addExp(10);
    }

    public void play() {
        happiness = Math.min(100, happiness + 25);
        energy = Math.max(0, energy - 10);
        lastPlayTime = System.currentTimeMillis();
        addExp(10);
    }

    public void study(int minutes) {
        energy = Math.max(0, energy - minutes);
        exp += minutes;
        if (exp >= expToNext && level < 10) {
            exp -= expToNext;
            level++;
            expToNext = level * 100;
        }
    }

    public void decay() {
        long now = System.currentTimeMillis();
        long hoursSinceFeed = (now - lastFeedTime) / (1000 * 60 * 60);
        long hoursSincePlay = (now - lastPlayTime) / (1000 * 60 * 60);
        if (hoursSinceFeed > 4) hunger = Math.max(0, hunger - 5);
        if (hoursSincePlay > 6) happiness = Math.max(0, happiness - 3);
        energy = Math.max(0, energy - 2);
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getExp() { return exp; }
    public void setExp(int exp) { this.exp = exp; }
    public int getExpToNext() { return expToNext; }
    public void setExpToNext(int expToNext) { this.expToNext = expToNext; }
    public int getHunger() { return hunger; }
    public void setHunger(int hunger) { this.hunger = hunger; }
    public int getHappiness() { return happiness; }
    public void setHappiness(int happiness) { this.happiness = happiness; }
    public int getEnergy() { return energy; }
    public void setEnergy(int energy) { this.energy = energy; }
    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }
    public long getLastFeedTime() { return lastFeedTime; }
    public void setLastFeedTime(long lastFeedTime) { this.lastFeedTime = lastFeedTime; }
    public long getLastPlayTime() { return lastPlayTime; }
    public void setLastPlayTime(long lastPlayTime) { this.lastPlayTime = lastPlayTime; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
