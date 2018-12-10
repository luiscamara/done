package br.com.luiscamara.roadqualitymonitor.data.DAOs;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import br.com.luiscamara.roadqualitymonitor.data.models.Track;

@Dao
public interface TrackDAO {
    @Insert
    long insert(Track t);

    @Query("SELECT * FROM tracks")
    List<Track> list();

    @Delete
    void remove(Track t);

    @Query("DELETE FROM tracks")
    void truncate();
}
