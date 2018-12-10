package br.com.luiscamara.roadqualitymonitor.data.models;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import static android.arch.persistence.room.ForeignKey.CASCADE;

@Entity(indices = {@Index("trackId")},
        tableName = "verticalaccelerations", foreignKeys = {
        @ForeignKey(entity = Track.class,
                    parentColumns = "id",
                    childColumns =  "trackId",
                    onDelete = CASCADE)
})
public class VerticalAccelerationReading implements Serializable {
    @JsonIgnore
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private Long id;

    @JsonIgnore
    private Long trackId;

    private float y;
    private Long timeSinceLastReading;

    @NonNull
    public Long getId() {
        return id;
    }

    public void setId(@NonNull Long id) {
        this.id = id;
    }

    public Long getTrackId() {
        return trackId;
    }

    public void setTrackId(Long trackId) {
        this.trackId = trackId;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public Long getTimeSinceLastReading() {
        return timeSinceLastReading;
    }

    public void setTimeSinceLastReading(Long timeSinceLastReading) {
        this.timeSinceLastReading = timeSinceLastReading;
    }

    public JSONObject toJSON() throws JSONException {
        try {
            JSONObject jo = new JSONObject();
            jo.put("y", y);
            jo.put("timeSinceLastReading", timeSinceLastReading);
            return jo;
        } catch(JSONException e) {
            throw e;
        }
    }
}
