package moodgenre.spotify.com.moodgenre;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import moodgenre.spotify.com.moodgenre.adapters.TrackListAdapter;
import moodgenre.spotify.com.moodgenre.model.Genre;
import moodgenre.spotify.com.moodgenre.model.Track;
import moodgenre.spotify.com.moodgenre.model.TrackContainer;
import moodgenre.spotify.com.moodgenre.service.SpotifyService;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class PathGenreSelectionActivity extends BaseActivity  {

    private Button buttonGetPlaylist;
    private Button buttonGotoPlayer;
    private AppCompatCheckBox checkboxHappy;
    private AppCompatCheckBox checkboxSad;
    private AppCompatCheckBox checkboxAngry;
    private AppCompatCheckBox checkboxConfused;

    private RecyclerView trackListRecyclerView;
    private TrackListAdapter trackListAdapter;
    private RecyclerView.LayoutManager trackListLayoutManager;
    private List<Track> trackList;

    private String spotifyAccessToken;
    private SpotifyService spotifyService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_genre_selection);

        buttonGetPlaylist = (Button) findViewById(R.id.button_get_playlist);
        buttonGotoPlayer = (Button) findViewById(R.id.button_goto_player);
        checkboxHappy = (AppCompatCheckBox) findViewById(R.id.checkbox_happy);
        checkboxSad = (AppCompatCheckBox) findViewById(R.id.checkbox_sad);
        checkboxAngry = (AppCompatCheckBox) findViewById(R.id.checkbox_angry);
        checkboxConfused = (AppCompatCheckBox) findViewById(R.id.checkbox_confused);

        buttonGotoPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (trackList == null || trackList.isEmpty()) {
                    Toast.makeText(PathGenreSelectionActivity.this, "Playlist empty, cannot proceed to player", Toast.LENGTH_LONG).show();
                    return;
                }

                application.setPlaylist(trackList);
                startActivity(new Intent(PathGenreSelectionActivity.this, PlayerActivity.class));
            }
        });

        buttonGetPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSpotifyRecommendations();
            }
        });

        trackListRecyclerView = (RecyclerView) findViewById(R.id.track_list_recycler);
        trackListRecyclerView.setHasFixedSize(true);
        trackListLayoutManager = new LinearLayoutManager(this);
        trackListRecyclerView.setLayoutManager(trackListLayoutManager);
        trackList = new ArrayList<>();
        trackListAdapter = new TrackListAdapter(PathGenreSelectionActivity.this, trackList);
        trackListRecyclerView.setAdapter(trackListAdapter);

        // get track list click events as observable
        // TODO do I need to unsubscribe from this somehow?
        trackListAdapter.asObservable().subscribe(new Action1<Track>() {
            @Override
            public void call(Track track) {
                // TODO make other adapter list items not clickable until one is processed?

            }
        });
  
        spotifyService = application.getSpotifyService();
        spotifyAccessToken = application.getSpotifyAccessToken();
    }

   
    //
    // private
    //

    private void getSpotifyRecommendations() {

        Log.d(Constants.TAG, "getSpotifyRecommendations");

        if (spotifyAccessToken == null) {
            Log.d(Constants.TAG, "   no access token, bail");
            Toast.makeText(this, "Spotify access token not present, cannot continue", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> genreList = new ArrayList<>();
        if (checkboxHappy.isChecked()) {
            genreList.addAll(Genre.HAPPY.getEmotions());
        }
        if (checkboxSad.isChecked()) {
            genreList.addAll(Genre.SAD.getEmotions());
        }
        if (checkboxAngry.isChecked()) {
            genreList.addAll(Genre.ANGRY.getEmotions());
        }
        if (checkboxConfused.isChecked()) {
            genreList.addAll(Genre.CONFUSED.getEmotions());
        }
        String genreListString = TextUtils.join(",", genreList);
        Log.d(Constants.TAG, "   genre list string:" + genreListString);

        if (genreListString == null || genreListString.isEmpty()) {
            Log.d(Constants.TAG, "   no genres, bail");
            Toast.makeText(this, "Cannot get playlist, no genres selected", Toast.LENGTH_LONG).show();
            return;
        }

        Observable<TrackContainer> observable = spotifyService.getRecommendations("Bearer " + spotifyAccessToken, genreListString);
        Subscription subscription = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TrackContainer>() {
                    @Override
                    public void onCompleted() {
                        Log.d(Constants.TAG, "Spotify getRecommendations completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(Constants.TAG, "Spotify getRecommendations error:" + e.getMessage());
                    }

                    @Override
                    public void onNext(TrackContainer trackContainer) {
                        Log.d(Constants.TAG, "Spotify getRecommendations TrackContainer: " + trackContainer);
                        trackList.clear();
                        trackList.addAll(trackContainer.getTracks());
                        trackListAdapter.notifyDataSetChanged();
                    }
                });
    }
}