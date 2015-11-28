package cat.guillempages.stairs;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>,
        CancellationSignal.OnCancelListener {

    private static final String[] IP_LIST = {"server", "spark1", "spark2", "spark3", "spark4", "spark5",
            "photon1", "photon2", "photon3"};

    private static final int PORT = 2222;
    private static final String TAG = "StairsMainActivity";

    private Spinner mIpSelection;
    private ListView mTable;
    private SpinnerAdapter mHostNamesAdapter;
    private CursorAdapter mStepValuesAdapter;
    private NetworkThread mNetworkThread;
    private ToggleButton mConnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHostNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                IP_LIST);
        mIpSelection = (Spinner) findViewById(R.id.ip_selector);
        mIpSelection.setAdapter(mHostNamesAdapter);
        mTable = (ListView) findViewById(R.id.table);
        mStepValuesAdapter = new StairsAdapter(this);
        mTable.setAdapter(mStepValuesAdapter);

        mConnectButton = (ToggleButton) findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (mConnectButton.isChecked()) {
                    mNetworkThread = new NetworkThread(MainActivity.this,
                            (String) mIpSelection.getSelectedItem(), PORT);
                    mNetworkThread.setOnCancelledListener(MainActivity.this);
                    mNetworkThread.execute();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    if (mNetworkThread != null && !mNetworkThread.isCancelled()) {
                        mNetworkThread.cancel(true);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().initLoader(0, null, this);
        SharedPreferences prefs = getSharedPreferences(getPackageName(),0);
        mIpSelection.setSelection(prefs.getInt("LastConnection", 0));
    }

    @Override
    protected void onPause() {
        getLoaderManager().destroyLoader(0);
        if (mNetworkThread != null && !mNetworkThread.isCancelled()) {
            mNetworkThread.cancel(true);
        }
        onCancel();
        SharedPreferences.Editor prefs = getSharedPreferences(getPackageName(),0).edit();
        prefs.putInt("LastConnection", mIpSelection.getSelectedItemPosition());
        prefs.apply();
        super.onPause();
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        return new CursorLoader(this, StairsProvider.URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        mStepValuesAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mStepValuesAdapter.swapCursor(null);
    }

    @Override
    public void onCancel() {
        Log.d(TAG, "Connection lost");
        mConnectButton.setChecked(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
