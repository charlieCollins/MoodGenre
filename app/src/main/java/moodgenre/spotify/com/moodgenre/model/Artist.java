package moodgenre.spotify.com.moodgenre.model;

/**
 * Created by charliecollins on 1/4/17.
 */

public class Artist {

    private String name;
    private String id;
    private String uri;
    private String href;

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
        return "Artist{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", uri='" + uri + '\'' +
                ", href='" + href + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artist artist = (Artist) o;

        if (name != null ? !name.equals(artist.name) : artist.name != null) return false;
        if (id != null ? !id.equals(artist.id) : artist.id != null) return false;
        if (uri != null ? !uri.equals(artist.uri) : artist.uri != null) return false;
        return href != null ? href.equals(artist.href) : artist.href == null;

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        result = 31 * result + (href != null ? href.hashCode() : 0);
        return result;
    }
}
