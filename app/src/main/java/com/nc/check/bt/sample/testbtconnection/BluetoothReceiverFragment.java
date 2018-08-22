package com.nc.check.bt.sample.testbtconnection;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.nc.check.bt.sample.testbtconnection.bluetooth.BtConstants;
import com.nc.check.bt.sample.testbtconnection.bluetooth.BtController;

/**
 * A placeholder fragment containing a simple view.
 */
public class BluetoothReceiverFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private BtController mBtController;
    private TextView mTextView;
    private View mButtonView;

    private static final String TAG = "+++++"; //BtController.class.getSimpleName();

    public BluetoothReceiverFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static BluetoothReceiverFragment newInstance() {
        return new BluetoothReceiverFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mBtController = new BtController(getHandler());

        View rootView = inflater.inflate(R.layout.fragment_receiver, container, false);
        setupView(rootView);
        return rootView;
    }

    private void setupView(View rootView) {
        mTextView = rootView.findViewById(R.id.label_data);
        Button start = rootView.findViewById(R.id.button_start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtController.start();
            }
        });
        mButtonView = rootView.findViewById(R.id.button_connect);
        mButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtController.connect("88:83:22:F7:F5:8B");
            }
        });
    }

    private Handler getHandler() {
        return new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case BtConstants.MESSAGE_READ:
                        if (msg.obj == null) {
                            break;
                        }
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);
                        Log.d(TAG, "Handle message " + strIncom);
                        mTextView.setText(mTextView.getText() + "received: " + strIncom + "\n");
                        break;
                    case BtConstants.MESSAGE_STATE_CHANGE:
                        Log.d(TAG, "new state: " + msg.what);
                        mTextView.setText(mTextView.getText() + "new state: " + msg.what + "\n");
                }
            }

            ;
        };
    }


    @Override
    public void onPause() {
        super.onPause();
        mBtController.stop();
    }
}
