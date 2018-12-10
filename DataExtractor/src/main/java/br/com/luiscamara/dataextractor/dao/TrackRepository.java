package br.com.luiscamara.dataextractor.dao;

import br.com.luiscamara.dataextractor.models.Track;
import java.util.Date;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackRepository extends CrudRepository<Track, Long> {
    @Query("from Track t where t.processed = false")
    public Iterable<Track> findUnprocessed();
    
    @Query("from Track t where t.startTime > :initialDate order by t.userID, t.numExecution, t.numSequence")
    public Iterable<Track> findAfterDate(Date initialDate);
}
