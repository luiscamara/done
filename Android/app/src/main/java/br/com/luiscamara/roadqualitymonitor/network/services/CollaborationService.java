package br.com.luiscamara.roadqualitymonitor.network.services;

import br.com.luiscamara.roadqualitymonitor.data.models.Device;
import br.com.luiscamara.roadqualitymonitor.data.models.Track;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CollaborationService {
    @POST("collaborate")
    Call<Track> collaborate(@Body Track track);

    @POST("collaborate/register")
    Call<Device> registerDevice(@Body Device device);
}
