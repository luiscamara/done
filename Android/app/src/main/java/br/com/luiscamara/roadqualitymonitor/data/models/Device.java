package br.com.luiscamara.roadqualitymonitor.data.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class Device implements Serializable {

    @JsonIgnore
    public Long id;

    public String userID;
    public String userDevice;
}
