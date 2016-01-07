package cat.guillempages.stairs;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Background thread to receive data from a socket.
 * <p/>
 * Created by guillem on 28/11/2015.
 */
public class NetworkThread extends AsyncTask<Void, Integer, Void> {

    public static final String TAG = "StairsNetworkThread";
    private final ContentResolver mResolver;
    private final String mAddress;
    private final int mPort;
    private Socket mSocket;

    private CancellationSignal.OnCancelListener mCancelListener;
    private ModeListener mModeListener;

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
            Log.d(TAG, "Received step count: " + stepCount);
            int mode = stream.read();
            publishProgress(mode);
            Log.d(TAG, "Received current mode: " + mode);
            if (stream.read() < 0) {//newline
                return null;
            }
            ContentValues values = new ContentValues();
            for (int i = 0; i < stepCount; i++) {
                values.put(StairsProvider._ID, i);
                mResolver.insert(StairsProvider.URI, values);
            }
            int value;
            do {
                values.clear();
                int id = stream.read();
                if (id < 0) {
                    break;
                }
                values.put(StairsProvider._ID, id);
                values.put(StairsProvider.IR_VALUE, stream.read());
                values.put(StairsProvider.LIGHT_VALUE, stream.read());
                values.put(StairsProvider.IR_THRESHOLD, stream.read());
                values.put(StairsProvider.LIGHT_THRESHOLD, stream.read());
                value = stream.read(); //newline
                Log.d(TAG, "Received values for row " + id);
                mResolver.update(StairsProvider.URI, values, StairsProvider._ID + "=?",
                        new String[]{String.valueOf(id)});
            } while (!isCancelled() && value >= 0);
            Log.d(TAG, "Exiting socket main thread");
            closeSocket();
        } catch (IOException e) {
            Log.e(TAG, "Could not open socket", e);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(final Integer... values) {
        super.onProgressUpdate(values);
        if (values != null && mModeListener != null) {
            mModeListener.updateMode(values[0]);
        }
    }

    @Override
    protected void onPostExecute(final Void aVoid) {
        super.onPostExecute(aVoid);
        closeSocket();
        if (mCancelListener != null) {
            mCancelListener.onCancel();
        }
    }

    private void closeSocket() {
        if (mSocket != null && !mSocket.isClosed()) {
            try {
                mSocket.close();
                Log.d(TAG, "Disconnected");
            } catch (IOException e) {
                Log.w(TAG, "Could not close socket", e);
            }
        } else {
            Log.d(TAG, "Socket already closed.");
        }
    }

    public void setOnCancelledListener(final CancellationSignal.OnCancelListener listener) {
        mCancelListener = listener;
    }

    public void setModeListener(final ModeListener listener) {
        mModeListener = listener;
    }

    /**
     * Interface for classes that can receive a mode update.
     */
    public interface ModeListener {
        /**
         * Called when the mode has been updated.
         *
         * @param mode The new mode.
         */
        void updateMode(final int mode);
    }
}
