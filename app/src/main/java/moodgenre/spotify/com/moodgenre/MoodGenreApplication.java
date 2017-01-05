package moodgenre.spotify.com.moodgenre;

import android.app.Application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import moodgenre.spotify.com.moodgenre.service.SpotifyService;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by charliecollins on 1/4/17.
 */

public class MoodGenreApplication extends Application {

    private SpotifyService spotifyService;


    @Override
    public void onCreate() {
        super.onCreate();

        initService();
    }

    @Override
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        super.registerActivityLifecycleCallbacks(callback);
    }

    @Override
    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        super.unregisterActivityLifecycleCallbacks(callback);
    }

    public SpotifyService getSpotifyService() {
        return this.spotifyService;
    }

    //
    // private
    //

    private void initService() {

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        OkHttpClient okHttpClient = httpClientBuilder.build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        RxJavaCallAdapterFactory rxAdapter = RxJavaCallAdapterFactory.create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/v1/")
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .addCallAdapterFactory(rxAdapter)
                .build();

        spotifyService = retrofit.create(SpotifyService.class);

    }
}
