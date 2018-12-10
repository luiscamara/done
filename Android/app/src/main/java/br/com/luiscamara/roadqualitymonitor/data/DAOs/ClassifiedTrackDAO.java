package br.com.luiscamara.roadqualitymonitor.data.DAOs;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import br.com.luiscamara.roadqualitymonitor.data.models.ClassifiedTrack;

@Dao
public interface ClassifiedTrackDAO {
    @Insert
    void insert(ClassifiedTrack t);

    @Query("SELECT * FROM classified_tracks")
    List<ClassifiedTrack> list();

    @Delete
    void remove(ClassifiedTrack t);
}
