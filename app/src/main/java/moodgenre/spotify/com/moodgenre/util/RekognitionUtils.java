package moodgenre.spotify.com.moodgenre.util;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by charliecollins on 12/28/16.
 */

public class RekognitionUtils {

    // TODO refactor this into an actual util
    public static void sendBytesExample(AWSCredentials credentials) throws Exception {

        InputStream inputStream = new FileInputStream(new File("inputfile.jpg"));
        ByteBuffer imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));

        DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(new Image()
                        .withBytes(imageBytes))
                .withMaxLabels(10)
                .withMinConfidence(77F);

        AmazonRekognitionClient rekognitionClient = new AmazonRekognitionClient(credentials);
        rekognitionClient.setEndpoint("https://rekognition.us-east-1.amazonaws.com");
        rekognitionClient.setSignerRegionOverride("us-east-1");

        DetectLabelsResult result = rekognitionClient.detectLabels(request);
        ObjectMapper objectMapper = new ObjectMapper();
        System.out.println("Result = " + objectMapper.writeValueAsString(result));
    }




}
