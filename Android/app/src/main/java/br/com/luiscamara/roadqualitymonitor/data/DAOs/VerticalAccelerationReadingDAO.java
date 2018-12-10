package br.com.luiscamara.roadqualitymonitor.data.DAOs;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import br.com.luiscamara.roadqualitymonitor.data.models.VerticalAccelerationReading;

@Dao
public interface VerticalAccelerationReadingDAO {
    @Insert
    long insert(VerticalAccelerationReading v);

    @Query("SELECT * FROM verticalaccelerations")
    List<VerticalAccelerationReading> list();

    @Query("SELECT * FROM verticalaccelerations WHERE trackId = :trackId")
    List<VerticalAccelerationReading> list(Long trackId);

    @Delete
    void remove(VerticalAccelerationReading t);

    @Query("DELETE FROM verticalaccelerations")
    void truncate();
}
