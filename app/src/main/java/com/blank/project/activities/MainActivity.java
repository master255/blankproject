package com.blank.project.activities;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.blank.project.R;

public class MainActivity extends FragmentActivity {

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Runtime.getRuntime().gc();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
}