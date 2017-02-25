package com.rovercontroller.mtalhaf.rovercontroller.networking;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by Study on 2/25/2017.
 */

public class BaseAdapter {

    protected Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    protected OkHttpClient httpClient;
    protected RestfulRetrofitService service;
    Context context;

    public BaseAdapter(Context context){
        OkHttpClient.Builder okHttpClientBuilder = getUnsafeOkHttpClient();
        httpClient = okHttpClientBuilder.build();

        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(Urls.getUrl())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(httpClient)
                .build();

        service = restAdapter.create(RestfulRetrofitService.class);
    }

    public RestfulRetrofitService getService(){
        return service;
    }

    public static OkHttpClient.Builder getUnsafeOkHttpClient() {
        try {
            final X509TrustManager[] trustAllCerts = new X509TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }


                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }


                        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder okHttpClient = new OkHttpClient()
                    .newBuilder()
                    .sslSocketFactory(sslSocketFactory, trustAllCerts[1])
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
