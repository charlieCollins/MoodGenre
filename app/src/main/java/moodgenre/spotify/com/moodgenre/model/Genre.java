package moodgenre.spotify.com.moodgenre.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by charliecollins on 12/27/16.
 */

public enum Genre {

    // TODO map all the Spotify genres, see NOTES.txt (from get-recommendations)

    // names based on Amazon Rekog emotions, and mapped to Spotify genres
    HAPPY(Arrays.asList("happy")),
    SAD(Arrays.asList("sad")),
    ANGRY(Arrays.asList("metal", "metal-core", "harcore")),
    CONFUSED(Arrays.asList("country")), // because no
    DISGUSTED(Arrays.asList("edm", "techno")), // because my friends are disgusted I listen to this
    SURPRISED(Arrays.asList("k-pop")),
    CALM(Arrays.asList("chill", "sleep"));

    private List<String> emotions;

    private Genre(final List<String> emotions) {
        this.emotions = emotions;
    }

    public List<String> getEmotions() {
        return Collections.unmodifiableList(this.emotions);
    }

}
