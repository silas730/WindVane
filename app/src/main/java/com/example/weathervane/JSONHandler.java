package com.example.weathervane;

import android.util.JsonReader;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JSONHandler {

    public JSONHandler(){

    }

    /**
     * Parses an input stream for weather station data
     * @param in InputStream containing JSON data
     * @return List of weather station locations
     * @throws IOException
     */
    public List<LocationResponse> parseLocations(InputStream in) throws IOException {

        if (in == null){
            return null;
        }
        JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        Log.i("Test: ", "JSONReader Initialized");


        try{
            return readLocationArray(reader);
        } finally {
            reader.close();
        }
    }

    /**
     * Reads each array from he json
     * @param reader
     * @return The completed List of LocationResponses
     * @throws IOException
     */
    private List<LocationResponse> readLocationArray(JsonReader reader) throws IOException{
        List<LocationResponse> locations = new ArrayList<LocationResponse>();

        try {
            reader.beginObject();
            reader.nextName();
            reader.beginArray();
        } catch (IllegalStateException e) {
            return null;
        }

        while (reader.hasNext()) {
            Log.i("Test: ", "Read array");
            locations.add(readLocationResponse(reader));
        }
        reader.endArray();
        return locations;
    }

    /**
     * Reads each element in the json array and adds the relevent items to a LocationResponse object
     * @param reader
     * @return
     * @throws IOException
     */
    private LocationResponse readLocationResponse(JsonReader reader) throws IOException {
        String stationName = null;
        double distance = -1;
        String status = null;
        String StId = null;
        boolean restricted = true;
        Log.i("Test: ","In readLocationResponse");

        reader.beginObject();
        while (reader.hasNext()){
            String name = reader.nextName();
            Log.i("Test: ", "Reading: " + name);
            if (name.equals("NAME")){
                stationName = reader.nextString();
            } else if (name.equals("DISTANCE")) {
                distance = reader.nextDouble();
            } else if (name.equals("STID")) {
                StId = reader.nextString();
            } else if (name.equals("RESTRICTED")){
                restricted = reader.nextBoolean();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new LocationResponse(stationName, distance, StId, restricted);
    }

    public List<WindResponse> parseWind(InputStream in) throws IOException{
        if (in == null){
            return null;
        }
        JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        Log.i("Test: ", "JSONReader Initialized");

        try{
            return readWindArray(reader);
        } finally {
            reader.close();
        }
    }

    private List<WindResponse> readWindArray(JsonReader reader) throws IOException{
        List<WindResponse> windData = new ArrayList<WindResponse>();
        try {
            reader.beginObject();
            while (true) {
                String name = reader.nextName();
                if (name.equals("STATION")) {
                    reader.beginArray();
                    reader.beginObject();
                    break;
                } else {
                    reader.skipValue();
                }
            }
            while (true) {
                String name = reader.nextName();
                if (name.equals("OBSERVATIONS")) {
                    reader.beginObject();
                    break;
                } else {
                    reader.skipValue();
                }
            }
        } catch (IllegalStateException e) {
            return null;
        }


        while (reader.hasNext()) {
            Log.i("Test: ", "Read array");
            windData.add(readWindResponse(reader));
        }
        //reader.endObject();
        return windData;
    }

    private WindResponse readWindResponse(JsonReader reader) throws IOException {
        double windSpeed = 0;
        double windDir = 0;
        double windGust = 0;
        Log.i("Test: ","In readLocationResponse");

        //reader.beginObject();
        while (reader.hasNext()){
            String name = reader.nextName();
            Log.i("Test: ", "Reading: " + name);
            if (name.equals("wind_gust_value_1")) {
                reader.beginObject();
                reader.nextName();
                reader.skipValue();
                reader.nextName();
                windGust = reader.nextDouble();
                reader.endObject();
            } else if (name.equals("wind_direction_value_1")){
                reader.beginObject();
                reader.nextName();
                reader.skipValue();
                reader.nextName();
                windDir = reader.nextDouble();
                reader.endObject();
            } else if (name.equals("wind_speed_value_1")){
                reader.beginObject();
                reader.nextName();
                reader.skipValue();
                reader.nextName();
                windSpeed = reader.nextDouble();
                reader.endObject();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new WindResponse(windSpeed, windGust, windDir);
    }
}
