package com.rovercontroller.mtalhaf.rovercontroller.networking.lcd;

import android.content.Context;

import com.rovercontroller.mtalhaf.rovercontroller.networking.BaseAdapter;
import com.rovercontroller.mtalhaf.rovercontroller.networking.RestfulRetrofitService;

import java.util.Map;

import io.reactivex.Observable;

/**
 * Created by Study on 2/25/2017.
 * implements the methods listed in lcd Adapter
 */

public class LcdServerAdapter implements LcdAdapter {

    RestfulRetrofitService service;

    public LcdServerAdapter(Context context){
        BaseAdapter baseAdapter = new BaseAdapter(context);
        service = baseAdapter.getService();
    }

    @Override
    public Observable<String> displayMessage(Map<String, String> options) {
        return service.displayMessage(options);
    }
}
