package br.com.luiscamara.roadqualitymonitor.models;

import br.com.luiscamara.roadqualitymonitor.serialize.JsonToPointDeserializer;
import br.com.luiscamara.roadqualitymonitor.serialize.PointToJsonSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Point;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ProcessedTrack implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    
    @JsonSerialize(using = PointToJsonSerializer.class)
    @JsonDeserialize(using = JsonToPointDeserializer.class)
    private Point startPosition;

    @JsonSerialize(using = PointToJsonSerializer.class)
    @JsonDeserialize(using = JsonToPointDeserializer.class)
    private Point endPosition;

    private float averageVelocity;
    
    private float standardDeviation;
    private float coefficientVariation;
    private float eIRI;
    private int numPeaks;
    
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public float getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(float standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public float getCoefficientVariation() {
        return coefficientVariation;
    }

    public void setCoefficientVariation(float coefficientVariation) {
        this.coefficientVariation = coefficientVariation;
    }

    public float geteIRI() {
        return eIRI;
    }

    public void seteIRI(float eIRI) {
        this.eIRI = eIRI;
    }

    public int getNumPeaks() {
        return numPeaks;
    }

    public void setNumPeaks(int numPeaks) {
        this.numPeaks = numPeaks;
    }
}
