package com.rovercontroller.mtalhaf.rovercontroller.networking.lcd;

import java.util.Map;

import io.reactivex.Observable;

/**
 * Created by Study on 2/25/2017.
 * This class is used as the interface for all the methods
 * available for using the lcd.
 */

public interface LcdAdapter {
    // displays a message on the lcd
    Observable<String> displayMessage(Map<String, String> options);
}
