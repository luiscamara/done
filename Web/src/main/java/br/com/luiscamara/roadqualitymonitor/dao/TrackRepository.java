package br.com.luiscamara.roadqualitymonitor.dao;

import br.com.luiscamara.roadqualitymonitor.models.Track;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackRepository extends CrudRepository<Track, Long> {
    @Query("from Track t where t.processed = false")
    public Iterable<Track> findUnprocessed();
}
