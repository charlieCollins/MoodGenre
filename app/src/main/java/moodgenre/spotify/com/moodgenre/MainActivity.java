package moodgenre.spotify.com.moodgenre;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.regions.Regions;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;

import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;

import java.io.File;
import java.util.List;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;
import pl.aprilapps.easyphotopicker.EasyImageConfig;
import pl.tajchert.nammu.Nammu;
import pl.tajchert.nammu.PermissionCallback;

import static android.R.attr.data;

public class MainActivity extends Activity implements
        SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    private static final int SPOTIFY_AUTH_REQUEST_CODE = 1738;

    private Button toggleAuthButton;
    private Button chooseImageButton;
    private TextView label1;

    private Player mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleAuthButton = (Button) findViewById(R.id.button_toggle_spotify_auth);
        chooseImageButton = (Button) findViewById(R.id.button_choose_image);
        label1 = (TextView) findViewById(R.id.textView1);

        toggleAuthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "click", Toast.LENGTH_SHORT).show();
            }
        });
        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ///Intent intent = new Intent(Intent.ACTION_PICK,
                ///        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                ///startActivityForResult(intent, IMAGE_PICKER_REQUEST_CODE);

                EasyImage.openGallery(MainActivity.this, EasyImageConfig.REQ_PICK_PICTURE_FROM_GALLERY);
            }
        });
        chooseImageButton.setEnabled(false);

        // permissions helper
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

        Log.d(Constants.TAG, "onCreate");

        // TODO check if already authed?
        // Spotify auth
        AuthenticationRequest.Builder builder =
                new AuthenticationRequest.Builder(
                        Constants.SPOTIFY_CLIENT_ID,
                        AuthenticationResponse.Type.TOKEN,
                        Constants.SPOTIFY_REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, request);

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

        Log.d(Constants.TAG, "AuthenticationClient LoginActivity opened");
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

            Log.d(Constants.TAG, "response: " + response.toString());

            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                Config playerConfig = new Config(this, response.getAccessToken(), Constants.SPOTIFY_CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver() {
                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer) {
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(Constants.TAG, "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent) {
        Log.d(Constants.TAG, "Playback event received: " + playerEvent.name());
        switch (playerEvent) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(Error error) {
        Log.d(Constants.TAG, "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() {
        Log.d(Constants.TAG, "User logged in");

        mPlayer.playUri(null, "spotify:track:7BKLCZ1jbUBVqRi2FVlTVw", 0, 0);

    }

    @Override
    public void onLoggedOut() {
        Log.d(Constants.TAG, "User logged out");
    }

    @Override
    public void onLoginFailed(Error e) {
        Log.d(Constants.TAG, "Login failed: " + e.toString());
    }

    @Override
    public void onTemporaryError() {
        Log.d(Constants.TAG, "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d(Constants.TAG, "Received connection message: " + message);
    }
}