package br.com.luiscamara.roadqualitymonitor.data.services;

import android.arch.persistence.room.Transaction;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.inject.Inject;

import br.com.luiscamara.roadqualitymonitor.data.DAOs.TrackDAO;
import br.com.luiscamara.roadqualitymonitor.data.DAOs.VerticalAccelerationReadingDAO;
import br.com.luiscamara.roadqualitymonitor.data.TrackDatabase;
import br.com.luiscamara.roadqualitymonitor.data.models.Track;
import br.com.luiscamara.roadqualitymonitor.data.models.VerticalAccelerationReading;

public class TrackService {
    private static TrackService instance = null;
    private Context context;

    public TrackService(Context context) {
        this.context = context;
    }

    public static TrackService getInstance(Context context) {
        if(instance == null)
            instance = new TrackService(context);

        return instance;
    }

    @Transaction
    public void insert(Track t) {
        long newTrackId = TrackDatabase.getInstance(context).trackDAO().insert(t);
        for(VerticalAccelerationReading var : t.getVerticalAccelerationReadings()) {
            var.setTrackId(newTrackId);
            TrackDatabase.getInstance(context).verticalAccelerationReadingDAO().insert(var);
        }
    }

    @Transaction
    public List<Track> list() {
        List<Track> tracks = TrackDatabase.getInstance(context).trackDAO().list();
        for(Track t : tracks) {
            t.setVerticalAccelerationReadings(TrackDatabase.getInstance(context).verticalAccelerationReadingDAO().list(t.getId()));
        }

        return tracks;
    }

    @Transaction
    public void remove(Track t) {
        TrackDatabase.getInstance(context).trackDAO().remove(t);
    }

    @Transaction
    public void clear() {
        TrackDatabase.getInstance(context).verticalAccelerationReadingDAO().truncate();
        TrackDatabase.getInstance(context).trackDAO().truncate();
    }
}
