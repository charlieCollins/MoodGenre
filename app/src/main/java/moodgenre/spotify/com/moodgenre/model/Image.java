package moodgenre.spotify.com.moodgenre.model;

/**
 * Created by charliecollins on 1/4/17.
 */

public class Image {

    private int height;
    private int width;
    private String url;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "Image{" +
                "height=" + height +
                ", width=" + width +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Image image = (Image) o;

        if (height != image.height) return false;
        if (width != image.width) return false;
        return url != null ? url.equals(image.url) : image.url == null;

    }

    @Override
    public int hashCode() {
        int result = height;
        result = 31 * result + width;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
