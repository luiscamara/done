package br.com.luiscamara.roadqualitymonitor.data.converters;

import android.arch.persistence.room.TypeConverter;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class PointConverter {
    private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 3857);

    @TypeConverter
    public static String fromPointToString(Point p) {
        return String.valueOf(p.getX()) + "|" + String.valueOf(p.getY());
    }

    @TypeConverter
    public static Point fromStringToPoint(String s) {
        String[] args = s.split("[|]");
        double lat = Double.valueOf(args[0]);
        double lon = Double.valueOf(args[1]);
        return geometryFactory.createPoint(new Coordinate(lat, lon));
    }
}
