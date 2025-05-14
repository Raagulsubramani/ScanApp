package com.gmscan.service;
import static com.gmscan.GmScanApplication.preferenceManager;

import android.util.Log;

import com.gmscan.model.loginRegister.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestApiBuilder {
    public static final String BASE_URL_OPEN_LIB = "https://openlibrary.org/";

    private static final String BASE_URL_SERVER = "https://gmscan.onrender.com";

    public static RestApiService getService() {

        // 🔍 Logging Interceptor
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        Gson gson = new GsonBuilder().create();

        // ✅ Timeout & Auth Interceptors
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    Request original = chain.request();

                    Request.Builder requestBuilder = original.newBuilder();
                    Log.e("Request :", requestBuilder.toString());

                    User user = preferenceManager.getUser();
                    String token = user != null ? user.getAccessToken() : null;

                    if (token != null) {
                        Log.e("User token", token);
                        requestBuilder.addHeader("Authorization", "Bearer " + token);
                    }

                    return chain.proceed(requestBuilder.build());
                })
                .build();

        // 🚀 Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL_SERVER)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        return retrofit.create(RestApiService.class);
    }
}
