package br.com.luiscamara.dataextractor.dao;

import br.com.luiscamara.dataextractor.models.ClassifiedTrack;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassifiedTrackRepository extends CrudRepository<ClassifiedTrack, Long> {
    @Query(value = "SELECT * FROM classified_track ct WHERE (ct.start_position && ST_MakeEnvelope(:longitudeEast, :latitudeSouth, :longitudeWest, :latitudeNorth) OR " +
                   "ct.end_position && ST_MakeEnvelope(:longitudeEast, :latitudeSouth, :longitudeWest, :latitudeNorth, 4326))",
           nativeQuery = true)
    public Iterable<ClassifiedTrack> findInsideExtent(double latitudeNorth, double latitudeSouth, double longitudeEast, double longitudeWest);
}
