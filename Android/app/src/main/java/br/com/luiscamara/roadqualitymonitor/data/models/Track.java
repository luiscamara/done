package br.com.luiscamara.roadqualitymonitor.data.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.locationtech.jts.geom.Point;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import br.com.luiscamara.roadqualitymonitor.network.serializers.JsonToPointDeserializer;
import br.com.luiscamara.roadqualitymonitor.network.serializers.PointToJsonSerializer;

@Entity(tableName = "tracks")
public class Track implements Serializable {

    @JsonIgnore
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private Long id;

    private String userID;

    private Long numExecution;
    private Long numSequence;

    private Date startTime;
    private Date endTime;

    @JsonSerialize(using = PointToJsonSerializer.class)
    @JsonDeserialize(using = JsonToPointDeserializer.class)
    private Point startPosition;

    @JsonSerialize(using = PointToJsonSerializer.class)
    @JsonDeserialize(using = JsonToPointDeserializer.class)
    private Point endPosition;

    private float averageVelocity;

    @Ignore
    private List<VerticalAccelerationReading> verticalAccelerationReadings;

    @JsonIgnore
    @Ignore
    private boolean processed;

    public Track() {
        verticalAccelerationReadings = new ArrayList<>();
    }

    @NonNull
    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public Long getNumExecution() {
        return numExecution;
    }

    public void setNumExecution(Long numExecution) {
        this.numExecution = numExecution;
    }

    public Long getNumSequence() {
        return numSequence;
    }

    public void setNumSequence(Long numSequence) {
        this.numSequence = numSequence;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Point getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Point startPosition) {
        this.startPosition = startPosition;
    }

    public Point getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Point endPosition) {
        this.endPosition = endPosition;
    }

    public float getAverageVelocity() {
        return averageVelocity;
    }

    public void setAverageVelocity(float averageVelocity) {
        this.averageVelocity = averageVelocity;
    }

    public List<VerticalAccelerationReading> getVerticalAccelerationReadings() {
        return verticalAccelerationReadings;
    }

    public void setVerticalAccelerationReadings(List<VerticalAccelerationReading> verticalAccelerationReadings) {
        this.verticalAccelerationReadings = verticalAccelerationReadings;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
}
