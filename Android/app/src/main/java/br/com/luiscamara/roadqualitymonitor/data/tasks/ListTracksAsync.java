package br.com.luiscamara.roadqualitymonitor.data.tasks;

import android.content.Context;
import android.os.AsyncTask;

import java.util.List;

import br.com.luiscamara.roadqualitymonitor.data.models.Track;
import br.com.luiscamara.roadqualitymonitor.data.services.TrackService;

public class ListTracksAsync extends AsyncTask<Context, Void, List<Track>> {
    private OnTaskCompleted delegate = null;

    public ListTracksAsync(OnTaskCompleted delegate) {
        this.delegate = delegate;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected List<Track> doInBackground(Context... context) {
        List<Track> tracks = TrackService.getInstance(context[0]).list();
        return tracks;
    }

    @Override
    protected void onPostExecute(List<Track> tracks) {
        super.onPostExecute(tracks);
        delegate.onTaskCompleted(tracks);
    }
}
