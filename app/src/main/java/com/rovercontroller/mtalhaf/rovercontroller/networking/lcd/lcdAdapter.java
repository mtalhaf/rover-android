package com.rovercontroller.mtalhaf.rovercontroller.networking.lcd;

import java.util.Map;

import io.reactivex.Observable;

/**
 * Created by Study on 2/25/2017.
 */

public interface lcdAdapter {
    Observable<String> displayMessage(Map<String, String> options);
}
