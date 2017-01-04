package moodgenre.spotify.com.moodgenre.model;

import java.util.List;

/**
 * Created by charliecollins on 1/4/17.
 */

public class TrackContainer {

    private List<Track> tracks;

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    @Override
    public String toString() {
        return "TrackContainer{" +
                "tracks=" + tracks +
                '}';
    }
}
