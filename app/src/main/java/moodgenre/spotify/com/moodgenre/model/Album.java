package moodgenre.spotify.com.moodgenre.model;

import java.util.List;

/**
 * Created by charliecollins on 1/4/17.
 */

public class Album {

    private List<Image> images;

    private String name;
    private String id;
    private String uri;
    private String href;

    public List<Image> getImages() {
        return images;
    }

    public void setImages(List<Image> images) {
        this.images = images;
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
        return "Album{" +
                "images=" + images +
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

        Album album = (Album) o;

        if (images != null ? !images.equals(album.images) : album.images != null) return false;
        if (name != null ? !name.equals(album.name) : album.name != null) return false;
        if (id != null ? !id.equals(album.id) : album.id != null) return false;
        if (uri != null ? !uri.equals(album.uri) : album.uri != null) return false;
        return href != null ? href.equals(album.href) : album.href == null;

    }

    @Override
    public int hashCode() {
        int result = (images != null ? images.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (href != null ? href.hashCode() : 0);
        return result;
    }
}
