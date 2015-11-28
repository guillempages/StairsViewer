package cat.guillempages.stairs;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by guillem on 28/11/2015.
 */
public class NetworkThread extends AsyncTask<Void, Void, Void> {

    public static final String TAG = "StairsNetworkThread";
    private final ContentResolver mResolver;
    private final String mAddress;
    private final int mPort;
    private Socket mSocket;

    private CancellationSignal.OnCancelListener mCancelListener;

    NetworkThread(final Context context, final String address, final int port) {
        mResolver = context.getContentResolver();
        mAddress = address;
        mPort = port;

        Log.d(TAG, "Created new socket for " + address + ":" + port);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d(TAG, "preexecute.");
    }

    @Override
    protected Void doInBackground(final Void... params) {
        Log.d(TAG, "doInBackground");
        try {
            mSocket = new Socket(mAddress, mPort);
            if (!mSocket.isConnected()) {
                Log.w(TAG, "Could not connect");
                return null;
            }
            Log.d(TAG, "Connected to: " + mSocket.getInetAddress());
            //Clear the table on connection
            mResolver.delete(StairsProvider.URI, null, null);

            InputStream stream = mSocket.getInputStream();
            int stepCount = stream.read();
            stream.read(); //newline
            Log.d(TAG, "Received step count: " + stepCount);
            ContentValues values = new ContentValues();
            for (int i = 0; i < stepCount; i++) {
                values.put(StairsProvider._ID, i);
                mResolver.insert(StairsProvider.URI, values);
            }
            int id;
            do {
                values.clear();
                id = stream.read();
                values.put(StairsProvider._ID, id);
                values.put(StairsProvider.IR_VALUE, stream.read());
                values.put(StairsProvider.LIGHT_VALUE, stream.read());
                values.put(StairsProvider.IR_THRESHOLD, stream.read());
                values.put(StairsProvider.LIGHT_THRESHOLD, stream.read());
                stream.read(); //newline
                Log.d(TAG, "Received values for row " + id);
                mResolver.update(StairsProvider.URI, values, StairsProvider._ID + "=?",
                        new String[]{String.valueOf(id)});
            } while (!isCancelled() && id >= 0);
            Log.d(TAG, "Exiting socket main thread");
        } catch (IOException e) {
            Log.e(TAG, "Could not open socket", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Void aVoid) {
        super.onPostExecute(aVoid);
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
                Log.d(TAG, "Disconnected");
            } catch (IOException e) {
                Log.w(TAG, "Could not close socket", e);
            }
        }
        if (mCancelListener != null) {
            mCancelListener.onCancel();
        }
    }

    public void setOnCancelledListener(CancellationSignal.OnCancelListener listener) {
        mCancelListener = listener;
    }
}
