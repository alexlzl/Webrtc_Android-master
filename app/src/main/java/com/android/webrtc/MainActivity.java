package com.android.webrtc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    private EditText et_address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_address = findViewById(R.id.et_address);
        setServerUrl(Constant.URL);
    }

    public void onClick(View view) {
        if (checkUrl()) return;
        startActivity(new Intent(MainActivity.this, WebRtcActivity.class));
    }

    private boolean checkUrl() {
        String address = et_address.getText().toString().trim();
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(MainActivity.this, "请填入 websocket 地址", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    public void setting(View view) {
        if (checkUrl()) return;
        setServerUrl(et_address.getText().toString().trim());
    }

    public void setServerUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            SharePreferences.getInstance(MainActivity.this).setServerUrl(url);
            et_address.setText(url);
        }
    }
}
