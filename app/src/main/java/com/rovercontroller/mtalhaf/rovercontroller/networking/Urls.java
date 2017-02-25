package com.rovercontroller.mtalhaf.rovercontroller.networking;

/**
 * Created by Study on 2/25/2017.
 */

public class Urls {
    private static final String url = "192.168.43.55:5000/";
    private static final String baseSecureUrl = "https://" + url;
    private static final String baseUnSecureUrl = "http://" + url;


    public static String getUrl() {
        return baseUnSecureUrl;
    }
}
