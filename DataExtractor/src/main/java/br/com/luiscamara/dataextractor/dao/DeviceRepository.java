package br.com.luiscamara.dataextractor.dao;

import br.com.luiscamara.dataextractor.models.Device;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends CrudRepository<Device, Long> {
    
}
