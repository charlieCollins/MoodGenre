package moodgenre.spotify.com.moodgenre;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Spotify;


public class MainActivity extends Activity  {

    private static final int SPOTIFY_AUTH_REQUEST_CODE = 1738;

    private MoodGenreApplication application;

    private ImageView buttonPathImageDetection;
    private ImageView buttonPathGenreSelection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(Constants.TAG, "onCreate");

        buttonPathImageDetection = (ImageView) findViewById(R.id.button_path_image_detection);
        buttonPathGenreSelection = (ImageView) findViewById(R.id.button_path_genre_selection);

        buttonPathImageDetection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //EasyImage.openGallery(MainActivity.this, EasyImageConfig.REQ_PICK_PICTURE_FROM_GALLERY);
                buttonPathImageDetection.setColorFilter(0xFF000000, PorterDuff.Mode.MULTIPLY);
                startActivity(new Intent(MainActivity.this, PathImageDetectionActivity.class));

            }
        });

        buttonPathGenreSelection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonPathGenreSelection.setColorFilter(0xFF000000, PorterDuff.Mode.MULTIPLY);
                startActivity(new Intent(MainActivity.this, PathGenreSelectionActivity.class));
            }
        });

        application = (MoodGenreApplication) this.getApplication();

        initAmazonAuth();

        initSpotifyAuth();
    }

    @Override
    protected void onStart() {
        super.onStart();
        buttonPathImageDetection.clearColorFilter();
        buttonPathGenreSelection.clearColorFilter();
    }

    @Override
    protected void onDestroy() {

        Spotify.destroyPlayer(this);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        Log.d(Constants.TAG, "onActivityResult");

        // Spotify auth callback
        if (requestCode == SPOTIFY_AUTH_REQUEST_CODE) {
            Log.d(Constants.TAG, "SPOTIFY_AUTH_REQUEST_CODE match, process response");

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            Log.d(Constants.TAG, "response type: " + response.getType().toString());

            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                String spotifyAccessToken = response.getAccessToken();
                application.setSpotifyAccessToken(spotifyAccessToken);
            }
        }
    }

    //
    // private
    //

    //
    // AMZN
    //

    private void initAmazonAuth() {
        // AMZN
        // Initialize the Amazon Cognito credentials provider
        // TODO check if already authed/cache/etc?
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

    private void initSpotifyAuth() {
        String spotifyAccessToken = application.getSpotifyAccessToken();
        if (spotifyAccessToken == null) {
            Log.d(Constants.TAG, "spotifyAccessToken NOT present, make request");
            AuthenticationRequest.Builder builder =
                    new AuthenticationRequest.Builder(
                            Constants.SPOTIFY_CLIENT_ID,
                            AuthenticationResponse.Type.TOKEN,
                            Constants.SPOTIFY_REDIRECT_URI);
            builder.setScopes(new String[]{"user-read-private", "streaming"});
            AuthenticationRequest request = builder.build();
            AuthenticationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, request);
        } else {
            Log.d(Constants.TAG, "spotifyAccessToken present, no request needed:" + spotifyAccessToken);
        }
    }
}