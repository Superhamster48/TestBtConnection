package com.nc.check.bt.sample.testbtconnection;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class MainActivity extends BaseBluetoothActivity {

    private EditText mEtCommand;
    private TextView mBitCommandLabel;
    private int mBitCommand = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupSendBits();
    }

    protected void writeReceivedMessage(String strIncom) {
        TextView textView = findViewById(R.id.label_data);
        textView.setText(textView.getText() + "received: " + strIncom + "\n");
        Toast.makeText(MainActivity.this, "Received: " + strIncom, Toast.LENGTH_LONG);
    }

    private void setupSendBits() {
        mBitCommandLabel = findViewById(R.id.label_bits_cmd);
        int [] ids = {R.id.cb_1, R.id.cb_2, R.id.cb_3, R.id.cb_4, R.id.cb_5, R.id.cb_6, R.id.cb_7, R.id.cb_8};

        for (int i = 0 ; i < ids.length ; i++) {
            CheckBox checkBox = findViewById(ids[i]);
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

        Button btnSendBits = findViewById(R.id.btn_send_bits);
        btnSendBits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btController.write(String.valueOf(mBitCommand));
            }
        });
    }

    public void stop(View view) {
        btController.stop();
    }

    public void connect(View view) {
        btController.connect();
    }

    public void start(View view) {
        btController.start();
    }

    public void send(View view) {
        mEtCommand = findViewById(R.id.et_cmd);
        btController.write(mEtCommand.getText().toString());
    }
}
