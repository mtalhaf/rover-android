package com.rovercontroller.mtalhaf.rovercontroller.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.rovercontroller.mtalhaf.rovercontroller.R;

public class MainActivity extends AppCompatActivity {

    EditText displayMessageEditText;
    Button displayMessageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpViews();
        setUpViewListeners();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    private void setUpViews() {
        displayMessageEditText = (EditText) findViewById(R.id.displayMessageEditText);
        displayMessageButton = (Button) findViewById(R.id.displayMessageButton);
    }

    private void setUpViewListeners() {
        displayMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
            }
        });
    }
}
