package com.example.sssshhift.usage.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.util.Log;

@Database(entities = {UsageLog.class}, version = 1, exportSchema = false)
@TypeConverters({DateConverter.class})
public abstract class UsageDatabase extends RoomDatabase {
    private static final String TAG = "UsageDatabase";
    private static volatile UsageDatabase INSTANCE;
    public abstract UsageLogDao usageLogDao();

    public static UsageDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (UsageDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            UsageDatabase.class,
                            "usage_database"
                        )
                        .fallbackToDestructiveMigration() // Allow database recreation if no migration found
                        .build();
                        Log.d(TAG, "Database initialized successfully");
                    } catch (Exception e) {
                        Log.e(TAG, "Error initializing database: " + e.getMessage(), e);
                        // If database creation fails, try to delete and recreate
                        context.deleteDatabase("usage_database");
                        INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            UsageDatabase.class,
                            "usage_database"
                        )
                        .fallbackToDestructiveMigration()
                        .build();
                        Log.d(TAG, "Database recreated after error");
                    }
                }
            }
        }
        return INSTANCE;
    }
} 