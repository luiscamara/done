package br.com.luiscamara.roadqualitymonitor.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class VerticalAccelerationReading implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    
    private float y;
    private long timeSinceLastReading;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }
    public long getTimeSinceLastReading() {
        return timeSinceLastReading;
    }

    public void setTimeSinceLastReading(long timeSinceLastReading) {
        this.timeSinceLastReading = timeSinceLastReading;
    }
}
