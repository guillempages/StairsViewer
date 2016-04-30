package cat.guillempages.stairs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements LoaderManager.LoaderCallbacks<Cursor>,
        CancellationSignal.OnCancelListener, NetworkThread.ModeListener {

    public static final byte NET_CMD_CHANGE_MODE = 0x10;
    public static final byte NET_CMD_DISCONNECT = 0x20;
    public static final byte NET_END_OF_COMMAND = '\n';

    private static final int PORT = 2222;
    private static final String TAG = "StairsMainActivity";
    private Spinner mIpSelection;
    private ListView mTable;
    private SpinnerAdapter mHostNamesAdapter;
    private CursorAdapter mStepValuesAdapter;
    private NetworkThread mNetworkThread;
    private ToggleButton mConnectButton;
    private TextView mModeButton;
    private int mCurrentMode = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHostNamesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.server_list));
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
                    mNetworkThread.setModeListener(MainActivity.this);
                    mNetworkThread.execute();
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    if (mNetworkThread != null && !mNetworkThread.isCancelled()) {
                        Log.d(TAG, "Cancel network thread");
                        mNetworkThread.cancel(true);
                    }
                }
            }
        });

        mModeButton = (TextView) findViewById(R.id.mode_button);
        mModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setTitle(R.string.mode_dialog_title);
                dialogBuilder.setSingleChoiceItems
                        (getResources().getStringArray(R.array.remote_modes), mCurrentMode,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int mode) {
                                        Log.d(TAG, "New Mode requested: " + mode);
                                        mNetworkThread.write(new byte[] {
                                                NET_CMD_CHANGE_MODE, (byte) mode, NET_END_OF_COMMAND
                                        });
                                        dialog.dismiss();
                                    }
                                });
                dialogBuilder.create().show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().initLoader(0, null, this);
        SharedPreferences prefs = getSharedPreferences(getPackageName(), 0);
        mIpSelection.setSelection(prefs.getInt("LastConnection", 0));
    }

    @Override
    protected void onPause() {
        getLoaderManager().destroyLoader(0);
        if (mNetworkThread != null && !mNetworkThread.isCancelled()) {
            mNetworkThread.cancel(true);
        }
        onCancel();
        SharedPreferences.Editor prefs = getSharedPreferences(getPackageName(), 0).edit();
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
        mModeButton.setVisibility(View.INVISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void updateMode(final int newMode) {
        int mode = newMode;
        /*
         Convert "ASCII modes" to normal modes. If the mode is sent as an ascii number, convert it.
        */
        if (mode >= '0' && mode <= '9') {
            mode -= '0';
        }
        Log.d(TAG, "Updating mode to " + mode);
        mCurrentMode = mode;
        final String[] remoteModes = getResources().getStringArray(R.array.remote_modes);

        if (mode >= 0 && mode < remoteModes.length) {
            mModeButton.setVisibility(View.VISIBLE);
            mModeButton.setText(remoteModes[mode]);
        } else {
            mModeButton.setVisibility(View.INVISIBLE);
        }
    }
}
