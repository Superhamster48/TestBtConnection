package com.nc.check.bt.sample.testbtconnection;

import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class MainActivity extends BaseBluetoothActivity {
    public void start(View view){
        btController.start();
    }
    public void connect(View view){
        btController.connect();
    }
    public void stop(View view){
        btController.stop();
    }

    @Override
    protected void writeReceivedMessage(String strIncom) {
        ((TextView)findViewById(R.id.label_data)).setText(strIncom);
    }
    public void send(View view){
        String message = ((TextView)findViewById(R.id.et_cmd)).getText().toString();
        btController.write(message);
    }
}
