package br.com.luiscamara.dataextractor.dao;

import br.com.luiscamara.dataextractor.models.ProcessedTrack;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedTrackRepository extends CrudRepository<ProcessedTrack, Long> {
    
}
