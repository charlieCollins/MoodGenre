package moodgenre.spotify.com.moodgenre.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import moodgenre.spotify.com.moodgenre.R;
import moodgenre.spotify.com.moodgenre.model.Track;

/**
 * Created by charliecollins on 1/4/17.
 */


public class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.ViewHolder> {

    private List<Track> tracks;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textView;

        public ViewHolder(View v) {
            super(v);
            textView = (TextView) v.findViewById(R.id.track_name);
        }
    }

    public TrackListAdapter(List<Track> tracks) {
        this.tracks = tracks;
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
        Track track = tracks.get(position);
        holder.textView.setText(track.getName());
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }
}



