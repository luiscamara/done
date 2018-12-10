package br.com.luiscamara.roadqualitymonitor.network.services;

import br.com.luiscamara.roadqualitymonitor.data.models.ClassifiedTrack;
import br.com.luiscamara.roadqualitymonitor.data.models.MapExtent;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RewardService {
    @POST("/")
    Call<Iterable<ClassifiedTrack>> query(@Body MapExtent extent);
}
