package moodgenre.spotify.com.moodgenre.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import moodgenre.spotify.com.moodgenre.R;
import moodgenre.spotify.com.moodgenre.model.Track;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by charliecollins on 1/4/17.
 */


public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.ViewHolder> {

    private Context context;
    private PublishSubject<Track> onClickSubject;
    private List<Track> tracks;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;
        public ImageView imageView;
        public ViewHolder(View v) {
            super(v);
            textView = (TextView) v.findViewById(R.id.track_name);
            imageView = (ImageView) v.findViewById(R.id.album_image);
        }
    }

    public TrackListAdapter(Context context, List<Track> tracks) {
        this.context = context;
        this.tracks = tracks;
        this.onClickSubject = PublishSubject.create();
    }

    @Override
    public TrackListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final Track track = tracks.get(position);

        holder.textView.setText(track.getName());

        if (track.getAlbum() != null && track.getAlbum().getImages() != null && track.getAlbum().getImages().size() > 0) {
            Picasso.with(context)
                    .load(track.getAlbum().getImages().get(0).getUrl())
                    .fit()
                    .centerCrop()
                    .placeholder(android.R.drawable.ic_media_play)
                    .into(holder.imageView);
        }


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSubject.onNext(track);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }
    
    // used to prime the pump, before an item selection is made, if necessary
    public Track getFirstTrack() {
        if (tracks == null || tracks.isEmpty()) {
            return null;
        }
        return tracks.get(0);
    }

    public Observable<Track> asObservable(){
        return onClickSubject.asObservable();
    }
}



