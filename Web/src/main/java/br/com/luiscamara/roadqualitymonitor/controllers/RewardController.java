package br.com.luiscamara.roadqualitymonitor.controllers;

import br.com.luiscamara.roadqualitymonitor.dao.ClassifiedTrackRepository;
import br.com.luiscamara.roadqualitymonitor.models.ClassifiedTrack;
import br.com.luiscamara.roadqualitymonitor.models.MapExtent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/")
public class RewardController {    
    @Autowired
    private ClassifiedTrackRepository classifiedTrackRepository;
    
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("index");
        return modelAndView;
    }
    
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> query(@RequestBody MapExtent extent) {
        Iterable<ClassifiedTrack> targetTracks = classifiedTrackRepository.findInsideExtent(extent.getLatitudeNorth(), extent.getLatitudeSouth(), extent.getLongitudeEast(), extent.getLongitudeWest());
        
        return new ResponseEntity<Iterable<ClassifiedTrack>>(targetTracks, HttpStatus.OK);
    }
}
