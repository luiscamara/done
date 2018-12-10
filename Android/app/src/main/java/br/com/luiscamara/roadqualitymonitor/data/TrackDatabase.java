package br.com.luiscamara.roadqualitymonitor.data;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;
import android.content.Context;

import br.com.luiscamara.roadqualitymonitor.data.DAOs.TrackDAO;
import br.com.luiscamara.roadqualitymonitor.data.DAOs.VerticalAccelerationReadingDAO;
import br.com.luiscamara.roadqualitymonitor.data.converters.DateConverter;
import br.com.luiscamara.roadqualitymonitor.data.converters.PointConverter;
import br.com.luiscamara.roadqualitymonitor.data.models.Track;
import br.com.luiscamara.roadqualitymonitor.data.models.VerticalAccelerationReading;
import br.com.luiscamara.roadqualitymonitor.data.services.TrackService;

@Database(entities = {Track.class, VerticalAccelerationReading.class}, version = 2, exportSchema = false)
@TypeConverters({PointConverter.class, DateConverter.class})
public abstract class TrackDatabase extends RoomDatabase {
    private static final String DB_NAME = "rqmDatabase.db";
    private static volatile TrackDatabase instance;

    public static synchronized TrackDatabase getInstance(Context context) {
        if(instance == null) {
            instance = create(context);
        }

        return instance;
    }

    private static TrackDatabase create(final Context context) {
        return Room.databaseBuilder(context, TrackDatabase.class, DB_NAME).allowMainThreadQueries().fallbackToDestructiveMigration().build();
    }

    public abstract TrackDAO trackDAO();
    public abstract VerticalAccelerationReadingDAO verticalAccelerationReadingDAO();
}
