/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.luiscamara.dataextractor;

import br.com.luiscamara.dataextractor.dao.QualityRangeRepository;
import br.com.luiscamara.dataextractor.dao.TrackRepository;
import br.com.luiscamara.dataextractor.models.QualityRange;
import br.com.luiscamara.dataextractor.models.Track;
import br.com.luiscamara.dataextractor.models.VerticalAccelerationReading;
import com.opencsv.CSVWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import java.io.File;
import java.io.FileWriter;
import static java.lang.System.exit;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.transaction.Transactional;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application implements CommandLineRunner {
    private final static GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 3857); 
    
    @Autowired
    private TrackRepository repository;
    
    @Autowired
    private QualityRangeRepository qualityRangeRepository;
    
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

    @Transactional
    @Override
    public void run(String... args) {
        Date initialDate = new Date();
        initialDate.setYear(118);
        initialDate.setMonth(11);
        initialDate.setDate(8);
        initialDate.setHours(0);
        initialDate.setMinutes(0);
        initialDate.setSeconds(0);
        log.debug(initialDate.toString());
        Iterable<Track> tracks = repository.findAfterDate(initialDate);
        
        log.debug("Avaliando tracks...");
        File file = new File("dados.csv");
        try {
            FileWriter outputFile = new FileWriter(file);
            CSVWriter writer = new CSVWriter(outputFile);
            String[] header = { "Dispositivo", "Origem: Latitude", "Origem: Longitude", "Destino: Latitude", "Destino: Longitude", "Velocidade", "eIRI", "eIRI_SD", "Qualidade", "Coeficiente de Variância" };
            writer.writeNext(header);

            for(Track t : tracks) {
                if(!isValid(t))
                    continue;

                while(!correctPosition(t));
                calculateStats(t, writer);
            }
            
            writer.close();
            
            log.debug("Tracks avaliadas!");
        } catch(Exception e) {
            e.printStackTrace();
        }
        exit(0);
    }
    
    private boolean isValid(Track t) {
        if(t.getAverageVelocity() < 20)
            return false;
        
        return true;
    }
    
    public boolean correctPosition(Track t) {
        String coordinates = String.valueOf(t.getStartPosition().getX()) + "," + String.valueOf(t.getStartPosition().getY()) + ";" +
                             String.valueOf(t.getEndPosition().getX()) + "," + String.valueOf(t.getEndPosition().getY());

        String url = String.format("http://router.project-osrm.org/match/v1/car/%s", coordinates);
        HttpClient httpClient = HttpClients.createDefault();
        JSONObject result = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            request.addHeader("accept", "application/json");
            HttpResponse response = httpClient.execute(request);
            result = new JSONObject(IOUtils.toString(response.getEntity().getContent()));
            
            // TODO: parse data and correct track positions
            if(result.has("message")) {
                if(result.getString("message").toLowerCase().equals("too many requests")) {
                    return false;
                }
            }
            
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
            
            return true;
        } catch (Exception e){
            System.out.println(e.getMessage());
            return true;
        }
    }
    
    public void calculateStats(Track t, CSVWriter writer) {
        QualityRange qualityRange = qualityRangeRepository.findByVelocity(t.getAverageVelocity() - (t.getAverageVelocity() % 10));

        String[] dados = new String[10];
        dados[0] = t.getUserID();
        dados[1] = Double.toString(t.getStartPosition().getX());
        dados[2] = Double.toString(t.getStartPosition().getY());
        dados[3] = Double.toString(t.getEndPosition().getX());
        dados[4] = Double.toString(t.getEndPosition().getY());
        dados[5] = Float.toString(t.getAverageVelocity());
        
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
        
        
        // Calculate estimated IRI
        double eIRI;
        double eIRI_SD;
        
        eIRI_SD = ((float)standardDeviation - 0.013f) / 0.5926f;
        
        // IRI = sum(vertical_displacement) / horizontal_displacement
        // TODO
        // Coisas para testar:
        // 1) Média móvel para filtrar a aceleração
        // 2) Dupla Integral da aceleração
        double totalVD = 0;
        for(VerticalAccelerationReading var : t.getVerticalAccelerationReadings()) {
            double time = var.getTimeSinceLastReading() / 1000000000f;
            //double speed = var.getY() * time;
            double position = (var.getY() * time * time) / 2f;
            totalVD += Math.abs(position);
        }
        
        double distance = calculateDistance(t.getStartPosition().getX(), t.getStartPosition().getY(), t.getEndPosition().getX(), t.getEndPosition().getY());
        eIRI = totalVD / (distance * 0.001);
        dados[6] = Double.toString(eIRI);
        dados[7] = Double.toString(eIRI_SD);
        

        // Save quality
        float deviationStep = (qualityRange.getMaxCoefficientVariation() - qualityRange.getMinCoefficientVariation()) / 5;

        if(coefficientVariation <= qualityRange.getMinCoefficientVariation() + deviationStep)
            dados[8] = "Excelente";
        else if(coefficientVariation > qualityRange.getMinCoefficientVariation() + deviationStep &&
                coefficientVariation <= qualityRange.getMinCoefficientVariation() + 2*deviationStep)
            dados[8] = "Boa";
        else if(coefficientVariation > qualityRange.getMinCoefficientVariation() + 2*deviationStep &&
                coefficientVariation <= qualityRange.getMinCoefficientVariation() + 3*deviationStep)
            dados[8] = "Média";
        else if(coefficientVariation > qualityRange.getMinCoefficientVariation() + 3*deviationStep &&
                coefficientVariation <= qualityRange.getMinCoefficientVariation() + 4*deviationStep)
            dados[8] = "Ruim";
        else
            dados[8] = "Péssima";
        
        // Save final coefficient of variation
        dados[9] = Float.toString(coefficientVariation);
        
        writer.writeNext(dados);
        try {
            writer.flush();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        GeodesicData g = Geodesic.WGS84.Inverse(lat1, lon1, lat2, lon2);
        return g.s12;
    }
}
