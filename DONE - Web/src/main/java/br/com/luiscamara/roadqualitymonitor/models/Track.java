package br.com.luiscamara.roadqualitymonitor.models;

import br.com.luiscamara.roadqualitymonitor.serialize.JsonToPointDeserializer;
import br.com.luiscamara.roadqualitymonitor.serialize.PointToJsonSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Point;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;

import javax.persistence.Entity;
import static javax.persistence.FetchType.EAGER;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

@Entity
public class Track implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    
    @NotNull
    private String userID;
    @NotNull
    private Long numExecution;
    @NotNull
    private Long numSequence;
    @NotNull
    private Date startTime;
    @NotNull
    private Date endTime;
    //@Column(name = "startPosition", columnDefinition = "POINT")
    @JsonSerialize(using = PointToJsonSerializer.class)
    @JsonDeserialize(using = JsonToPointDeserializer.class)
    @NotNull
    private Point startPosition;
    //@Column(name = "endPosition", columnDefinition = "POINT")
    @JsonSerialize(using = PointToJsonSerializer.class)
    @JsonDeserialize(using = JsonToPointDeserializer.class)
    @NotNull
    private Point endPosition;
    @NotNull
    private float averageVelocity;
    
    @OneToMany(fetch = EAGER, cascade = CascadeType.ALL, orphanRemoval = true)    
    @NotNull
    private List<VerticalAccelerationReading> verticalAccelerationReadings;
    
    private boolean processed;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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
