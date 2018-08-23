package com.nc.check.bt.sample.testbtconnection.bluetooth;

import android.bluetooth.*;
import android.os.*;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import java.io.*;
import java.util.*;

import static com.nc.check.bt.sample.testbtconnection.bluetooth.BtConstants.STATE_CONNECTED;
import static com.nc.check.bt.sample.testbtconnection.bluetooth.BtConstants.STATE_CONNECTING;
import static com.nc.check.bt.sample.testbtconnection.bluetooth.BtConstants.STATE_LISTEN;
import static com.nc.check.bt.sample.testbtconnection.bluetooth.BtConstants.STATE_NONE;

public class BtControllerImpl implements BtController {

    private static final String TAG = "+++++"; //BtController.class.getSimpleName();
    private static final String NAME_SERVICE = "BluetoothSecure";

    private BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private Handler mHandler;
    private int mState;
    private String mSelectedMacAddress = null;

    // SPP UUID сервиса
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BtControllerImpl() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    @Override
    public List<Pair<String, String>> getDevicesAddresses() {
        ArrayList<Pair<String, String>> pairs = new ArrayList<>();
        if (checkBTState()) {
            Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
            for (BluetoothDevice bluetoothDevice : bondedDevices) {
                pairs.add(new Pair<>(bluetoothDevice.getName(), bluetoothDevice.getAddress()));
            }
        }
        return pairs;
    }

    private boolean checkBTState() {
        if (mAdapter == null) {
            Log.e("Fatal Error", "Bluetooth не поддерживается");
            return false;
        }
        return mAdapter.isEnabled();
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @Override
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
            Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_CONNECTED);

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        if (mHandler != null) {
            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(BtConstants.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(BtConstants.DEVICE_NAME, device.getName());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    }

    /**
     * Start the ConnectThread to initiate a connection for preset remote device.
     */
    @Override
    public synchronized void connect() {
        if (mSelectedMacAddress != null) {
            connect(mSelectedMacAddress);
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param deviceAddress The BluetoothDevice to connect
     */
    @Override
    public synchronized void connect(String deviceAddress) {
        BluetoothDevice device = mAdapter.getRemoteDevice(deviceAddress);
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    /**
     * Stop all threads
     */
    @Override
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /* Call this from the main activity to send data to the remote device */
    @Override
    public void write(String message) {
        Log.d(TAG, "Text to send: " + message);
        write(message.getBytes());
    }
    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    @Override
    public void setSelectedMacAddress(String selectedMacAddress) {
        mSelectedMacAddress = selectedMacAddress;
    }

    public void setHandler(Handler handler) {
        this.mHandler = handler;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        if (mHandler != null) {

            // Send a failure message back to the Activity
            Message msg = mHandler.obtainMessage(BtConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(BtConstants.TOAST, "Unable to connect device");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        setState(STATE_NONE);

        // Start the service over to restart listening mode
        BtControllerImpl.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        if (mHandler != null) {
            // Send a failure message back to the Activity
            Message msg = mHandler.obtainMessage(BtConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(BtConstants.TOAST, "Device connection was lost");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        setState(STATE_NONE);

        // Start the service over to restart listening mode
        BtControllerImpl.this.start();
    }

    private void setState(int state) {
        Log.d(TAG, "Socket BEGIN mAcceptThread" + this);
        mState = state;
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage(BtConstants.MESSAGE_STATE_CHANGE);
            msg.what = state;
            mHandler.sendMessage(msg);
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SERVICE, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket listen() failed", e);
            }
            mmServerSocket = tmp;
            setState(STATE_LISTEN);
        }

        public void run() {
            Log.d(TAG, "Socket BEGIN mAcceptThread" + this);
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "mAcceptThread accept passed");
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BtControllerImpl.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                Log.d(TAG, "mAcceptThread connecting");
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                Log.d(TAG, "mAcceptThread connected");
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket");

        }

        public void cancel() {
            Log.d(TAG, "Socket cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
            setState(STATE_CONNECTING);
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
                Log.i(TAG, "mConnectThread connect passed");
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BtControllerImpl.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            setState(STATE_CONNECTED);
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    if (bytes > 0) {
                        if (mHandler != null) {

                            // Send the obtained bytes to the UI Activity
                            mHandler.obtainMessage(BtConstants.MESSAGE_READ, bytes, -1, buffer)
                                    .sendToTarget();
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                if (mHandler != null) {
                    // Share the sent message back to the UI Activity
                    mHandler.obtainMessage(BtConstants.MESSAGE_WRITE, -1, -1, buffer)
                            .sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
