package com.nc.check.bt.sample.testbtconnection;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.widget.*;

import com.nc.check.bt.sample.testbtconnection.bluetooth.*;

import java.util.List;

public class BaseBluetoothActivity extends AppCompatActivity {

    protected BtController btController;
    private RadioGroup mAddressesView;


    private static final String TAG = BaseBluetoothActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btController = new BtControllerImpl();
        setupAddress();
    }

    protected void writeReceivedMessage(String strIncom) {
        Log.i("BaseBluetoothActivity", "received: " + strIncom);
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
                        writeReceivedMessage(strIncom);
                        break;
                    case BtConstants.MESSAGE_STATE_CHANGE:
                        Log.d(TAG, "new state: " + msg.what);
                        break;
                }
            }
        };
    }

    private void setupAddress() {
        mAddressesView = findViewById(R.id.devices);
        updateAddresses();
    }

    protected void updateAddresses() {
        mAddressesView.removeAllViews();
        List<Pair<String, String>> macAddresses = btController.getDevicesAddresses();
        for (Pair<String, String> address : macAddresses) {
            addSelector(mAddressesView, address.first, address.second);
        }
    }

    private void addSelector(RadioGroup radioGroup, String name, String address) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(name + " : " + address);
        radioButton.setTag(address);
        radioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
                if (checked) {
                    btController.setSelectedMacAddress((String)buttonView.getTag());

                }
            }
        });
        radioGroup.addView(radioButton, radioGroup.getChildCount());
    }

    @Override
    protected void onResume() {
        super.onResume();
        btController.setHandler(getHandler());
    }

    @Override
    public void onPause() {
        super.onPause();
        btController.stop();
        btController.setHandler(null);
    }
}
