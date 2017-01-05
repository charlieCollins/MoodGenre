package moodgenre.spotify.com.moodgenre;

import org.junit.Assert;
import org.junit.Test;

import moodgenre.spotify.com.moodgenre.model.Genre;


public class GenreTest {

    @Test
    public void lookupCorrect() throws Exception {
        Genre genre = Genre.valueOf("SAD");
        Assert.assertTrue(genre.getEmotions().get(0).equals("SAD"));

        genre = Genre.valueOf("ANGRY");
        Assert.assertTrue(genre.getEmotions().size() == 4);
    }
}