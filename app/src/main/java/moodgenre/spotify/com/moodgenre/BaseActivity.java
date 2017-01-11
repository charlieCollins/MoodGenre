package moodgenre.spotify.com.moodgenre;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class BaseActivity extends Activity  {

    protected MoodGenreApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, this.getClass().getSimpleName() + " onCreate");       
        
        application = (MoodGenreApplication) this.getApplication();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(Constants.TAG, this.getClass().getSimpleName() + " onStart");
    }

    @Override
    protected void onDestroy() {
        Log.d(Constants.TAG, this.getClass().getSimpleName() + " onCreate");
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Log.d(Constants.TAG, this.getClass().getSimpleName() + " onActivityResult");
    }
}