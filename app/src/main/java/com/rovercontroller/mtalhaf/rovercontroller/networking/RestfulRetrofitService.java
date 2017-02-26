package com.rovercontroller.mtalhaf.rovercontroller.networking;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

/**
 * Created by Study on 2/25/2017.
 * This class contains all the methods/ endpoints which are available in the API
 * All the code in this class is self-explanatory
 */

public interface RestfulRetrofitService {

    @GET("v1.0/lcd/print")
    Observable<String> displayMessage(@QueryMap Map<String, String> options);
}
