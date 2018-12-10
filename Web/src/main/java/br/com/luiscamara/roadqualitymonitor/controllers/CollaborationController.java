package br.com.luiscamara.roadqualitymonitor.controllers;

import br.com.luiscamara.roadqualitymonitor.dao.DeviceRepository;
import br.com.luiscamara.roadqualitymonitor.dao.TrackRepository;
import br.com.luiscamara.roadqualitymonitor.models.Device;
import br.com.luiscamara.roadqualitymonitor.models.Track;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collaborate")
public class CollaborationController {  
    @Autowired
    private DeviceRepository deviceRepository;
    
    @Autowired
    private TrackRepository trackRepository;
    
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> Collaborate(@Valid @RequestBody Track track) {
        boolean foundUser = false;
        for(Device d : deviceRepository.findAll()) {
            if(d.getUserID().equals(track.getUserID())) {
                foundUser = true;
                break;
            }
        }
        
        if(!foundUser) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not registered");
        }
        
        if(track.getAverageVelocity() < 20)
            return new ResponseEntity<Track>(track, HttpStatus.OK);
        
        trackRepository.save(track);
        return new ResponseEntity<Track>(track, HttpStatus.CREATED);
    }
    
    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ResponseEntity<?> RegisterDevice(@Valid @RequestBody Device device) {
        Iterable<Device> devices = deviceRepository.findAll();
        for(Device d : devices) {
            if(d.getUserID().equals(device.getUserID())) {
                return new ResponseEntity<Device>(device, HttpStatus.ALREADY_REPORTED);
            }
        }
        
        deviceRepository.save(device);
        return new ResponseEntity<Device>(device, HttpStatus.CREATED);
    }
}
