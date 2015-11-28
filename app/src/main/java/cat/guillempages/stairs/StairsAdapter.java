package cat.guillempages.stairs;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ResourceCursorAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Created by guillem on 28/11/2015.
 */
public class StairsAdapter extends ResourceCursorAdapter {

    public StairsAdapter(final Context context) {
        super(context, R.layout.table_row, null, true);
    }

    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        if (cursor != null) {
            final TextView rowIdView = (TextView) view.findViewById(R.id.row_number);
            final TextView irValue = (TextView) view.findViewById(R.id.ir_value);
            final TextView irThreshold = (TextView) view.findViewById(R.id.ir_threshold);
            final TextView lightValue = (TextView) view.findViewById(R.id.light_value);
            final TextView tempValue = (TextView) view.findViewById(R.id.temp_value);

            rowIdView.setText(cursor.getString(cursor.getColumnIndex(StairsProvider._ID)));
            irValue.setText(cursor.getString(cursor.getColumnIndex(StairsProvider.IR_VALUE)));
            irThreshold.setText(cursor.getString(cursor.getColumnIndex(StairsProvider.IR_THRESHOLD)));
            lightValue.setText(cursor.getString(cursor.getColumnIndex(StairsProvider.LIGHT_VALUE)));
            tempValue.setText(cursor.getString(cursor.getColumnIndex(StairsProvider.TEMPERATURE_VALUE)));
        }
    }
}
