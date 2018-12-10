package br.com.luiscamara.roadqualitymonitor.data.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.locationtech.jts.geom.Point;

import java.io.Serializable;

import br.com.luiscamara.roadqualitymonitor.network.serializers.JsonToPointDeserializer;
import br.com.luiscamara.roadqualitymonitor.network.serializers.PointToJsonSerializer;

@Entity(tableName = "classified_tracks")
public class ClassifiedTrack implements Serializable {

    @JsonIgnore
    @PrimaryKey(autoGenerate = true)
    @NonNull
    public Long id;

    @JsonSerialize(using = PointToJsonSerializer.class)
    @JsonDeserialize(using = JsonToPointDeserializer.class)
    public Point startPosition;
    @JsonSerialize(using = PointToJsonSerializer.class)
    @JsonDeserialize(using = JsonToPointDeserializer.class)
    public Point endPosition;

    public float averageVelocity;
    public TrackQuality quality;
    public float eIRI;
}
