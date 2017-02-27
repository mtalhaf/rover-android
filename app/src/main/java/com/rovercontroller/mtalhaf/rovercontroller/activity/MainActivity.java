package com.rovercontroller.mtalhaf.rovercontroller.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    EditText mDisplayMessageEditText;
    Button mDisplayMessageButton;

    TextView mAzimutTextView;
    TextView mPitchTextView;
    TextView mRollTextView;

    LcdAdapter mLcdAdapter;
    Observable<String> mDisplayMessageObservable;
    Disposable mDisplayMessageDisposable;

    SensorManager mSensorManager;
    Sensor mAccelometer;
    Sensor mMagneticField;

    float mGravity[];
    float mGeomagnetic[];

    int azimut;
    int pitch;
    int roll;


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
        registerSensors();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cleanUp();
    }

    private void setUpViews() {
        mDisplayMessageEditText = (EditText) findViewById(R.id.displayMessageEditText);
        mDisplayMessageButton = (Button) findViewById(R.id.displayMessageButton);

        mAzimutTextView = (TextView) findViewById(R.id.azimutTextView);
        mPitchTextView = (TextView) findViewById(R.id.pitchTextView);
        mRollTextView = (TextView) findViewById(R.id.rollTextView);
    }

    private void setUpViewListeners() {
        mDisplayMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void setUpVariables() {
        mLcdAdapter = new LcdServerAdapter(this.getApplicationContext());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    }

    private void registerSensors() {
        mSensorManager.registerListener(this, mAccelometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void cleanUp() {
        if (mDisplayMessageDisposable != null)
            if (!mDisplayMessageDisposable.isDisposed())
                mDisplayMessageDisposable.dispose();

        mSensorManager.unregisterListener(this);
    }

    private void sendMessage() {
        Map<String, String> options = new HashMap<>();
        options.put("message", String.valueOf(mDisplayMessageEditText.getText()));

        mDisplayMessageObservable = mLcdAdapter.displayMessage(options);
        mDisplayMessageDisposable = mDisplayMessageObservable
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimut = (int) Math.toDegrees(orientation[0]); // orientation contains: azimut, pitch and roll
                pitch = (int) Math.toDegrees(orientation[1]);
                roll = (int) Math.toDegrees(orientation[2]);

                mAzimutTextView.setText("azimut: " + azimut);
                mPitchTextView.setText("pitch: " + pitch);
                mRollTextView.setText("roll: " + roll);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
