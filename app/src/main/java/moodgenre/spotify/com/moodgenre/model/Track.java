package moodgenre.spotify.com.moodgenre.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by charliecollins on 1/4/17.
 */

public class Track {

    // top level class returned by curl -X GET "https://api.spotify.com/v1/recommendations?seed_genres=alternative&limit=2
    // reccomemendations to get seed genres

    // tracks is an array of album+artist combination objects

    @SerializedName("tracks")
    private List<Album> albums;




}
