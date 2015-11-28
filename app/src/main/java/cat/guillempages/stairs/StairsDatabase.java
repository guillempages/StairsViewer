package cat.guillempages.stairs;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by guillem on 28/11/2015.
 */
class StairsDatabase extends SQLiteOpenHelper {
    static final String TABLE_STAIRS = "stairsTable";

    private final static int DB_VERSION = 1;

    public StairsDatabase(final Context context) {
        super(context, null, null, DB_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {
        createStairsTable(db);
//        fillDummyValues(db);
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        if (oldVersion != newVersion) {
            onCreate(db);
        }
    }

    private void createStairsTable(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_STAIRS + " ("
                + StairsProvider._ID + " INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", " + StairsProvider.IR_VALUE + " INTEGER"
                + ", " + StairsProvider.IR_THRESHOLD + " INTEGER"
                + ", " + StairsProvider.LIGHT_VALUE + " INTEGER"
                + ", " + StairsProvider.LIGHT_THRESHOLD + " INTEGER"
                + ", " + StairsProvider.TEMPERATURE_VALUE + " INTEGER"
                + ", " + StairsProvider.TEMPERATURE_THRESHOLD + " INTEGER"
                + ")");
    }

    private void fillDummyValues(final SQLiteDatabase db) {
        for (int i = 10; i < 100; i += 10) {
            insertRow(db, i + 1, i + 2, i + 3, i + 4, i + 5, i + 6);
        }
    }

    private long insertRow(final SQLiteDatabase db, final int irValue, final int irThreshold,
                           final int lightValue, final int lightThreshold,
                           final int temperatureValue, final int temperatureThreshold) {
        ContentValues values = new ContentValues();
        values.put(StairsProvider.IR_VALUE, irValue);
        values.put(StairsProvider.IR_THRESHOLD, irThreshold);
        values.put(StairsProvider.LIGHT_VALUE, lightValue);
        values.put(StairsProvider.LIGHT_THRESHOLD, lightThreshold);
        values.put(StairsProvider.TEMPERATURE_VALUE, temperatureValue);
        values.put(StairsProvider.TEMPERATURE_THRESHOLD, temperatureThreshold);
        return db.insert(TABLE_STAIRS, null, values);
    }
}
