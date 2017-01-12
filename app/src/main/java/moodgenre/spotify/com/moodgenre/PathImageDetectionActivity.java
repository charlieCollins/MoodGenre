package moodgenre.spotify.com.moodgenre;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;

import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;
import pl.aprilapps.easyphotopicker.EasyImageConfig;
import pl.tajchert.nammu.Nammu;
import pl.tajchert.nammu.PermissionCallback;


public class PathImageDetectionActivity extends BaseActivity  {

    private Button chooseImageButton;
    private ImageView selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_image_detection);

        chooseImageButton = (Button) findViewById(R.id.button_choose_image);
        selectedImage = (ImageView) findViewById(R.id.image_selected);

        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EasyImage.openGallery(PathImageDetectionActivity.this, EasyImageConfig.REQ_PICK_PICTURE_FROM_GALLERY);
            }
        });

        checkPerms();

        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

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


                    Picasso.with(PathImageDetectionActivity.this)
                            .load(imageFile)
                            .fit()
                            .centerCrop()
                            .into(selectedImage);

                }
            });
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
                    Toast.makeText(PathImageDetectionActivity.this, "cannot use image selector, permission not granted", Toast.LENGTH_SHORT).show();
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

   

}