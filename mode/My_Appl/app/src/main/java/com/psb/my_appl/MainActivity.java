package com.psb.my_appl;

import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Camera2Fragment camera2Fragment = (Camera2Fragment) getFragmentManager().findFragmentById(R.id.contentFrame);
        if (camera2Fragment == null) {
            camera2Fragment = Camera2Fragment.newInstance();
            ActivityUtils.addFragmentToActivity(getFragmentManager(), camera2Fragment, R.id.contentFrame);
        }
    }
}
