package com.example.sssshhift.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.sssshhift.data.ProfileDatabaseHelper;

public class ProfileContentProvider extends ContentProvider {

    private static final String AUTHORITY = "com.example.sssshhift.provider";
    private static final String PATH_PROFILES = "profiles";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + PATH_PROFILES);

    private static final int PROFILES = 100;
    private static final int PROFILE_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, PATH_PROFILES, PROFILES);
        sUriMatcher.addURI(AUTHORITY, PATH_PROFILES + "/#", PROFILE_ID);
    }

    private ProfileDatabaseHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new ProfileDatabaseHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case PROFILES:
                cursor = database.query(ProfileDatabaseHelper.TABLE_PROFILES, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case PROFILE_ID:
                selection = ProfileDatabaseHelper.COLUMN_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(ProfileDatabaseHelper.TABLE_PROFILES, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = database.insert(ProfileDatabaseHelper.TABLE_PROFILES, null, values);

        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, id);
        }
        return null;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsUpdated;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case PROFILES:
                rowsUpdated = database.update(ProfileDatabaseHelper.TABLE_PROFILES, values, selection, selectionArgs);
                break;
            case PROFILE_ID:
                selection = ProfileDatabaseHelper.COLUMN_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsUpdated = database.update(ProfileDatabaseHelper.TABLE_PROFILES, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsDeleted;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case PROFILES:
                rowsDeleted = database.delete(ProfileDatabaseHelper.TABLE_PROFILES, selection, selectionArgs);
                break;
            case PROFILE_ID:
                selection = ProfileDatabaseHelper.COLUMN_ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(ProfileDatabaseHelper.TABLE_PROFILES, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }
}
