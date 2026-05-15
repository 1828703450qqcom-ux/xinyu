package com.xinyu.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.xinyu.app.model.Pet;

public class PetManager {

    private static final String PREFS_NAME = "xinyu_pet";
    private static PetManager instance;
    private final SharedPreferences prefs;

    private PetManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, 0);
    }

    public static synchronized PetManager getInstance(Context context) {
        if (instance == null) {
            instance = new PetManager(context.getApplicationContext());
        }
        return instance;
    }

    public Pet getPet() {
        Pet pet = new Pet();
        pet.setName(prefs.getString("name", "小屿"));
        pet.setLevel(prefs.getInt("level", 1));
        pet.setExp(prefs.getInt("exp", 0));
        pet.setExpToNext(prefs.getInt("exp_to_next", 100));
        pet.setHunger(prefs.getInt("hunger", 80));
        pet.setHappiness(prefs.getInt("happiness", 80));
        pet.setEnergy(prefs.getInt("energy", 80));
        pet.setStage(prefs.getInt("stage", 0));
        pet.setLastFeedTime(prefs.getLong("last_feed_time", System.currentTimeMillis()));
        pet.setLastPlayTime(prefs.getLong("last_play_time", System.currentTimeMillis()));
        pet.setCreatedAt(prefs.getLong("created_at", System.currentTimeMillis()));
        pet.decay();
        return pet;
    }

    public void savePet(Pet pet) {
        prefs.edit()
                .putString("name", pet.getName())
                .putInt("level", pet.getLevel())
                .putInt("exp", pet.getExp())
                .putInt("exp_to_next", pet.getExpToNext())
                .putInt("hunger", pet.getHunger())
                .putInt("happiness", pet.getHappiness())
                .putInt("energy", pet.getEnergy())
                .putInt("stage", pet.getStage())
                .putLong("last_feed_time", pet.getLastFeedTime())
                .putLong("last_play_time", pet.getLastPlayTime())
                .putLong("created_at", pet.getCreatedAt())
                .apply();
    }

    public boolean hasPet() {
        return prefs.contains("name");
    }

    public void resetPet() {
        prefs.edit().clear().apply();
    }
}
