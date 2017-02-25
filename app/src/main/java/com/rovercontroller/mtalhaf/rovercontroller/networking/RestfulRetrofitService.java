package com.rovercontroller.mtalhaf.rovercontroller.networking;

import java.util.Map;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import rx.Observable;

/**
 * Created by Study on 2/25/2017.
 */

public interface RestfulRetrofitService {

    @GET("v1.0/lcd/print")
    Observable<String> displayMessage(@QueryMap Map<String, String> options);
}
