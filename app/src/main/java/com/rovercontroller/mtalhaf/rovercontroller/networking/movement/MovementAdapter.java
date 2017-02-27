package com.rovercontroller.mtalhaf.rovercontroller.networking.movement;

import java.util.Map;

import io.reactivex.Observable;

/**
 * Created by Study on 2/27/2017.
 * This class is used as the interface for all the methods
 * available for moving and stopping the rover.
 */

public interface MovementAdapter {

    //moves the rover forward
    Observable<String> moveRoverForward();

    //moves the rover backward
    Observable<String> moveRoverBackward();

    //turns the rover left
    Observable<String> turnRoverLeft();

    //turns the rover right
    Observable<String> turnRoverRight();

    //stops the rover
    Observable<String> stopRover();

}
