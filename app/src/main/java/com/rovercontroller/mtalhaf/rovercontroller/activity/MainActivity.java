package com.rovercontroller.mtalhaf.rovercontroller.activity;

import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.rovercontroller.mtalhaf.rovercontroller.R;
import com.rovercontroller.mtalhaf.rovercontroller.networking.lcd.LcdAdapter;
import com.rovercontroller.mtalhaf.rovercontroller.networking.lcd.LcdServerAdapter;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE = 100;
    EditText displayMessageEditText;
    Button displayMessageButton;

    LcdAdapter lcdAdapter;
    Observable<String> displayMessageObservable;
    Disposable displayMessageDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpViews();
        setUpViewListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpVariables();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cleanUp();
    }

    private void setUpViews() {
        displayMessageEditText = (EditText) findViewById(R.id.displayMessageEditText);
        displayMessageButton = (Button) findViewById(R.id.displayMessageButton);
    }

    private void setUpViewListeners() {
        displayMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void setUpVariables() {
        lcdAdapter = new LcdServerAdapter(this.getApplicationContext());
    }

    private void cleanUp() {
        if (displayMessageDisposable != null)
            if (!displayMessageDisposable.isDisposed())
                displayMessageDisposable.dispose();
    }

    private void sendMessage() {
        Map<String, String> options = new HashMap<>();
        options.put("message", String.valueOf(displayMessageEditText.getText()));

        displayMessageObservable = lcdAdapter.displayMessage(options);
        displayMessageDisposable = displayMessageObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String message) throws Exception {
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
