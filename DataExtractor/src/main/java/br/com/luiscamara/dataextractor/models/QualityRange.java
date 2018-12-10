package br.com.luiscamara.dataextractor.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
public class QualityRange implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id;
    
    @NotNull
    private float velocityRange;
    @NotNull
    private float minDeviation;
    @NotNull
    private float maxDeviation;
    
    private float minCoefficientVariation;
    private float maxCoefficientVariation;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public float getVelocityRange() {
        return velocityRange;
    }

    public void setVelocityRange(float velocityRange) {
        this.velocityRange = velocityRange;
    }

    public float getMinDeviation() {
        return minDeviation;
    }

    public void setMinDeviation(float minDeviation) {
        this.minDeviation = minDeviation;
    }

    public float getMaxDeviation() {
        return maxDeviation;
    }

    public void setMaxDeviation(float maxDeviation) {
        this.maxDeviation = maxDeviation;
    }

    public float getMinCoefficientVariation() {
        return minCoefficientVariation;
    }

    public void setMinCoefficientVariation(float minCoefficientVariation) {
        this.minCoefficientVariation = minCoefficientVariation;
    }

    public float getMaxCoefficientVariation() {
        return maxCoefficientVariation;
    }

    public void setMaxCoefficientVariation(float maxCoefficientVariation) {
        this.maxCoefficientVariation = maxCoefficientVariation;
    }
}
