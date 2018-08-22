package com.nc.check.bt.sample.testbtconnection;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.*;

import com.nc.check.bt.sample.testbtconnection.bluetooth.BtConstants;
import com.nc.check.bt.sample.testbtconnection.bluetooth.BtController;

import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class BluetoothControllerFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private BtController mBtController;
    private EditText mEtCommand;
    private RadioGroup mAddressesView;
    private TextView mBitCommandLabel;
    private TextView mTextView;

    private int mBitCommand = 0;

    private static final String TAG = "+++++"; //BtController.class.getSimpleName();
    private String macAddress;

    public BluetoothControllerFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static BluetoothControllerFragment newInstance() {
        return new BluetoothControllerFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBtController = new BtController(getHandler());

        View rootView = inflater.inflate(R.layout.fragment_controller, container, false);
        setupAddress(rootView);
        setupSendCommand(rootView);
        setupSendBits(rootView);
        mTextView = rootView.findViewById(R.id.label_data);
        return rootView;
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
        };
    }

    private void setupAddress(View rootView) {
        mAddressesView = rootView.findViewById(R.id.rbg_mac);
        Button btnAccept = rootView.findViewById(R.id.btn_accept);
        updateAddresses();
        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtController.start();
            }
        });
        Button btnStop = rootView.findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtController.stop();
            }
        });
    }

    private void updateAddresses() {
        mAddressesView.removeAllViews();
        List<Pair<String, String>> macAddresses = mBtController.getDevicesAddresses();
        for (Pair<String, String> address : macAddresses) {
            addSelector(mAddressesView, address.first, address.second);
        }
    }

    private void addSelector(RadioGroup radioGroup, String name, String address) {
        RadioButton radioButton = new RadioButton(getContext());
        radioButton.setText(name + " : " + address);
        radioButton.setTag(address);
        radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                if (checked) {
                    String macAddress = (String)buttonView.getTag();
                    updateDevice(macAddress);
                }
            }
        });
        radioGroup.addView(radioButton, radioGroup.getChildCount());
    }

    private void updateDevice(String macAddress) {
        mBtController.connect(macAddress);
    }

    private void setupSendCommand(View rootView) {
        Button btnSend =  rootView.findViewById(R.id.button_send);
        mEtCommand = rootView.findViewById(R.id.et_cmd);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtController.write(mEtCommand.getText().toString());
            }
        });
    }

    private void setupSendBits(View rootView) {
        mBitCommandLabel = rootView.findViewById(R.id.label_bits_cmd);
        int [] ids = {R.id.cb_1, R.id.cb_2, R.id.cb_3, R.id.cb_4, R.id.cb_5, R.id.cb_6, R.id.cb_7, R.id.cb_8};

        for (int i = 0 ; i < ids.length ; i++) {
            CheckBox checkBox = rootView.findViewById(ids[i]);
            checkBox.setTag(ids.length - 1 - i);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int shift = (Integer)buttonView.getTag();
                    if (isChecked) {
                        mBitCommand |= 1 << shift;
                    } else {
                        mBitCommand &= ~1 << shift;
                    }
                    mBitCommandLabel.setText(String.valueOf(mBitCommand));
                }
            });
        }

        Button btnSendBits = rootView.findViewById(R.id.btn_send_bits);
        btnSendBits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBtController.write(String.valueOf(mBitCommand));
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mBtController.stop();
    }
}
