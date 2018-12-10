package br.com.luiscamara.roadqualitymonitor.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import br.com.luiscamara.roadqualitymonitor.data.DAOs.ClassifiedTrackDAO;
import br.com.luiscamara.roadqualitymonitor.data.converters.DateConverter;
import br.com.luiscamara.roadqualitymonitor.data.converters.PointConverter;
import br.com.luiscamara.roadqualitymonitor.data.converters.TrackQualityConverter;
import br.com.luiscamara.roadqualitymonitor.data.models.ClassifiedTrack;

@Database(entities = {ClassifiedTrack.class}, version = 1, exportSchema = false)
@TypeConverters({PointConverter.class, TrackQualityConverter.class, DateConverter.class})
public abstract class ClassifiedTrackDatabase extends RoomDatabase {
    public abstract ClassifiedTrackDAO classifiedTrackDAO();
}
