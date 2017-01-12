package moodgenre.spotify.com.moodgenre;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.Vision.Builder;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.Status;
import com.google.common.collect.ImmutableList;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import moodgenre.spotify.com.moodgenre.model.Genre;
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
import rx.schedulers.Schedulers;
import utils.ImageUtils;
import utils.PackageManagerUtils;

public class PathImageDetectionActivity extends BaseActivity {

    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private Button chooseImageButton;
    private Button gotoPlayerButton;
    private ImageView selectedImage;
    private TextView sentimentDetectedLabel;

    private String spotifyAccessToken;
    private SpotifyService spotifyService;
    
    private boolean trackListCreated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_image_detection);

        chooseImageButton = (Button) findViewById(R.id.button_choose_image);
        gotoPlayerButton = (Button) findViewById(R.id.button_goto_player);
        selectedImage = (ImageView) findViewById(R.id.image_selected);
        sentimentDetectedLabel = (TextView) findViewById(R.id.label_sentiment_detected);

        gotoPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!trackListCreated) {
                    Toast.makeText(PathImageDetectionActivity.this, "Playlist empty, cannot proceed to player", Toast.LENGTH_LONG).show();
                    return;
                }                
                startActivity(new Intent(PathImageDetectionActivity.this, PlayerActivity.class));
            }
        });
        
        chooseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trackListCreated = false;
                EasyImage.openGallery(PathImageDetectionActivity.this, EasyImageConfig.REQ_PICK_PICTURE_FROM_GALLERY);
            }
        });

        checkPerms();

        spotifyService = application.getSpotifyService();
        spotifyAccessToken = application.getSpotifyAccessToken();
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
                    Log.e(Constants.TAG, "EasyImage error:" + e.getMessage());
                }

                @Override
                public void onImagePicked(File imageFile, EasyImage.ImageSource source, int type) {
                    Log.d(Constants.TAG, "EasyImage picked file" + imageFile.getName());

                    sentimentDetectedLabel.setText("");
                    
                    // the chosen image
                    Picasso.with(PathImageDetectionActivity.this)
                            .load(imageFile)
                            .fit()
                            .centerCrop()
                            .into(selectedImage);
                    
                    
                    Observer<List<FaceAnnotation>> processImageRequestObserver = new Observer<List<FaceAnnotation>>() {
                        @Override
                        public void onCompleted() {
                            Log.d(Constants.TAG, this.getClass().getSimpleName() + " visionProcessRequest COMPLETED");
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(Constants.TAG, this.getClass().getSimpleName() + " visionProcessRequest ERROR:" + e.getMessage());
                        }

                        @Override
                        public void onNext(List<FaceAnnotation> faceAnnotations) {
                            Log.d(Constants.TAG, this.getClass().getSimpleName() + " visionProcessRequest faceAnnotations:" + faceAnnotations.size());
                            FaceAnnotation faceAnnotation = faceAnnotations.get(0);
                            
                            // google can do sorrow, joy, anger, surprise
                            // likelihood is UNKNOWN, VERY_UNLIKELY, UNLIKELY, POSSIBLE, LIKELY, VERY_LIKELY
                            // (api docs refer to an enum for this but can't find it in javadoc index, fucking strings)
                            
                            // build genreList using detected facial sentiment                            
                            List<Genre> genreList = new ArrayList<Genre>();
                            if (faceAnnotation.getDetectionConfidence() > 0.5) {
                                // joy, sorrow, anger
                                if (isLikely(faceAnnotation.getAngerLikelihood())) {
                                    // ANGRY
                                    Log.d(Constants.TAG, this.getClass().getSimpleName() + " face ANGRY");
                                    sentimentDetectedLabel.setText("Detected: ANGER");
                                    genreList.add(Genre.ANGRY);                                    
                                } else if (isLikely(faceAnnotation.getSorrowLikelihood())) {
                                    // SORROW
                                    Log.d(Constants.TAG, this.getClass().getSimpleName() + " face SORROWFUL");
                                    sentimentDetectedLabel.setText("Detected: SORROW");
                                    genreList.add(Genre.SAD);                                    
                                } else if (isLikely(faceAnnotation.getJoyLikelihood())) {
                                    // JOY
                                    Log.d(Constants.TAG, this.getClass().getSimpleName() + " face JOYFUL");
                                    sentimentDetectedLabel.setText("Detected: JOY");
                                    genreList.add(Genre.HAPPY);
                                }                                
                            } else {
                                Log.d(Constants.TAG, this.getClass().getSimpleName() + " face confidence low, giving up");
                                Toast.makeText(PathImageDetectionActivity.this, "face confidence low, giving up", Toast.LENGTH_LONG).show();
                            }       
                            
                            if (genreList.size() > 0) {
                                genPlaylist(genreList);
                                Toast.makeText(PathImageDetectionActivity.this, "Playlist created, use goto playlist button...", Toast.LENGTH_LONG).show();                                
                            } else {
                                Log.d(Constants.TAG, this.getClass().getSimpleName() + " face sentiment not detected, giving up");
                                Toast.makeText(PathImageDetectionActivity.this, "Sentiment not detected", Toast.LENGTH_LONG).show();                                
                            }                            
                        }
                    };

                    observeImageRequest(imageFile).subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(processImageRequestObserver);

                }
            });
        }
    }

    //
    // private
    //
    
    private void genPlaylist(List<Genre> genreList) {
        
        Log.d(Constants.TAG, "genPlaylist");
        
        String genreListString = TextUtils.join(",", genreList);
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
                        List<Track> trackList = trackContainer.getTracks();
                        application.setPlaylist(trackList);
                        trackListCreated = true;
                    }
                });
    }
    
    private boolean isLikely(String text) {
        if (text != null && text.equals("LIKELY") || text.equals("VERY_LIKELY")) {
            return true;
        }
        return false;        
    }

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
    // GOOG
    //

    private Observable<List<FaceAnnotation>> observeImageRequest(final File file) {
        return Observable.create(new Observable.OnSubscribe<List<FaceAnnotation>>() {
            @Override
            public void call(Subscriber<? super List<FaceAnnotation>> subscriber) {
                List<FaceAnnotation> result = processImageRequest(file);
                subscriber.onNext(result);
                subscriber.onCompleted();
            }
        });
    }

    private List<FaceAnnotation> processImageRequest(File file) {
        
        List<FaceAnnotation> faceAnnotations = new ArrayList<>();

        Log.d(Constants.TAG, this.getClass().getSimpleName() + " visionProcessRequest for file:" + file.getName());

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(Constants.GCP_API_KEY) {
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);
                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        Builder builder = new Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        List<Feature> featureList = new ArrayList<>();
        Feature faceDetectionFeature = new Feature();
        faceDetectionFeature.setType("FACE_DETECTION");
        faceDetectionFeature.setMaxResults(5);
        featureList.add(faceDetectionFeature);

        // Cloud Vision API recommends 640x480 for "most cases"
        Bitmap bitmap = ImageUtils.decodeSampledBitmapFromFile(file.getAbsolutePath(), 640, 480);
        Image image = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        image.encodeContent(imageBytes);

        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setFeatures(featureList);
        annotateImageRequest.setImage(image);        
        
        
        // one of the worst APIs ever created
        // come on google, to make a single I have to inject it into a batch (names and stringly typed and clumsy and sucks)
        try {
            Vision.Images.Annotate annotate = vision.images()
                    .annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(annotateImageRequest)));
            annotate.setDisableGZipContent(true);

            Log.d(Constants.TAG, "created Cloud Vision request object, sending request");

            BatchAnnotateImagesResponse batchResponse = annotate.execute();
            List<AnnotateImageResponse> annotatedImageResponses = batchResponse.getResponses();
            if (annotatedImageResponses != null && !annotatedImageResponses.isEmpty()) {                
                AnnotateImageResponse response = batchResponse.getResponses().get(0);   // clumsy 
                Status status = response.getError(); // status is error? wtf
                Log.d(Constants.TAG, this.getClass().getSimpleName() + " response Status:" + status);
                
                // add all face annotations to resp
                faceAnnotations.addAll(response.getFaceAnnotations());
            } else {
                Log.e(Constants.TAG, this.getClass().getSimpleName() + " batch response error, no internal responses");
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, this.getClass().getSimpleName() + " ERROR with image process request:" + e.getMessage());
        }
        
        return faceAnnotations;
    }
}
