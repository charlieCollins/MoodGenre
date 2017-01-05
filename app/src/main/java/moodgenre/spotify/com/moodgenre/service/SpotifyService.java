package moodgenre.spotify.com.moodgenre.service;

/**
 * Created by charliecollins on 1/4/17.
 */

import java.util.List;

import moodgenre.spotify.com.moodgenre.model.TrackContainer;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
import rx.Observable;

public interface SpotifyService {

    @GET("recommendations")
    Observable<TrackContainer> getRecommendations(@Header("Authorization") String token, @Query("seed_genres") String seedGenres);

}
