package com.rovercontroller.mtalhaf.rovercontroller.networking.movement;

import android.content.Context;

import com.rovercontroller.mtalhaf.rovercontroller.networking.BaseAdapter;
import com.rovercontroller.mtalhaf.rovercontroller.networking.RestfulRetrofitService;

import io.reactivex.Observable;

/**
 * Created by Study on 2/27/2017.
 */

public class MovementServerAdapter implements MovementAdapter {

    RestfulRetrofitService service;

    public MovementServerAdapter(Context context){
        BaseAdapter baseAdapter = new BaseAdapter(context);
        service = baseAdapter.getService();
    }

    @Override
    public Observable<String> moveRoverForward() {
        return service.moveRoverForward();
    }

    @Override
    public Observable<String> moveRoverBackward() {
        return service.moveRoverBackward();
    }

    @Override
    public Observable<String> turnRoverLeft() {
        return service.turnRoverLeft();
    }

    @Override
    public Observable<String> turnRoverRight() {
        return service.turnRoverRight();
    }

    @Override
    public Observable<String> stopRover() {
        return service.stopRover();
    }
}
