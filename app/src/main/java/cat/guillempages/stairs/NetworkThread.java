package cat.guillempages.stairs;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private final ConcurrentLinkedQueue<String> mQueue = new ConcurrentLinkedQueue<>();
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

            final InputStream readStream = mSocket.getInputStream();
            final OutputStream writeStream = mSocket.getOutputStream();
            int stepCount = readStream.read();
            Log.d(TAG, "Received step count: " + stepCount);
            int mode = readStream.read();
            publishProgress(mode);
            Log.d(TAG, "Received current mode: " + mode);
            if (readStream.read() < 0) {//newline
                return null;
            }
            ContentValues values = new ContentValues();
            for (int i = 0; i < stepCount; i++) {
                values.put(StairsProvider._ID, i);
                mResolver.insert(StairsProvider.URI, values);
            }
            do {
                values.clear();
                if (!mSocket.isConnected() || mSocket.isClosed()) {
                    break;
                }
                if (!mQueue.isEmpty()) {

                    final byte[] bytesToWrite = mQueue.poll().getBytes();
                    writeStream.write(bytesToWrite);
                    Log.d(TAG, "Wrote: " + Arrays.toString(bytesToWrite));
                }
                if (readStream.available() > 0) {
                    int id = readStream.read();
                    if (id < 0) {
                        break;
                    }
                    values.put(StairsProvider._ID, id);
                    values.put(StairsProvider.IR_VALUE, readStream.read());
                    values.put(StairsProvider.LIGHT_VALUE, readStream.read());
                    values.put(StairsProvider.IR_THRESHOLD, readStream.read());
                    values.put(StairsProvider.LIGHT_THRESHOLD, readStream.read());
                    readStream.read(); //newline
                    Log.v(TAG, "Received values for row " + id);
                    mResolver.update(StairsProvider.URI, values, StairsProvider._ID + "=?",
                            new String[]{String.valueOf(id)});
                }
            } while (!isCancelled());
            Log.d(TAG, "Exiting socket main thread");
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
    }

    @Override
    protected void onCancelled(Void aVoid) {
        closeSocket();
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
        if (mCancelListener != null) {
            mCancelListener.onCancel();
        }
    }

    public void setOnCancelledListener(final CancellationSignal.OnCancelListener listener) {
        mCancelListener = listener;
    }

    public void setModeListener(final ModeListener listener) {
        mModeListener = listener;
    }

    public void write(final byte[] byteArray) {
        write(new String(byteArray));
    }

    public void write(final String text) {
        mQueue.add(text);
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
