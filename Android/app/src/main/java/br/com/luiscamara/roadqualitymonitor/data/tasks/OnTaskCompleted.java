package br.com.luiscamara.roadqualitymonitor.data.tasks;

import java.util.List;

import br.com.luiscamara.roadqualitymonitor.data.models.Track;

public interface OnTaskCompleted {
    void onTaskCompleted(List<Track> tracks);
}
