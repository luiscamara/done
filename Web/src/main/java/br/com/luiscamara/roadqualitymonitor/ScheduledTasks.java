/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.luiscamara.roadqualitymonitor;

import br.com.luiscamara.roadqualitymonitor.dao.ClassifiedTrackRepository;
import br.com.luiscamara.roadqualitymonitor.dao.ProcessedTrackRepository;
import br.com.luiscamara.roadqualitymonitor.dao.QualityRangeRepository;
import br.com.luiscamara.roadqualitymonitor.dao.TrackRepository;
import br.com.luiscamara.roadqualitymonitor.models.ClassifiedTrack;
import br.com.luiscamara.roadqualitymonitor.models.ProcessedTrack;
import br.com.luiscamara.roadqualitymonitor.models.QualityRange;
import br.com.luiscamara.roadqualitymonitor.models.Track;
import br.com.luiscamara.roadqualitymonitor.models.TrackQuality;
import br.com.luiscamara.roadqualitymonitor.models.VerticalAccelerationReading;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {
    private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 3857); 
    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");    
    
    @Autowired
    private TrackRepository trackRepository;
    
    @Autowired
    private ProcessedTrackRepository processedTrackRepository;
    
    @Autowired
    private ClassifiedTrackRepository classifiedTrackRepository;
    
    @Autowired
    private QualityRangeRepository qualityRangeRepository;
    
    @Scheduled(fixedDelay = 5 * 1000)
    public void processTracks() {
        Iterable<Track> tracks = trackRepository.findUnprocessed();
        for(Track t : tracks) {
            if(!isValid(t))
                continue;

            correctPosition(t);
            calculateStats(t);
        }
        
        consolidateTracksAndQualityStats();
    }
    
    private boolean isValid(Track t) {
        if(t.getAverageVelocity() < 20)
            return false;
        
        return true;
    }
    
    public void correctPosition(Track t) {
        String coordinates = String.valueOf(t.getStartPosition().getX()) + "," + String.valueOf(t.getStartPosition().getY()) + ";" +
                             String.valueOf(t.getEndPosition().getX()) + "," + String.valueOf(t.getEndPosition().getY());

        String url = String.format("http://router.project-osrm.org/match/v1/car/%s", coordinates);
        HttpClient httpClient = HttpClients.createDefault();
        JSONObject result = null;
        try{
            URIBuilder builder = new URIBuilder(url);
            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            request.addHeader("accept", "application/json");
            HttpResponse response = httpClient.execute(request);
            result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
            
            // TODO: parse data and correct track positions
            JSONArray tracePoints = (JSONArray)result.get("tracepoints");
            for(Object point : tracePoints) {
                JSONObject jsonObj = (JSONObject)point;
                int locationIndex = jsonObj.getInt("waypoint_index");
                JSONArray correctLocation = jsonObj.getJSONArray("location");
                double latitude = correctLocation.getDouble(0);
                double longitude = correctLocation.getDouble(1);
                Point geom = geometryFactory.createPoint(new Coordinate(latitude, longitude));
                if(locationIndex % 2 == 0)
                    t.setStartPosition(geom);
                else
                    t.setEndPosition(geom);
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
    
    public void calculateStats(Track t) 
    {
        QualityRange qualityRange = qualityRangeRepository.findByVelocity(t.getAverageVelocity() - (t.getAverageVelocity() % 10));
        boolean modifiedQualityRange = false;
        if(qualityRange == null) {
            qualityRange = new QualityRange();
            qualityRange.setVelocityRange(t.getAverageVelocity() - (t.getAverageVelocity() % 10));
            qualityRange.setMinDeviation(999f);
            qualityRange.setMaxDeviation(-1f);
            qualityRange.setMinCoefficientVariation(99);
            qualityRange.setMaxCoefficientVariation(0);
            modifiedQualityRange = true;
        }

        ProcessedTrack newProcessedTrack = new ProcessedTrack();
        newProcessedTrack.setStartPosition(t.getStartPosition());
        newProcessedTrack.setEndPosition(t.getEndPosition());
        newProcessedTrack.setAverageVelocity(t.getAverageVelocity());

        float sum = 0;
        for(VerticalAccelerationReading var : t.getVerticalAccelerationReadings()) {
            sum += Math.abs(var.getY());
        }

        float average = sum / t.getVerticalAccelerationReadings().size();
        float variance = 0;
        for(VerticalAccelerationReading var : t.getVerticalAccelerationReadings()) {
            variance += Math.abs(Math.pow(var.getY() - average, 2));
        }

        // Modified to N-1 because it is a sample of the population (only a few readings for the track)
        float standardDeviation = (float)Math.sqrt(variance / (t.getVerticalAccelerationReadings().size() - 1));
        // Coefficient of variation should eliminate differences between car suspension
        float coefficientVariation = standardDeviation / average;
        
        List<Long> peaksToIgnore = new ArrayList<>();
        int numPeaks = 0;
        for(VerticalAccelerationReading var : t.getVerticalAccelerationReadings()) {
            // 4 sigma = 99.99%
            // If value is higher than it is clearly a peak
            if(var.getY() > average + 4 * standardDeviation) { 
                peaksToIgnore.add(var.getId());
                numPeaks++;
            }
        }            
        newProcessedTrack.setNumPeaks(numPeaks);
        
        // Recalculate average and standard deviation if any peak is detected
        // Peaks should be excluded from final result
        if(numPeaks > 0) {
            sum = 0;
            for(VerticalAccelerationReading var : t.getVerticalAccelerationReadings()) {
                if(peaksToIgnore.contains(var.getId()))
                    continue;
                
                sum += Math.abs(var.getY());
            }

            average = sum / (t.getVerticalAccelerationReadings().size() - numPeaks);
            variance = 0;
            for(VerticalAccelerationReading var : t.getVerticalAccelerationReadings()) {
                if(peaksToIgnore.contains(var.getId()))
                    continue;
                
                variance += Math.abs(Math.pow(var.getY() - average, 2));
            }

            // Modified to N-1 because it is a sample of the population (only a few readings for each track)
            standardDeviation = (float)Math.sqrt(variance / (t.getVerticalAccelerationReadings().size() - numPeaks - 1));
            // Coefficient of variation should eliminate differences between car suspension
            coefficientVariation = standardDeviation / average;
        }
        
        // Save final standard deviation
        newProcessedTrack.setStandardDeviation(standardDeviation);
        if(standardDeviation > qualityRange.getMaxDeviation()) {
            qualityRange.setMaxDeviation(standardDeviation);
            modifiedQualityRange = true;
        } else if(standardDeviation < qualityRange.getMinDeviation()) {
            qualityRange.setMinDeviation(standardDeviation);
            modifiedQualityRange = true;
        }

        // Save final coefficient of variation
        newProcessedTrack.setCoefficientVariation(coefficientVariation);
        if(coefficientVariation > qualityRange.getMaxCoefficientVariation()) {
            qualityRange.setMaxCoefficientVariation(coefficientVariation);
            modifiedQualityRange = true;
        } else if(coefficientVariation < qualityRange.getMinCoefficientVariation()) {
            qualityRange.setMinCoefficientVariation(coefficientVariation);
            modifiedQualityRange = true;
        }
        
        if(modifiedQualityRange)
            qualityRangeRepository.save(qualityRange);
        
        // TODO: Calculate estimated IRI
        float eIRI = 0;
        eIRI = ((float)standardDeviation - 0.013f) / 0.5926f;
        newProcessedTrack.seteIRI(eIRI);

        processedTrackRepository.save(newProcessedTrack);
        t.setProcessed(true);
        trackRepository.save(t);
    }
    
    public void consolidateTracksAndQualityStats() {
        // TODO: Consolidate tracks at similar positions
        // TODO: Summarize consolidated track quality
        
        Iterable<ProcessedTrack> processedTracks = processedTrackRepository.findAll();
        for(ProcessedTrack pt : processedTracks) {
            QualityRange qualityRange = qualityRangeRepository.findByVelocity(pt.getAverageVelocity() - (pt.getAverageVelocity() % 10));
            
            ClassifiedTrack ct = new ClassifiedTrack();
            ct.setStartPosition(pt.getStartPosition());
            ct.setEndPosition(pt.getEndPosition());
            ct.setAverageVelocity(pt.getAverageVelocity());
            ct.seteIRI(pt.geteIRI());
            
            if(qualityRange.getMinCoefficientVariation() >= 99f || qualityRange.getMaxCoefficientVariation() <= 0f) {
                ct.setQuality(TrackQuality.UNKNOWN);
            } else {
                // Do math to calculate apropriate level
                float deviationStep = (qualityRange.getMaxCoefficientVariation() - qualityRange.getMinCoefficientVariation()) / 5;

                if((pt.getStandardDeviation() - qualityRange.getMinCoefficientVariation()) < deviationStep)
                    ct.setQuality(TrackQuality.EXCELLENT);
                else if((pt.getStandardDeviation() - qualityRange.getMinCoefficientVariation()) > deviationStep && 
                        (pt.getStandardDeviation() - qualityRange.getMinCoefficientVariation()) < 2*deviationStep)
                         ct.setQuality(TrackQuality.GOOD);
                else if((pt.getStandardDeviation() - qualityRange.getMinCoefficientVariation()) > 2*deviationStep && 
                        (pt.getStandardDeviation() - qualityRange.getMinCoefficientVariation()) < 3*deviationStep)
                         ct.setQuality(TrackQuality.AVERAGE);
                else if((pt.getStandardDeviation() - qualityRange.getMinCoefficientVariation()) > 3*deviationStep && 
                        (pt.getStandardDeviation() - qualityRange.getMinCoefficientVariation()) < 4*deviationStep)
                         ct.setQuality(TrackQuality.BAD);
                else
                    ct.setQuality(TrackQuality.TERRIBLE);
            }
            
            classifiedTrackRepository.save(ct);
        }        
        
        processedTrackRepository.deleteAll();
    }
}
