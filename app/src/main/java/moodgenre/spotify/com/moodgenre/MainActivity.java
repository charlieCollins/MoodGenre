package moodgenre.spotify.com.moodgenre;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Spotify;


public class MainActivity extends BaseActivity  {

    private static final int SPOTIFY_AUTH_REQUEST_CODE = 1738;

    private ImageView buttonPathImageDetection;
    private ImageView buttonPathGenreSelection;
    private Button buttonAuthenticate;
    private LinearLayout layoutAuthenticated;
    private LinearLayout layoutUnAuthenticated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonPathImageDetection = (ImageView) findViewById(R.id.button_path_image_detection);
        buttonPathGenreSelection = (ImageView) findViewById(R.id.button_path_genre_selection);
        buttonAuthenticate = (Button) findViewById(R.id.button_authenticate);
        layoutAuthenticated = (LinearLayout) findViewById(R.id.layout_authenticated);
        layoutUnAuthenticated = (LinearLayout) findViewById(R.id.layout_unauthenticated); 

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
        
        buttonAuthenticate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initSpotifyAuth();
            }
        });        
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

        // Spotify auth callback
        if (requestCode == SPOTIFY_AUTH_REQUEST_CODE) {
            Log.d(Constants.TAG, "SPOTIFY_AUTH_REQUEST_CODE match, process response");

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            Log.d(Constants.TAG, "response type: " + response.getType().toString());

            AuthenticationResponse.Type responseType = response.getType();
            
            switch(responseType) {
                case TOKEN:
                    String spotifyAccessToken = response.getAccessToken();
                    application.setSpotifyAccessToken(spotifyAccessToken);
                    transitionUI(true);                    
                    break;
                default:
                    // this will catch ERROR, UNKNOWN, CODE (which I don't know what to with any)
                    Toast.makeText(MainActivity.this, "Spotify auth request failed:" 
                            + responseType.toString() + " " + response.getError().toString(), Toast.LENGTH_LONG).show();
                    Log.d(Constants.TAG, "response error (if any): " + response.getError());
                    transitionUI(false);
                    break;
            }           
        }
    }

    //
    // private
    //

    //
    // SPOT
    //

    private void transitionUI(boolean authenticated) {
        if (authenticated) {
            if (layoutAuthenticated.getVisibility() == View.VISIBLE) {
                return;
            } else {
                layoutUnAuthenticated.setVisibility(View.GONE);
                layoutAuthenticated.setVisibility(View.VISIBLE);                
            }
        } else {
            if (layoutAuthenticated.getVisibility() == View.VISIBLE) {
                layoutAuthenticated.setVisibility(View.GONE);
                layoutUnAuthenticated.setVisibility(View.VISIBLE);
            } else {
                return;
            }
        }
    }
    
    private void initSpotifyAuth() {
        String spotifyAccessToken = application.getSpotifyAccessToken();
        if (spotifyAccessToken == null) {
            Log.d(Constants.TAG, "spotifyAccessToken NOT present, make request");
            AuthenticationRequest.Builder builder =
                    new AuthenticationRequest.Builder(
                            application.getSpotifyClientId(),
                            AuthenticationResponse.Type.TOKEN,
                            application.getSpotifyCallbackUri());
            builder.setScopes(new String[]{"user-read-private", "streaming"});
            AuthenticationRequest request = builder.build();
            AuthenticationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, request);
            transitionUI(false);
        } else {
            Log.d(Constants.TAG, "spotifyAccessToken present, no request needed:" + spotifyAccessToken);
            transitionUI(true);
        }
    }
}