package br.com.luiscamara.roadqualitymonitor.dao;

import br.com.luiscamara.roadqualitymonitor.models.QualityRange;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityRangeRepository extends CrudRepository<QualityRange, Long> {
    @Query("from QualityRange qr where qr.velocityRange=:velocityRange")
    public QualityRange findByVelocity(@Param("velocityRange") float velocityRange);
}