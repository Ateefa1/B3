package com.example.b1;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";
    private static final String appName = "MY BLUETOOTH APP";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "Accept Thread : Setting up server using" + MY_UUID_INSECURE);
            } catch (IOException e) {

            }
            mmServerSocket = tmp;

        }

        public void run() {
            Log.d(TAG, "run : Accept thread running");
            BluetoothSocket socket = null;
            try {
                Log.d(TAG, "run : RFcom server Socket Starts");
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Accept Thread : IOException" + e.getMessage());
            }
            if (socket != null) {
                connected(socket,mmDevice);
            }
            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel : Cancelling Accept Thread");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel : close of accept thread ServerSocket failed" + e.getMessage());
            }
        }

    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread : Started ");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.i(TAG, "run mConnectThread");
            try {
                Log.d(TAG, "ConnectThread : trying to create insecureRFcommSocket using UUID:" + MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread : could not create insecureRFcommSocket using UUID:" + e.getMessage());

            }
            mmSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                Log.d(TAG, "RUN: ConnectThread connected");
            } catch (IOException e) {
                try {
                    mmSocket.close();
                    Log.d(TAG, "run : close socket");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: unable to close connection in socket" + e1.getMessage());
                }
                Log.d(TAG, "run : ConnectThread : could not connect to UUID" + MY_UUID_INSECURE);
            }
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                Log.d(TAG, "cancel closing client");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel : close() of mmSocket in ConnectThread failed" + e.getMessage());
            }
        }
    }


    public synchronized void start() {
        Log.d(TAG, "start");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient : started ");
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                mProgressDialog.dismiss();
            }catch (NullPointerException e) {
                e.printStackTrace();
            }

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String incommingMessgae = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream :" + incommingMessgae);
                } catch (IOException e) {
                    Log.e(TAG,"write: Error reading inputstream" + e.getMessage());
                    break;
                }
            }
        }
        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG,"Write: Writing to outputstream : " + text);
            try {
                mmOutStream.write(bytes);
            }catch (IOException e){
                Log.e(TAG,"write: Error writing to outputstream" + e.getMessage());
            }

        }
        public void cancel() {
            try{
                mmSocket.close();
            }catch (IOException e) {}
        }
    }
    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG,"connected: String");
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectThread.start();
    }

    public void write(byte[] out) {
        ConnectedThread r;
        Log.d(TAG, "write: Write Called");
        mConnectedThread.write(out);
    }

}