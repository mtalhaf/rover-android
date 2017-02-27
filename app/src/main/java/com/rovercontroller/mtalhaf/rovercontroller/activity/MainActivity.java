package com.rovercontroller.mtalhaf.rovercontroller.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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
import io.reactivex.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /*
     * The following lines of code are declaration of all the views in the main activity
     */
    EditText mDisplayMessageEditText;
    Button mDisplayMessageButton;

    TextView mAzimutTextView;
    TextView mPitchTextView;
    TextView mRollTextView;

    /*
     * The adapter used to connect to different resources of the API
     */
    
    //contains all the lcd manipulation functions
    LcdAdapter mLcdAdapter;
    
    /*
     * All the observables and disposables used for connecting to the API
     */
    
    //observable and disposable for displaying a message on the lcd screen
    Observable<String> mDisplayMessageObservable;
    Disposable mDisplayMessageDisposable;

    /*
     * The following code is used to get the gyroscope values from the android device
     */
    
    //Android device sensor manager, this is used to retrieve the 2 accelerometer and magnetic field sensors
    SensorManager mSensorManager;
    
    //sensor variables to hold the accelerometer and magnetic field meter
    Sensor mAccelerometer;
    Sensor mMagneticField;

    //publisher to publish the device rotational values
    PublishSubject<Integer> pitchPublisher;

    //hold the gravity and geo magnetic values
    float mGravity[];
    float mGeomagnetic[];

    // variables to hold the azimut, pitch and roll
    int mAzimuth;
    int mPitch;
    int mRoll;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //sets up the views finding them in the layout
        setUpViews();
        //set up view listeners
        setUpViewListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //initilises all the variables
        setUpVariables();

        //registers the sensors
        registerSensors();

        //sets up the rover turning method
        turnRover();
    }

    /*
     * Turns the rover when the device rotates
     */
    private void turnRover() {
        pitchPublisher
                .distinctUntilChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        Log.d("TURN", "Pitch: " + integer);
                    }
                });
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
        //sets up the display message button which sends a message to display on the Lcd screen
        mDisplayMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void setUpVariables() {
        //sets up the lcd adapter
        mLcdAdapter = new LcdServerAdapter(this.getApplicationContext());

        //sets up the sensor manager and retrieves the accelerometer and magnetic field
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //creates a publish subject to publish the pitch of the device
        pitchPublisher = PublishSubject.create();

    }

    private void registerSensors() {
        //register this activity as the listener to retieve the values from the 2 sensors
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void cleanUp() {
        //disposes off the display message observable
        if (mDisplayMessageDisposable != null)
            if (!mDisplayMessageDisposable.isDisposed())
                mDisplayMessageDisposable.dispose();

        //unregisters the sensor callbacks
        mSensorManager.unregisterListener(this);
    }

    /*
     * This method sends the message type on the edit text box to the
     * backend API. the API will publish the message on a topic which
     * will display that message on the lcd placed on the rover
     */
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

    /*
     * Retrieves the sensor change events and calculates the relative Azimuth, Pitch and Roll
     * of the device.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        //checks if the sensor event type is the accelerometer
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        //checks if the sensor event type is the magnetic field sensor
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        // if gravity and geomagnetic values are present then calculate azimuth, pitch and roll
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            //get the rotation matrix
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            //checks to see if all the data is present
            if (success) {
                float orientation[] = new float[3];
                //gets the orientation of the device
                SensorManager.getOrientation(R, orientation);

                //pulls out the respective values
                mAzimuth = (int) Math.toDegrees(orientation[0]); // orientation contains: mAzimuth, mPitch and mRoll
                mPitch = (int) Math.toDegrees(orientation[1]);
                mRoll = (int) Math.toDegrees(orientation[2]);

                //displays the values on the screen
                mAzimutTextView.setText("Azimuth: " + mAzimuth);
                mPitchTextView.setText("Pitch: " + mPitch);
                mRollTextView.setText("Roll: " + mRoll);

                //publishes the pitch of the device
                pitchPublisher.onNext(mPitch);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
