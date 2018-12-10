package br.com.luiscamara.roadqualitymonitor.util;

/**
 * Created by luisc on 20/12/2017.
 */

public class Constants {
    public static final String BROADCAST_DETECTED_ACTIVITY = "activity_intent";
    public static final String BROADCAST_CURRENT_LOCATION = "send_current_location";
    public static final String BROADCAST_VERTICAL_ACCELERATION_DATA = "vertical_acceleration_data";
    public static final String BROADCAST_INIT_READINGS = "init_readings";
    public static final String BROADCAST_SUBMITTED_TRACK = "submitted_track";
    public static final String BROADCAST_TRACK_SENT = "track_sent";
    public static final String BROADCAST_AUTOMATIC_READINGS_ENABLE = "automatic_readings_enable";
    public static final String BROADCAST_READINGS_STATUS_REQUEST = "readings_status_request";
    public static final String BROADCAST_READINGS_STATUS_REPLY = "readings_status_reply";

    public static final int GNSS_PERMISSION = 1717;
    public static final int STORAGE_PERMISSION = 1718;
}
