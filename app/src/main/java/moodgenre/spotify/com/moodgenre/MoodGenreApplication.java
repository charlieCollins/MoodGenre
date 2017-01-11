package moodgenre.spotify.com.moodgenre;

import android.app.Application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import moodgenre.spotify.com.moodgenre.model.Track;
import moodgenre.spotify.com.moodgenre.service.SpotifyService;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by charliecollins on 1/4/17.
 */

public class MoodGenreApplication extends Application {

    private String spotifyAccessToken;
    private List<Track> playlist;

    private SpotifyService spotifyService;

    @Override
    public void onCreate() {
        super.onCreate();

        playlist = new ArrayList<>();

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

    //
    // DATA (lazy globals)
    //

    public String getSpotifyAccessToken() {
        return this.spotifyAccessToken;
    }

    public void setSpotifyAccessToken(String spotifyAuthToken) {
        this.spotifyAccessToken = spotifyAuthToken;
        // TODO check token expiration and serialize it if longer lived?
    }

    public List<Track> getPlaylist() {
        return Collections.unmodifiableList(playlist);
    }

    public void setPlaylist(List<Track> playlist) {
        this.playlist = playlist;
    }

    public SpotifyService getSpotifyService() {
        return this.spotifyService;
    }

    //
    // private
    //

    private void initService() {

        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        if (Constants.DEBUG) {
            httpClientBuilder.addInterceptor(httpLoggingInterceptor);
        }

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
