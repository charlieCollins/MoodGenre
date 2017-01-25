package moodgenre.spotify.com.moodgenre;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.util.List;

import moodgenre.spotify.com.moodgenre.adapters.TrackListAdapter;
import moodgenre.spotify.com.moodgenre.model.Track;
import rx.functions.Action1;


public class PlayerActivity extends BaseActivity  {

    private ImageView buttonPlayPause;
    private TextView labelNowPlaying;

    private RecyclerView trackListRecyclerView;
    private TrackListAdapter trackListAdapter;
    private RecyclerView.LayoutManager trackListLayoutManager;

    private Player spotifyPlayer;
    private ConnectionStateCallback spotifyConnectionStateCallback;
    private Player.NotificationCallback spotifyPlayerNotificationCallback;
    private Player.OperationCallback spotifyPlayerOperationCallback;
    private String spotifyAccessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        buttonPlayPause = (ImageView) findViewById(R.id.button_play_pause);
        labelNowPlaying = (TextView) findViewById(R.id.label_now_playing);

        buttonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // toggle spot pause/play
                if (spotifyPlayer == null) {
                    Toast.makeText(PlayerActivity.this, "Spotify player not ready", Toast.LENGTH_SHORT).show();
                } else {
                    // SpotifyPlayer provides init and connect methods; direct controls (play uri, pause, seek, skip, resume); and state (metadata and playbackstate)
                    PlaybackState playbackState = spotifyPlayer.getPlaybackState();
                    Log.d(Constants.TAG, "playPause click, playbackState:" + playbackState);
                    Metadata metadata = spotifyPlayer.getMetadata();
                    
                    if (!playbackState.isPlaying && playbackState.positionMs == 0) {
                        // nothing has been started yet play track 1
                        Track track = trackListAdapter.getFirstTrack();
                        labelNowPlaying.setText(track.getName());
                        spotifyPlayer.playUri(null, track.getUri(), 0, 0);
                        return;
                    }
                    
                    if (playbackState.isPlaying) {
                        spotifyPlayer.pause(spotifyPlayerOperationCallback);
                        return;
                    } 
                    
                    if (!playbackState.isPlaying && playbackState.positionMs > 0) {
                        // TODO how to tell if player is paused, idPlaying false and just position != 0? or is there an actual pause state, weird?
                        spotifyPlayer.resume(spotifyPlayerOperationCallback);
                        return;
                    } 
                    
                    // get here it's weird
                    Log.d(Constants.TAG, "error unexepected playback state:" + playbackState);
                    Toast.makeText(PlayerActivity.this, "Spotify playback state weird:" + playbackState, Toast.LENGTH_LONG).show();                    
                }
            }
        });

        spotifyPlayerNotificationCallback = new Player.NotificationCallback() {
            @Override
            public void onPlaybackEvent(PlayerEvent playerEvent) {
                Log.d(Constants.TAG, "Spotify player notif callback: playback event: " + playerEvent.name());
                handleSpotifyEvent(playerEvent);
            }

            @Override
            public void onPlaybackError(Error error) {
                Log.d(Constants.TAG, "Spotify player notif callback: playback error: " + error.name());
                handleSpotifyError(error);
            }
        };

        // recycler view
        List<Track> playList = application.getPlaylist();
        trackListRecyclerView = (RecyclerView) findViewById(R.id.track_list_recycler);
        trackListRecyclerView.setHasFixedSize(true);
        trackListLayoutManager = new LinearLayoutManager(this);
        trackListRecyclerView.setLayoutManager(trackListLayoutManager);
        trackListAdapter = new TrackListAdapter(PlayerActivity.this, playList);
        trackListRecyclerView.setAdapter(trackListAdapter);

        // get track list click events as observable
        // TODO do I need to unsubscribe from this somehow?
        trackListAdapter.asObservable().subscribe(new Action1<Track>() {
            @Override
            public void call(Track track) {
                // TODO make other adapter list items not clickable until one is processed?
                labelNowPlaying.setText(track.getName());
                spotifyPlayer.playUri(null, track.getUri(), 0, 0);
            }
        });

        // spotify player callback
        spotifyConnectionStateCallback = new ConnectionStateCallback() {

            @Override
            public void onLoggedIn() {
                Log.d(Constants.TAG, "Spotify connection callback: User logged in");
            }

            @Override
            public void onLoggedOut() {
                Log.d(Constants.TAG, "Spotify connection callback: user logged out");
            }

            @Override
            public void onLoginFailed(Error e) {
                Log.d(Constants.TAG, "Spotify connection callback: login failed: " + e.toString());
            }

            @Override
            public void onTemporaryError() {
                Log.d(Constants.TAG, "Spotify connection callback: temp error occurred");
            }

            @Override
            public void onConnectionMessage(String message) {
                Log.d(Constants.TAG, "Spotify connection callback: connection message: " + message);
            }
        };

        spotifyPlayerOperationCallback = new Player.OperationCallback() {
            @Override
            public void onSuccess() {
                Log.d(Constants.TAG, "Spotify operation callback: success");
            }

            @Override
            public void onError(Error error) {
                Log.d(Constants.TAG, "Spotify operation callback: error " + error.name());
                Toast.makeText(PlayerActivity.this, "Spotify op error: " + error.name(), Toast.LENGTH_SHORT).show();
            }
        };

        spotifyAccessToken = application.getSpotifyAccessToken();
        initSpotifyPlayer();
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    //
    // private
    //

    //
    // SPOT
    //

    private void initSpotifyPlayer() {
        
        Log.d(Constants.TAG, "initSpotifyPlayer");

        if (spotifyAccessToken == null) {
            Toast.makeText(this, "Spotify access token not present, cannot continue", Toast.LENGTH_LONG).show();
            return;
        }

        Config playerConfig = new Config(this, spotifyAccessToken, Constants.SPOTIFY_CLIENT_ID);
        Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
            @Override
            public void onInitialized(SpotifyPlayer player) {
                spotifyPlayer = player;
                spotifyPlayer.addConnectionStateCallback(spotifyConnectionStateCallback);
                spotifyPlayer.addNotificationCallback(spotifyPlayerNotificationCallback);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(Constants.TAG, "Could not initialize player: " + throwable.getMessage());
            }
        });
    }

    private void handleSpotifyEvent(final PlayerEvent playerEvent) {
        Log.d(Constants.TAG, "Spotify playerEvent:" + playerEvent.name());

        switch (playerEvent) {

            case kSpPlaybackEventAudioFlush:
                break;
            case kSpPlaybackNotifyAudioDeliveryDone:
                break;
            case kSpPlaybackNotifyBecameActive:
                break;
            case kSpPlaybackNotifyBecameInactive:
                break;
            case kSpPlaybackNotifyContextChanged:
                break;
            case kSpPlaybackNotifyLostPermission:
                Toast.makeText(PlayerActivity.this, "Spotify player perms lost (logged in elsewhere?)", Toast.LENGTH_LONG).show();
                buttonPlayPause.setImageResource(android.R.drawable.ic_media_play);
                startActivity(new Intent(PlayerActivity.this, MainActivity.class));
                break;
            case kSpPlaybackNotifyMetadataChanged:
                break;
            case kSpPlaybackNotifyNext:
                break;
            case kSpPlaybackNotifyPause:
                buttonPlayPause.setImageResource(android.R.drawable.ic_media_play);                
                break;
            case kSpPlaybackNotifyPlay:
                buttonPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                // TODO get current playing track here? 
                //labelNowPlaying.setText(spotifyPlayer.getMetadata().currentTrack.
                break;
            case kSpPlaybackNotifyPrev:
                break;
            case kSpPlaybackNotifyRepeatOff:
                break;
            case kSpPlaybackNotifyRepeatOn:
                break;
            case kSpPlaybackNotifyShuffleOff:
                break;
            case kSpPlaybackNotifyShuffleOn:
                break;
            case kSpPlaybackNotifyTrackChanged:
                break;
            case kSpPlaybackNotifyTrackDelivered:
                break;

            default:
                break;
        }
    }

    private void handleSpotifyError(final Error error) {
        Log.e(Constants.TAG, "Spotify Error:" + error.name());

        switch (error) {
            // corrupt track, add is playing, need perms, travel restriction, etc
            default:
                break;
        }
    }

}