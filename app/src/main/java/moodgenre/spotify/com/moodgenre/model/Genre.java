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
    HAPPY(Arrays.asList("HAPPY")),
    SAD(Arrays.asList("SAD")),
    ANGRY(Arrays.asList("METAL", "METAL-CORE", "BLACK-METAL", "HARDCORE")),
    CONFUSED(Arrays.asList("COUNTRY")), // because no
    DISGUSTED(Arrays.asList("EDM", "TECHNO")), // because my friends are disgusted I listen to this
    SURPRISED(Arrays.asList("K-POP")),
    CALM(Arrays.asList("CHILL", "SLEEP"));

    private List<String> emotions;

    private Genre(final List<String> emotions) {
        this.emotions = emotions;
    }

    public List<String> getEmotions() {
        return Collections.unmodifiableList(this.emotions);
    }

}
