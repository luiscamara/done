package br.com.luiscamara.roadqualitymonitor.dao;

import br.com.luiscamara.roadqualitymonitor.models.Device;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends CrudRepository<Device, Long> {
    
}
