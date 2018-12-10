package br.com.luiscamara.dataextractor.models;

import java.io.Serializable;

public class MapExtent implements Serializable {
    private double latitudeNorth;
    private double latitudeSouth;
    private double longitudeEast;
    private double longitudeWest;

    public double getLatitudeNorth() {
        return latitudeNorth;
    }

    public void setLatitudeNorth(double latitudeNorth) {
        this.latitudeNorth = latitudeNorth;
    }

    public double getLatitudeSouth() {
        return latitudeSouth;
    }

    public void setLatitudeSouth(double latitudeSouth) {
        this.latitudeSouth = latitudeSouth;
    }

    public double getLongitudeEast() {
        return longitudeEast;
    }

    public void setLongitudeEast(double longitudeEast) {
        this.longitudeEast = longitudeEast;
    }

    public double getLongitudeWest() {
        return longitudeWest;
    }

    public void setLongitudeWest(double longitudeWest) {
        this.longitudeWest = longitudeWest;
    }
}
