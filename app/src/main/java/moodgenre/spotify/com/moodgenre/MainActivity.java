package moodgenre.spotify.com.moodgenre;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;

import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import moodgenre.spotify.com.moodgenre.adapters.TrackListAdapter;
import moodgenre.spotify.com.moodgenre.model.Track;
import moodgenre.spotify.com.moodgenre.model.TrackContainer;

import moodgenre.spotify.com.moodgenre.service.SpotifyService;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;
import pl.aprilapps.easyphotopicker.EasyImageConfig;
import pl.tajchert.nammu.Nammu;
import pl.tajchert.nammu.PermissionCallback;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class MainActivity extends Activity  {

    private static final int SPOTIFY_AUTH_REQUEST_CODE = 1738;

    private Button chooseImageButton;
    private ImageView playPauseButton;
    private Button getRecommendationsButton;
    private TextView label1;
    private TextView authStateLabel;

    private RecyclerView trackListRecyclerView;
    private TrackListAdapter trackListAdapter;
    private RecyclerView.LayoutManager trackListLayoutManager;
    private List<Track> trackList;

    private Player spotifyPlayer;
    private ConnectionStateCallback spotifyConnectionStateCallback;
    private Player.NotificationCallback spotifyPlayerNotificationCallback;
    private Player.OperationCallback spotifyPlayerOperationCallback;
    private String spotifyAccessToken;

    private SpotifyService spotifyService;
    private Subscriber spotifyServiceSubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(Constants.TAG, "onCreate");

        chooseImageButton = (Button) findViewById(R.id.button_choose_image);
        playPauseButton = (ImageView) findViewById(R.id.button_play_pause);
        getRecommendationsButton = (Button) findViewById(R.id.button_get_recommendations);
        label1 = (TextView) findViewById(R.id.label);
        authStateLabel = (TextView) findViewById(R.id.label_auth_state);

        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EasyImage.openGallery(MainActivity.this, EasyImageConfig.REQ_PICK_PICTURE_FROM_GALLERY);
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // toggle spot pause/play
                if (spotifyPlayer == null) {
                    Toast.makeText(MainActivity.this, "Spotify player not ready", Toast.LENGTH_SHORT).show();
                } else {
                    // SpotifyPlayer provides init and connect methods; direct controls (play uri, pause, seek, skip, resume); and state (metadata and playbackstate)
                    PlaybackState playbackState = spotifyPlayer.getPlaybackState();
                    Metadata metadata = spotifyPlayer.getMetadata();
                    if (playbackState.isPlaying) {
                        spotifyPlayer.pause(spotifyPlayerOperationCallback);
                        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                    } else if (playbackState.positionMs > 0) {
                        // TODO how to tell if player is paused, just position > 0? or is there an actual pause state?
                        spotifyPlayer.resume(spotifyPlayerOperationCallback);
                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                    }
                }
            }
        });

        getRecommendationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSpotifyRecommendations();
            }
        });

        spotifyPlayerNotificationCallback = new Player.NotificationCallback() {
            @Override
            public void onPlaybackEvent(PlayerEvent playerEvent) {
                Log.d(Constants.TAG, "Spotify player notif callback: playback event received: " + playerEvent.name());
                handleSpotifyEvent(playerEvent);
            }

            @Override
            public void onPlaybackError(Error error) {
                Log.d(Constants.TAG, "Spotify player notif callback: playback error received: " + error.name());
                handleSpotifyError(error);
            }
        };

        // recycler view
        trackListRecyclerView = (RecyclerView) findViewById(R.id.track_list_recycler);
        trackListRecyclerView.setHasFixedSize(true);
        trackListLayoutManager = new LinearLayoutManager(this);
        trackListRecyclerView.setLayoutManager(trackListLayoutManager);
        trackList = new ArrayList<>();
        trackListAdapter = new TrackListAdapter(MainActivity.this, trackList);
        trackListRecyclerView.setAdapter(trackListAdapter);

        // get track list click events as observable
        // TODO do I need to unsubscribe from this somehow?
        trackListAdapter.asObservable().subscribe(new Action1<Track>() {
            @Override
            public void call(Track track) {
                // TODO make other adapter list items not clickable until one is processed?
                Toast.makeText(MainActivity.this, "playing track: " + track.getName(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(MainActivity.this, "Spotify op error: " + error.name(), Toast.LENGTH_SHORT).show();
            }
        };

        spotifyServiceSubscriber = new Subscriber<TrackContainer>() {
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
        };

        MoodGenreApplication application = (MoodGenreApplication) this.getApplication();
        spotifyService = application.getSpotifyService();

        checkPerms();

        initAmazonAuth();

        initSpotifyAuth();
    }

    @Override
    protected void onDestroy() {

        Spotify.destroyPlayer(this);

        // unsubscribe rxjava network call
        spotifyServiceSubscriber.unsubscribe();

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        Log.d(Constants.TAG, "onActivityResult");

        // image picker callback
        if (requestCode == EasyImageConfig.REQ_PICK_PICTURE_FROM_GALLERY) {
            Log.d(Constants.TAG, "IMAGE_PICKER_REQUEST_CODE match, process response");

            EasyImage.handleActivityResult(requestCode, resultCode, intent, this, new DefaultCallback() {
                @Override
                public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                    // TODO error
                    Log.e(Constants.TAG, "EasyImage error:" + e.getMessage());
                }

                @Override
                public void onImagePicked(File imageFile, EasyImage.ImageSource source, int type) {
                    // TODO picked a file
                    Log.d(Constants.TAG, "EasyImage picked file" + imageFile.getName());
                }
            });
        }

        // Spotify auth callback
        if (requestCode == SPOTIFY_AUTH_REQUEST_CODE) {
            Log.d(Constants.TAG, "SPOTIFY_AUTH_REQUEST_CODE match, process response");

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            Log.d(Constants.TAG, "response type: " + response.getType().toString());

            authStateLabel.setText(response.getType().toString());

            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                spotifyAccessToken = response.getAccessToken();
                initSpotifyPlayer(spotifyAccessToken);
            }
        }
    }

    //
    // private
    //

    private void checkPerms() {
        // NAMMU permissions helper
        Nammu.init(this);
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        Log.d(Constants.TAG, "permission check:" + permissionCheck);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Nammu.askForPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE, new PermissionCallback() {
                @Override
                public void permissionGranted() {
                    chooseImageButton.setEnabled(true);
                }

                @Override
                public void permissionRefused() {
                    Toast.makeText(MainActivity.this, "cannot use image selector, permission not granted", Toast.LENGTH_SHORT).show();
                    chooseImageButton.setEnabled(false);
                }
            });
        } else {
            chooseImageButton.setEnabled(true);
        }
    }

    //
    // AMZN
    //

    private void initAmazonAuth() {
        // AMZN
        // Initialize the Amazon Cognito credentials provider
        final CognitoCachingCredentialsProvider credentialsProvider =
                new CognitoCachingCredentialsProvider(getApplicationContext(),
                        Constants.AMZN_IDENTITY_POOL_ID,
                        Regions.US_EAST_2
                );
        new Thread(new Runnable() {
            public void run() {
                String identityId = credentialsProvider.getIdentityId();
                Log.d(Constants.TAG, "AMZN ID is " + identityId);
            }
        }).start();

        /*
        // Initialize the Cognito Sync client
        CognitoSyncManager syncClient = new CognitoSyncManager(
                getApplicationContext(),
                Regions.US_EAST_2, // Region
                credentialsProvider);
        // Create a record in a dataset and synchronize with the server
        Dataset dataset = syncClient.openOrCreateDataset("myDataset");
        dataset.put("myKey", "myValue");
        dataset.synchronize(new DefaultSyncCallback() {
            @Override
            public void onSuccess(Dataset dataset, List newRecords) {
                //Your handler code here
            }
        });
        */
    }

    //
    // SPOT
    //

    private void getSpotifyRecommendations() {
        Observable<TrackContainer> observable = spotifyService.getRecommendations("Bearer " + spotifyAccessToken, "alternative");
        Subscription subscription = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(spotifyServiceSubscriber);
    }

    private void initSpotifyAuth() {
        // TODO check if already authed?
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(
                        Constants.SPOTIFY_CLIENT_ID,
                        AuthenticationResponse.Type.TOKEN,
                        Constants.SPOTIFY_REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, request);
    }

    private void initSpotifyPlayer(String oauthToken) {
        Config playerConfig = new Config(this, oauthToken, Constants.SPOTIFY_CLIENT_ID);
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
                break;
            case kSpPlaybackNotifyMetadataChanged:
                break;
            case kSpPlaybackNotifyNext:
                break;
            case kSpPlaybackNotifyPause:
                break;
            case kSpPlaybackNotifyPlay:
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