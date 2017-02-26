package com.rovercontroller.mtalhaf.rovercontroller.networking;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by Study on 2/25/2017.
 */

public class BaseAdapter {


    protected Gson gson;
    protected OkHttpClient httpClient;
    protected RestfulRetrofitService service;
    Context context;

    public BaseAdapter(Context context) {

        //initiates the gson converter which excludes fields and sets the gson read to be lenient
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setLenient().create();

        //get an unsafe http client which does not checks for ssl certificates or the verification of the host
        OkHttpClient.Builder okHttpClientBuilder = getUnsafeOkHttpClient();
        httpClient = okHttpClientBuilder.build();

        //starts the retrofit adapter and connects to the Api using the httpclient
        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(Urls.getUrl())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build();

        //provides the class which contains all the endpoints listed for the API
        service = restAdapter.create(RestfulRetrofitService.class);
    }

    public RestfulRetrofitService getService() {
        return service;
    }


    //returns an unsafe http client builder
    public static OkHttpClient.Builder getUnsafeOkHttpClient() {
        try {

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
            }

            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();


            OkHttpClient.Builder okHttpClient = new OkHttpClient()
                    .newBuilder()
                    .sslSocketFactory(sslSocketFactory, trustManager)
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return false;
                        }
                    });

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
