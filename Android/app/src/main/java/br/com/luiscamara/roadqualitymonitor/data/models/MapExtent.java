package br.com.luiscamara.roadqualitymonitor.data.models;

import java.io.Serializable;

public class MapExtent implements Serializable {
    public MapExtent() {

    }

    public MapExtent(double latNor, double latSou, double lonEas, double lonWes) {
        this();
        this.latitudeNorth = latNor;
        this.latitudeSouth = latSou;
        this.longitudeEast = lonEas;
        this.longitudeWest = lonWes;
    }

    public double latitudeNorth;
    public double latitudeSouth;
    public double longitudeEast;
    public double longitudeWest;
}
