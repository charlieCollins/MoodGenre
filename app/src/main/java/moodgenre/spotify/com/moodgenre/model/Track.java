package moodgenre.spotify.com.moodgenre.model;

import java.util.List;

/**
 * Created by charliecollins on 1/4/17.
 */

public class Track {

    // top level class returned by curl -X GET "https://api.spotify.com/v1/recommendations?seed_genres=alternative&limit=2
    // reccomemendations to get seed genres

    // track contains direct info and a combo of album and artist (mainly it looks like)

    private Album album;
    private List<Artist> artists;

    private String name;
    private String id;
    private String uri;
    private String href;

    // explicit
    // duration
    // disc_number
    // track_number
    // external ids
    // external urls
    // popularity
    // type


    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public List<Artist> getArtists() {
        return artists;
    }

    public void setArtists(List<Artist> artists) {
        this.artists = artists;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    @Override
    public String toString() {
        return "Track{" +
                "album=" + album +
                ", artists=" + artists +
                ", name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", uri='" + uri + '\'' +
                ", href='" + href + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Track track = (Track) o;

        if (album != null ? !album.equals(track.album) : track.album != null) return false;
        if (artists != null ? !artists.equals(track.artists) : track.artists != null) return false;
        if (name != null ? !name.equals(track.name) : track.name != null) return false;
        if (id != null ? !id.equals(track.id) : track.id != null) return false;
        if (uri != null ? !uri.equals(track.uri) : track.uri != null) return false;
        return href != null ? href.equals(track.href) : track.href == null;

    }

    @Override
    public int hashCode() {
        int result = album != null ? album.hashCode() : 0;
        result = 31 * result + (artists != null ? artists.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (href != null ? href.hashCode() : 0);
        return result;
    }
}
