package com.xinyu.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.xinyu.app.model.MoodRecord;
import com.xinyu.app.model.Note;
import com.xinyu.app.model.TestResult;
import com.xinyu.app.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "xinyu.db";
    private static final int DATABASE_VERSION = 2;

    private static AppDatabase instance;
    private final Context context;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_MOODS = "moods";
    private static final String TABLE_NOTES = "notes";
    private static final String TABLE_TEST_RESULTS = "test_results";
    private static final String TABLE_CHECKINS = "checkins";
    private static final String TABLE_CONTACTS = "contacts";

    // Common column
    private static final String COL_USERNAME = "username";

    // Users columns
    private static final String COL_PASSWORD = "password";
    private static final String COL_NICKNAME = "nickname";
    private static final String COL_GENDER = "gender";
    private static final String COL_CREATED_AT = "created_at";

    // Moods columns
    private static final String COL_ID = "id";
    private static final String COL_MOOD_VALUE = "mood_value";
    private static final String COL_MOOD_LABEL = "mood_label";
    private static final String COL_EMOJI = "emoji";
    private static final String COL_NOTE = "note";
    private static final String COL_HEART_RATE = "heart_rate";

    // Notes columns
    private static final String COL_TITLE = "title";
    private static final String COL_CONTENT = "content";
    private static final String COL_UPDATED_AT = "updated_at";

    // Test results columns
    private static final String COL_TEST_ID = "test_id";
    private static final String COL_TEST_TITLE = "test_title";
    private static final String COL_SCORE = "score";
    private static final String COL_LEVEL = "level";
    private static final String COL_DESCRIPTION = "description";

    // Checkins columns
    private static final String COL_DAY = "day";

    // Contacts columns
    private static final String COL_NAME = "name";
    private static final String COL_PHONE = "phone";
    private static final String COL_RELATION = "relation";

    private AppDatabase(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new AppDatabase(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USERNAME + " TEXT PRIMARY KEY, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_NICKNAME + " TEXT, " +
                COL_GENDER + " TEXT, " +
                "avatar TEXT DEFAULT '', " +
                COL_CREATED_AT + " INTEGER)";

        String createMoods = "CREATE TABLE " + TABLE_MOODS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT NOT NULL, " +
                COL_MOOD_VALUE + " INTEGER, " +
                COL_MOOD_LABEL + " TEXT, " +
                COL_EMOJI + " TEXT, " +
                COL_NOTE + " TEXT, " +
                COL_HEART_RATE + " INTEGER DEFAULT -1, " +
                COL_CREATED_AT + " INTEGER)";

        String createNotes = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT NOT NULL, " +
                COL_TITLE + " TEXT, " +
                COL_CONTENT + " TEXT, " +
                COL_CREATED_AT + " INTEGER, " +
                COL_UPDATED_AT + " INTEGER)";

        String createTestResults = "CREATE TABLE " + TABLE_TEST_RESULTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT NOT NULL, " +
                COL_TEST_ID + " TEXT, " +
                COL_TEST_TITLE + " TEXT, " +
                COL_SCORE + " INTEGER, " +
                COL_LEVEL + " TEXT, " +
                COL_DESCRIPTION + " TEXT, " +
                COL_CREATED_AT + " INTEGER)";

        String createCheckins = "CREATE TABLE " + TABLE_CHECKINS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT NOT NULL, " +
                COL_DAY + " TEXT, " +
                COL_CREATED_AT + " INTEGER)";

        String createContacts = "CREATE TABLE " + TABLE_CONTACTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT NOT NULL, " +
                COL_NAME + " TEXT, " +
                COL_PHONE + " TEXT, " +
                COL_RELATION + " TEXT)";

        db.execSQL(createUsers);
        db.execSQL(createMoods);
        db.execSQL(createNotes);
        db.execSQL(createTestResults);
        db.execSQL(createCheckins);
        db.execSQL(createContacts);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add avatar column to users table
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN avatar TEXT DEFAULT ''");
            } catch (Exception e) {
                // Column already exists
            }
        }
    }

    // ==================== User Methods ====================

    public User authenticate(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                null,
                COL_USERNAME + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{username, password},
                null, null, null
        );

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    public void registerUser(User user) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, user.getUsername());
        values.put(COL_PASSWORD, user.getPassword());
        values.put(COL_NICKNAME, user.getNickname());
        values.put(COL_GENDER, user.getGender());
        values.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
        values.put(COL_CREATED_AT, user.getCreatedAt());
        db.insert(TABLE_USERS, null, values);
    }

    public User getUser(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USERS,
                null,
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            user = cursorToUser(cursor);
            cursor.close();
        }
        return user;
    }

    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)));
        user.setPassword(cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)));
        user.setNickname(cursor.getString(cursor.getColumnIndexOrThrow(COL_NICKNAME)));
        user.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COL_GENDER)));
        user.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)));
        try {
            user.setAvatar(cursor.getString(cursor.getColumnIndexOrThrow("avatar")));
        } catch (Exception e) {
            user.setAvatar("");
        }
        return user;
    }

    // ==================== Mood Methods ====================

    public void saveMood(MoodRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, record.getUsername());
        values.put(COL_MOOD_VALUE, record.getMoodValue());
        values.put(COL_MOOD_LABEL, record.getMoodLabel());
        values.put(COL_EMOJI, record.getEmoji());
        values.put(COL_NOTE, record.getNote());
        values.put(COL_HEART_RATE, record.getHeartRate());
        values.put(COL_CREATED_AT, record.getCreatedAt());
        record.setId(db.insert(TABLE_MOODS, null, values));
    }

    public List<MoodRecord> getMoods(String username, int limit) {
        SQLiteDatabase db = getReadableDatabase();
        List<MoodRecord> moods = new ArrayList<>();

        Cursor cursor = db.query(
                TABLE_MOODS,
                null,
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null,
                COL_CREATED_AT + " DESC",
                String.valueOf(limit)
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                moods.add(cursorToMoodRecord(cursor));
            }
            cursor.close();
        }
        return moods;
    }

    public MoodRecord getTodayMood(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_MOODS +
                        " WHERE " + COL_USERNAME + " = ?" +
                        " AND strftime('%Y-%m-%d', " + COL_CREATED_AT + " / 1000, 'unixepoch', 'localtime') = ?" +
                        " ORDER BY " + COL_CREATED_AT + " ASC LIMIT 1",
                new String[]{username, today}
        );

        MoodRecord mood = null;
        if (cursor != null && cursor.moveToFirst()) {
            mood = cursorToMoodRecord(cursor);
            cursor.close();
        }
        return mood;
    }

    private MoodRecord cursorToMoodRecord(Cursor cursor) {
        MoodRecord record = new MoodRecord();
        record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        record.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)));
        record.setMoodValue(cursor.getInt(cursor.getColumnIndexOrThrow(COL_MOOD_VALUE)));
        record.setMoodLabel(cursor.getString(cursor.getColumnIndexOrThrow(COL_MOOD_LABEL)));
        record.setEmoji(cursor.getString(cursor.getColumnIndexOrThrow(COL_EMOJI)));
        record.setNote(cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)));
        record.setHeartRate(cursor.getInt(cursor.getColumnIndexOrThrow(COL_HEART_RATE)));
        record.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)));
        return record;
    }

    // ==================== Note Methods ====================

    public void saveNote(Note note) {
        SQLiteDatabase db = getWritableDatabase();
        note.setUpdatedAt(System.currentTimeMillis());

        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, note.getUsername());
        values.put(COL_TITLE, note.getTitle());
        values.put(COL_CONTENT, note.getContent());
        values.put(COL_CREATED_AT, note.getCreatedAt());
        values.put(COL_UPDATED_AT, note.getUpdatedAt());

        if (note.getId() > 0) {
            // Update existing note
            db.update(
                    TABLE_NOTES,
                    values,
                    COL_ID + " = ?",
                    new String[]{String.valueOf(note.getId())}
            );
        } else {
            // Insert new note
            note.setCreatedAt(System.currentTimeMillis());
            values.put(COL_CREATED_AT, note.getCreatedAt());
            long id = db.insert(TABLE_NOTES, null, values);
            note.setId(id);
        }
    }

    public List<Note> getNotes(String username) {
        SQLiteDatabase db = getReadableDatabase();
        List<Note> notes = new ArrayList<>();

        Cursor cursor = db.query(
                TABLE_NOTES,
                null,
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null,
                COL_UPDATED_AT + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                notes.add(cursorToNote(cursor));
            }
            cursor.close();
        }
        return notes;
    }

    public Note getNote(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_NOTES,
                null,
                COL_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        Note note = null;
        if (cursor != null && cursor.moveToFirst()) {
            note = cursorToNote(cursor);
            cursor.close();
        }
        return note;
    }

    public void deleteNote(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NOTES, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    private Note cursorToNote(Cursor cursor) {
        Note note = new Note();
        note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        note.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)));
        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
        note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)));
        note.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)));
        note.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT)));
        return note;
    }

    // ==================== Test Result Methods ====================

    public void saveTestResult(TestResult result) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, result.getUsername());
        values.put(COL_TEST_ID, result.getTestId());
        values.put(COL_TEST_TITLE, result.getTestTitle());
        values.put(COL_SCORE, result.getScore());
        values.put(COL_LEVEL, result.getLevel());
        values.put(COL_DESCRIPTION, result.getDescription());
        values.put(COL_CREATED_AT, result.getCreatedAt());
        result.setId(db.insert(TABLE_TEST_RESULTS, null, values));
    }

    public List<TestResult> getTestResults(String username, int limit) {
        SQLiteDatabase db = getReadableDatabase();
        List<TestResult> results = new ArrayList<>();

        Cursor cursor = db.query(
                TABLE_TEST_RESULTS,
                null,
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null,
                COL_CREATED_AT + " DESC",
                String.valueOf(limit)
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                results.add(cursorToTestResult(cursor));
            }
            cursor.close();
        }
        return results;
    }

    public int getTestCount(String username, String testId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_TEST_RESULTS +
                        " WHERE " + COL_USERNAME + " = ? AND " + COL_TEST_ID + " = ?",
                new String[]{username, testId}
        );

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private TestResult cursorToTestResult(Cursor cursor) {
        TestResult result = new TestResult();
        result.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        result.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)));
        result.setTestId(cursor.getString(cursor.getColumnIndexOrThrow(COL_TEST_ID)));
        result.setTestTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TEST_TITLE)));
        result.setScore(cursor.getInt(cursor.getColumnIndexOrThrow(COL_SCORE)));
        result.setLevel(cursor.getString(cursor.getColumnIndexOrThrow(COL_LEVEL)));
        result.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COL_DESCRIPTION)));
        result.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED_AT)));
        return result;
    }

    // ==================== Checkin Methods ====================

    public void addCheckin(String username, String day) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_DAY, day);
        values.put(COL_CREATED_AT, System.currentTimeMillis());
        db.insert(TABLE_CHECKINS, null, values);
    }

    public boolean hasCheckinToday(String username) {
        SQLiteDatabase db = getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_CHECKINS +
                        " WHERE " + COL_USERNAME + " = ? AND " + COL_DAY + " = ?",
                new String[]{username, today}
        );

        boolean hasCheckin = false;
        if (cursor != null && cursor.moveToFirst()) {
            hasCheckin = cursor.getInt(0) > 0;
            cursor.close();
        }
        return hasCheckin;
    }

    public int getCheckinCount(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_CHECKINS +
                        " WHERE " + COL_USERNAME + " = ?",
                new String[]{username}
        );

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // ==================== Contact Methods ====================

    public void addContact(String username, String name, String phone, String relation) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_NAME, name);
        values.put(COL_PHONE, phone);
        values.put(COL_RELATION, relation);
        db.insert(TABLE_CONTACTS, null, values);
    }

    public List<Map<String, String>> getContacts(String username) {
        SQLiteDatabase db = getReadableDatabase();
        List<Map<String, String>> contacts = new ArrayList<>();

        Cursor cursor = db.query(
                TABLE_CONTACTS,
                null,
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null, null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                Map<String, String> contact = new HashMap<>();
                contact.put("id", String.valueOf(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID))));
                contact.put("name", cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
                contact.put("phone", cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE)));
                contact.put("relation", cursor.getString(cursor.getColumnIndexOrThrow(COL_RELATION)));
                contacts.add(contact);
            }
            cursor.close();
        }
        return contacts;
    }

    public void deleteContact(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_CONTACTS, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // ==================== User Profile Update ====================

    public void updateUserProfile(String username, String nickname, String avatar) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NICKNAME, nickname);
        values.put("avatar", avatar);
        db.update(TABLE_USERS, values, COL_USERNAME + " = ?", new String[]{username});
    }
}
