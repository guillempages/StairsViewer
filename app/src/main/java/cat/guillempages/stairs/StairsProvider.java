package cat.guillempages.stairs;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;

public class StairsProvider extends ContentProvider {

    public static final String _ID = BaseColumns._ID;
    public static final String IR_VALUE = "ir_value";
    public static final String LIGHT_VALUE = "light_value";
    public static final String TEMPERATURE_VALUE = "temperature_value";
    public static final String LIGHT_THRESHOLD = "light_threshold";
    public static final String IR_THRESHOLD = "ir_threshold";
    public static final String TEMPERATURE_THRESHOLD = "temperature_threshold";

    public static final Uri URI = Uri.parse("content://cat.guillempages.stairs");

    private SQLiteOpenHelper mDB = new StairsDatabase(getContext());

    public StairsProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = mDB.getWritableDatabase().delete(StairsDatabase.TABLE_STAIRS,
                selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = mDB.getWritableDatabase().insert(StairsDatabase.TABLE_STAIRS, null, values);
        if (id >= 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return Uri.withAppendedPath(uri, String.valueOf(id));
    }

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor = mDB.getReadableDatabase().query(StairsDatabase.TABLE_STAIRS,
                projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count = mDB.getWritableDatabase().update(StairsDatabase.TABLE_STAIRS,
                values, selection, selectionArgs);
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
}
