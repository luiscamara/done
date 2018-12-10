package br.com.luiscamara.roadqualitymonitor.data.converters;

import android.arch.persistence.room.TypeConverter;

import br.com.luiscamara.roadqualitymonitor.data.models.TrackQuality;

public class TrackQualityConverter {
    @TypeConverter
    public static int fromEnumToInt(TrackQuality tq) {
        return tq.ordinal();
    }

    @TypeConverter
    public static TrackQuality fromIntToEnum(int i) {
        return TrackQuality.values()[i];
    }
}
