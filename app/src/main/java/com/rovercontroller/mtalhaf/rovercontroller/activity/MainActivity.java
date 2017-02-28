package com.rovercontroller.mtalhaf.rovercontroller.activity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rovercontroller.mtalhaf.rovercontroller.R;
import com.rovercontroller.mtalhaf.rovercontroller.networking.lcd.LcdAdapter;
import com.rovercontroller.mtalhaf.rovercontroller.networking.lcd.LcdServerAdapter;
import com.rovercontroller.mtalhaf.rovercontroller.networking.movement.MovementAdapter;
import com.rovercontroller.mtalhaf.rovercontroller.networking.movement.MovementServerAdapter;
import com.rovercontroller.mtalhaf.rovercontroller.utility.Constants;

import java.util.HashMap;
import java.util.Map;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
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

    //button to keep the rover moving
    Button mKeepRoverMovingButton;

    //joystick to move the rover forwards or backwards
    JoystickView mRoverMovementJoyStick;


    /*
     * The adapter used to connect to different resources of the API
     */

    //contains all the lcd manipulation functions
    LcdAdapter mLcdAdapter;
    MovementAdapter mMovementAdapter;
    
    /*
     * All the observables and disposables used for connecting to the API
     */

    //observable and disposable for displaying a message on the lcd screen
    Observable<String> mDisplayMessageObservable;
    Disposable mDisplayMessageDisposable;

    //observables and disposables for moving the rover
    Observable<String> mMoveRoverForwardObservable;
    Disposable mMoveRoverForwardDisposable;

    Observable<String> mMoveRoverBackwardObservable;
    Disposable mMoveRoverBackwardDisposable;

    Observable<String> mTurnRoverLeftObservable;
    Disposable mTurnRoverLeftDisposable;

    Observable<String> mTurnRoverRightObservable;
    Disposable mTurnRoverRightDisposable;

    Observable<String> mStopRoverObservable;
    Disposable mStopRoverDisposable;

    /*
     * The following code is used to get the gyroscope values from the android device
     */

    //Android device sensor manager, this is used to retrieve the 2 accelerometer and magnetic field sensors
    SensorManager mSensorManager;

    //sensor variables to hold the accelerometer and magnetic field meter
    Sensor mAccelerometer;
    Sensor mMagneticField;

    //publisher to publish the device rotational values
    PublishSubject<Integer> mAzimuthPublisher;
    PublishSubject<Integer> mPitchPublisher;

    //hold the gravity and geo magnetic values
    float mGravity[];
    float mGeomagnetic[];

    // variables to hold the azimut, pitch and roll
    int mAzimuth;
    int mPitch;
    int mRoll;

    //PublishSubject which will move the rover in one direction at a time or stop it
    PublishSubject<String> mMoveRoverPublisher;

    //checks to see if the command should be sent to the rover
    boolean mMoveRover;

    //checks to see if the rover is moving forward or backwards
    boolean mRoverMovingForwardOrBackward;


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

        mKeepRoverMovingButton = (Button) findViewById(R.id.keepRoverMovingButton);
        mRoverMovementJoyStick = (JoystickView) findViewById(R.id.roverMovementJoyStick);
    }

    private void setUpViewListeners() {
        //sets up the display message button which sends a message to display on the Lcd screen
        mDisplayMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        //setsup the move rover button listener, the rover will move while this is pressed otherwise a stop command is sent
        mKeepRoverMovingButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        mMoveRover = false;
                        mMoveRoverPublisher.onNext(Constants.STOP_ROVER);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        mMoveRover = true;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        mMoveRover = true;
                        break;
                }
                return false;
            }
        });

        //moves the rover in the forward or backwards direction
        mRoverMovementJoyStick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if (strength > 10) {
                    mRoverMovingForwardOrBackward = true;
                    if (angle >= 0 && angle < 180)
                        mMoveRoverPublisher.onNext(Constants.MOVE_ROVER_FORWARD);
                    if (angle >= 180 && angle < 360)
                        mMoveRoverPublisher.onNext(Constants.MOVE_ROVER_BACKWARD);

                } else {
                    mMoveRover = false;
                    mRoverMovingForwardOrBackward = false;
                    mMoveRoverPublisher.onNext(Constants.STOP_ROVER);
                }
            }
        }, 500);
    }

    private void setUpVariables() {
        //sets up the lcd adapter
        mLcdAdapter = new LcdServerAdapter(this.getApplicationContext());
        mMovementAdapter = new MovementServerAdapter(this.getApplicationContext());

        //sets up the sensor manager and retrieves the accelerometer and magnetic field
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //creates publish subject to publish the pitch and azimuth of the device
        mAzimuthPublisher = PublishSubject.create();
        mPitchPublisher = PublishSubject.create();

        //crates publisher for moving the rover
        mMoveRoverPublisher = PublishSubject.create();
    }

    /*
     * Turns the rover when the device rotates
     */
    private void turnRover() {

        mAzimuthPublisher
                .distinctUntilChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(Integer azimuth) throws Exception {
                        //if the device is nearly horizontal then we use azimuth to calculate where the rover should move
                        if (mRoll < 25 && mRoll > -20) {
                            //turn the rover left if the device is turning left
                            if (azimuth > 40 && azimuth < 70)
                                return Observable.just(Constants.ROVER_TURN_LEFT);
                            //turn the rover right if the device is turning right
                            if (azimuth > 85 && azimuth < 120)
                                return Observable.just(Constants.ROVER_TURN_RIGHT);
                            //stops the rover if the device is nearly straight
                            if(azimuth > 70 && azimuth < 85)
                                return Observable.just(Constants.STOP_ROVER);
                        }
                        return Observable.empty();
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String turn) throws Exception {
                        turnRover(turn);
                    }
                });

        mPitchPublisher
                .distinctUntilChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<Integer, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(Integer pitch) throws Exception {
                        //if the device is nearly vertical then we use pitch to calculate where the rover should move
                        if (mRoll > -120 && mRoll < -20) {
                            //turn the rover left if the device is turning left
                            if (pitch > 15 && pitch < 50)
                                return Observable.just(Constants.ROVER_TURN_LEFT);
                            //turn the rover right if the device is turning right
                            if (pitch > -50 && pitch < -20)
                                return Observable.just(Constants.ROVER_TURN_RIGHT);
                            //stops the rover if the device is nearly straight
                            if(pitch > -20 && pitch < 15)
                                return Observable.just(Constants.STOP_ROVER);
                        }
                        return Observable.empty();
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String turn) throws Exception {
                        turnRover(turn);
                    }
                });

        //this publisher makes sure that the moving commands are only sent when they are changed
        mMoveRoverPublisher
                .distinctUntilChanged()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String moveString) throws Exception {
                        return mMoveRover ? Observable.just(moveString) : Observable.just(Constants.STOP_ROVER);
                    }
                })
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String moveString) throws Exception {
                        switch (moveString) {
                            case Constants.MOVE_ROVER_FORWARD:
                                moveRoverForward();
                                break;
                            case Constants.MOVE_ROVER_BACKWARD:
                                moveRoverBackward();
                                break;
                            case Constants.ROVER_TURN_LEFT:
                                turnRoverLeft();
                                break;
                            case Constants.ROVER_TURN_RIGHT:
                                turnRoverRight();
                                break;
                            case Constants.STOP_ROVER:
                                stopRover();
                                break;
                        }
                    }
                });
    }

    private void turnRover(String turn) {
        if (!mRoverMovingForwardOrBackward) {
            if (turn.equals(Constants.ROVER_TURN_LEFT))
                mMoveRoverPublisher.onNext(Constants.ROVER_TURN_LEFT);
            if (turn.equals(Constants.ROVER_TURN_RIGHT))
                mMoveRoverPublisher.onNext(Constants.ROVER_TURN_RIGHT);
        }
    }


    /*
     * Moves the rover forward
     */

    private void moveRoverForward() {
        mMoveRoverForwardObservable = mMovementAdapter.moveRoverForward();
        mMoveRoverForwardDisposable = mMoveRoverForwardObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this, "Can't connect to the API", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*
     * Moves the rover backward
     */

    private void moveRoverBackward() {
        mMoveRoverBackwardObservable = mMovementAdapter.moveRoverBackward();
        mMoveRoverBackwardDisposable = mMoveRoverBackwardObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this, "Can't connect to the API", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    /*
     * Turns the rover left
     */
    private void turnRoverLeft() {
        mTurnRoverLeftObservable = mMovementAdapter.turnRoverLeft();
        mTurnRoverLeftDisposable = mTurnRoverLeftObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this, "Can't connect to the API", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*
     * Turns the rover right
     */

    private void turnRoverRight() {
        mTurnRoverRightObservable = mMovementAdapter.turnRoverRight();
        mTurnRoverRightDisposable = mTurnRoverRightObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this, "Can't connect to the API", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /*
     * Stops the rover from moving
     */

    private void stopRover() {
        mStopRoverObservable = mMovementAdapter.stopRover();
        mStopRoverDisposable = mStopRoverObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                        Toast.makeText(MainActivity.this, "Can't connect to the API", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void registerSensors() {
        //register this activity as the listener to retieve the values from the 2 sensors
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void cleanUp() {
        //disposes off the observables
        if (mDisplayMessageDisposable != null)
            if (!mDisplayMessageDisposable.isDisposed())
                mDisplayMessageDisposable.dispose();

        if (mMoveRoverForwardDisposable != null)
            if (!mMoveRoverForwardDisposable.isDisposed())
                mMoveRoverForwardDisposable.dispose();

        if (mMoveRoverBackwardDisposable != null)
            if (!mMoveRoverBackwardDisposable.isDisposed())
                mMoveRoverBackwardDisposable.dispose();

        if (mTurnRoverLeftDisposable != null)
            if (!mTurnRoverLeftDisposable.isDisposed())
                mTurnRoverLeftDisposable.dispose();

        if (mTurnRoverRightDisposable != null)
            if (!mTurnRoverRightDisposable.isDisposed())
                mTurnRoverRightDisposable.dispose();

        if (mStopRoverDisposable != null)
            if (!mStopRoverDisposable.isDisposed())
                mStopRoverDisposable.dispose();

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

                //publishes the azimuth, pitch, roll of the device
                mPitchPublisher.onNext(mPitch);
                mAzimuthPublisher.onNext(mAzimuth);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
