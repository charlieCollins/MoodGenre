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
import android.widget.ProgressBar;
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
    
    private static final String LIKELY = "LIKELY";
    private static final String UNLIKELY = "UNLIKELY";
    private static final String VERY_LIKELY = "VERY_LIKELY";
    private static final String POSSIBLE = "POSSIBLE";
    
    private static final double FACE_CONFIDENCE_THRESHOLD = 0.6;
    private static final int IMAGE_DESIRED_WIDTH = 640;
    private static final int IMAGE_DESIRED_HEIGHT = 480;

    private Button buttonChooseImage;
    private Button buttonGotoPlayer;
    private ImageView imageViewSelected;
    private TextView labelSentiment;
    private ProgressBar progressBar;

    private String spotifyAccessToken;
    private SpotifyService spotifyService;
    
    private boolean trackListCreated;

    private class FaceAnnotationData {
        public FaceAnnotation faceAnnotation;
        public Genre genre;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_path_image_detection);

        buttonChooseImage = (Button) findViewById(R.id.button_choose_image);
        buttonGotoPlayer = (Button) findViewById(R.id.button_goto_player);
        imageViewSelected = (ImageView) findViewById(R.id.image_selected);
        labelSentiment = (TextView) findViewById(R.id.label_sentiment_detected);
        progressBar = (ProgressBar) findViewById(R.id.progressbar1); 

        buttonGotoPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!trackListCreated) {
                    Toast.makeText(PathImageDetectionActivity.this, "Playlist empty, cannot proceed to player", Toast.LENGTH_LONG).show();
                    return;
                }                
                startActivity(new Intent(PathImageDetectionActivity.this, PlayerActivity.class));
            }
        });
        
        buttonChooseImage.setOnClickListener(new View.OnClickListener() {
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

            progressBar.setVisibility(View.VISIBLE);
            labelSentiment.setText("");

            EasyImage.handleActivityResult(requestCode, resultCode, intent, this, new DefaultCallback() {
                @Override
                public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                    Log.e(Constants.TAG, "EasyImage error:" + e.getMessage());
                }

                @Override
                public void onImagePicked(File imageFile, EasyImage.ImageSource source, int type) {
                    Log.d(Constants.TAG, "EasyImage picked file" + imageFile.getName());                   

                    // the chosen image
                    Picasso.with(PathImageDetectionActivity.this)
                            .load(imageFile)
                            .fit()
                            .centerCrop()
                            .into(imageViewSelected);

                    processSelectedImage(imageFile);
                }
            });
        }
    }

    //
    // private
    //
    
    private void processSelectedImage(File imageFile) {

        Observer<List<FaceAnnotation>> processImageRequestObserver = new Observer<List<FaceAnnotation>>() {
            @Override
            public void onCompleted() {
                Log.d(Constants.TAG, this.getClass().getSimpleName() + " visionProcessRequest COMPLETED");
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, this.getClass().getSimpleName() + " visionProcessRequest ERROR:" + e.getMessage());
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onNext(List<FaceAnnotation> faceAnnotations) {
                progressBar.setVisibility(View.GONE);

                Log.d(Constants.TAG, this.getClass().getSimpleName() + " visionProcessRequest faceAnnotations size:" + faceAnnotations.size());

                if (faceAnnotations == null || faceAnnotations.isEmpty()) {
                    Toast.makeText(PathImageDetectionActivity.this, "no faces detected", Toast.LENGTH_LONG).show();
                    labelSentiment.setText("no faces detected");
                    return;
                }
                
                // for each faceannotation get the emotionlist (Strings) that can be passed to recommendation service
                List<FaceAnnotationData> faceAnnotationDataList = new ArrayList<>();
                for (FaceAnnotation faceAnnotation : faceAnnotations) {
                    FaceAnnotationData faceAnnotationData = processFaceAnnotation(faceAnnotation);
                    faceAnnotationDataList.add(faceAnnotationData);
                }

                displayResults(faceAnnotationDataList); 
            } 
        };

        // make net request and observe
        observeImageRequest(imageFile).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(processImageRequestObserver);
    }

    private void displayResults(List<FaceAnnotationData> faceAnnotationDataList) {
        
        if (faceAnnotationDataList == null) {
            return;
        }
        
        boolean detected = false;
        for (FaceAnnotationData data : faceAnnotationDataList) { 
            if (data.genre != null) {
                detected = true;
                labelSentiment.setText("sentiment detected:" + data.genre.toString());
                genPlaylist(data.genre.getEmotions());
                Toast.makeText(PathImageDetectionActivity.this, "Playlist created, use goto player button...", Toast.LENGTH_LONG).show();
                break; // for NOW just break after first face and use that (no aggregate or such)
            }
        }
        
        if (!detected) {
            Toast.makeText(PathImageDetectionActivity.this, "No sentiment detected", Toast.LENGTH_LONG).show();
            labelSentiment.setText("");
        }
    }                    
                    
    private FaceAnnotationData processFaceAnnotation(FaceAnnotation faceAnnotation) {
        Log.d(Constants.TAG, this.getClass().getSimpleName() + " process faceAnnotation:" + faceAnnotation);

        // google can do sorrow, joy, anger, surprise
        // likelihood is UNKNOWN, VERY_UNLIKELY, UNLIKELY, POSSIBLE, LIKELY, VERY_LIKELY
        // (api docs refer to an enum for this but can't find it in javadoc index, damn strings)

        // build genreList using detected facial sentiment                            
        FaceAnnotationData faceAnnotationData = new FaceAnnotationData();
        faceAnnotationData.faceAnnotation = faceAnnotation;
        
        if (faceAnnotation.getDetectionConfidence() > FACE_CONFIDENCE_THRESHOLD) {            
            
            Log.d(Constants.TAG, "   anger:" + faceAnnotation.getAngerLikelihood());
            Log.d(Constants.TAG, "   sorrow:" + faceAnnotation.getSorrowLikelihood());
            Log.d(Constants.TAG, "   joy:" + faceAnnotation.getJoyLikelihood());
            Log.d(Constants.TAG, "   surprise:" + faceAnnotation.getSurpriseLikelihood());
            

            // process in order, anger, sorrow, joy, surprise
            // FUTURE come with faceannotation processing strategy and allow dynamic config
            // FUTURE allow setting switch between "possible" inclusion or not            
            
            if (isLikelyOrPossible(faceAnnotation.getAngerLikelihood())) {
                // ANGRY
                Log.d(Constants.TAG, this.getClass().getSimpleName() + " face ANGRY");
                faceAnnotationData.genre = Genre.ANGRY;
            } else if (isLikelyOrPossible(faceAnnotation.getSorrowLikelihood())) {
                // SORROW
                Log.d(Constants.TAG, this.getClass().getSimpleName() + " face SORROWFUL");
                faceAnnotationData.genre = Genre.SAD;
            } else if (isLikelyOrPossible(faceAnnotation.getJoyLikelihood())) {
                // JOY
                Log.d(Constants.TAG, this.getClass().getSimpleName() + " face JOYFUL");
                faceAnnotationData.genre = Genre.HAPPY;
            } else if (isLikelyOrPossible(faceAnnotation.getSurpriseLikelihood())) {
                // SURPRISE
                Log.d(Constants.TAG, this.getClass().getSimpleName() + " face SURPRISED");
                faceAnnotationData.genre = Genre.SURPRISED;
            }
        } else {
            Log.d(Constants.TAG, this.getClass().getSimpleName() + " face confidence low, giving up");
            ///Toast.makeText(PathImageDetectionActivity.this, "face confidence low, giving up", Toast.LENGTH_LONG).show();
        }
        
        return faceAnnotationData;
    }
    
    private void genPlaylist(List<String> emotionList) {
        
        Log.d(Constants.TAG, "genPlaylist using genreList:" + emotionList);
        
        String genreListString = TextUtils.join(",", emotionList);
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
                        application.setPlaylist(trackContainer.getTracks());
                        trackListCreated = true;
                    }
                });
    }
    
    private boolean isLikelyOrPossible(String text) {
        if (text == null) {
            return false;
        }
        
        if (isLikely(text)) {
            return true;
        }
        if (text.equals(POSSIBLE)) {
            return true;
        }
        return false;
    }
    
    private boolean isLikely(String text) {
        if (text == null) {
            return false;
        }
        if (text.equals(LIKELY) || text.equals(VERY_LIKELY)) {
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
                    buttonChooseImage.setEnabled(true);
                }

                @Override
                public void permissionRefused() {
                    Toast.makeText(PathImageDetectionActivity.this, "cannot use image selector, permission not granted", Toast.LENGTH_SHORT).show();
                    buttonChooseImage.setEnabled(false);
                }
            });
        } else {
            buttonChooseImage.setEnabled(true);
        }
    }

    //
    // GOOG Cloud Vision API 
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
        Bitmap bitmap = ImageUtils.decodeSampledBitmapFromFile(file.getAbsolutePath(), 
                IMAGE_DESIRED_WIDTH, IMAGE_DESIRED_HEIGHT);
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
                if (response.getFaceAnnotations() != null) {
                    faceAnnotations.addAll(response.getFaceAnnotations());
                }
            } else {
                Log.e(Constants.TAG, this.getClass().getSimpleName() + " batch response error, no internal responses");
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, this.getClass().getSimpleName() + " ERROR with image process request:" + e.getMessage());
        }
        
        return faceAnnotations;
    }  
    
   
}
