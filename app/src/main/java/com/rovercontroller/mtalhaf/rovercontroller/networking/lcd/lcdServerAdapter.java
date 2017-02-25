package com.rovercontroller.mtalhaf.rovercontroller.networking.lcd;

import android.content.Context;

import com.rovercontroller.mtalhaf.rovercontroller.networking.BaseAdapter;
import com.rovercontroller.mtalhaf.rovercontroller.networking.RestfulRetrofitService;

import java.util.Map;

import io.reactivex.Observable;

/**
 * Created by Study on 2/25/2017.
 */

public class lcdServerAdapter implements lcdAdapter {

    RestfulRetrofitService service;

    public lcdServerAdapter(Context context){
        BaseAdapter baseAdapter = new BaseAdapter(context);
        service = baseAdapter.getService();
    }

    @Override
    public Observable<String> displayMessage(Map<String, String> options) {
        return service.displayMessage(options);
    }
}
