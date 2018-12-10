package br.com.luiscamara.roadqualitymonitor.dao;

import br.com.luiscamara.roadqualitymonitor.models.ProcessedTrack;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedTrackRepository extends CrudRepository<ProcessedTrack, Long> {
    
}
