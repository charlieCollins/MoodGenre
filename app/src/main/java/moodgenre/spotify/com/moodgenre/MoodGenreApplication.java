package moodgenre.spotify.com.moodgenre;

import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

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

    // properties from config file -- see assets/app.properties
    // (these ids and such are not checked into source control, instead read from properties)
    private String spotifyClientId;
    private String spotifyCallbackUri;
    private String gcpApiKey;
    
    private String spotifyAccessToken;
    private List<Track> playlist;

    private SpotifyService spotifyService;

    @Override
    public void onCreate() {
        super.onCreate();

        playlist = new ArrayList<>();

        loadProperties();
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
    
    public String getSpotifyClientId() {
        return this.spotifyClientId;
    }
    
    public String getSpotifyCallbackUri() {
        return this.spotifyCallbackUri;
    }
    
    public String getGcpApiKey() {
        return this.gcpApiKey;
    }

    //
    // private
    //

    private void loadProperties() {
        try {
            AssetManager am = getAssets();
            InputStream is = am.open("app.properties");
            Properties props = new Properties();
            props.load(is);
            spotifyClientId = props.getProperty("spotify.client.id");
            spotifyCallbackUri = props.getProperty("spotify.callback.uri");
            gcpApiKey = props.getProperty("gcp.api.key");            
            is.close();
        } catch (IOException e) {
            Log.e(Constants.TAG, "cannot load properties from config file, unrecoverable");
        }

        Log.d(Constants.TAG, "MoodGenreApplication properties loaded");
        Log.d(Constants.TAG, "   spotifyClientId:" + spotifyClientId);
        Log.d(Constants.TAG, "   spotifyCallbackUri:" + spotifyCallbackUri);
        Log.d(Constants.TAG, "   gcpApiKey:" + gcpApiKey);        
    }
    
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
