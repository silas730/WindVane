package com.example.weathervane;

public class LocationResponse {
    private String name;
    private double distance;
    private String StId;
    private boolean restricted;

    public LocationResponse(String name, double distance, String StId, boolean restricted){
        this.name = name;
        this.distance = distance;

        this.StId = StId;
        this.restricted = restricted;
    }

    public String getName() {
        return name;
    }

    public String getStId() {
        return StId;
    }
    public boolean isRestricted(){
        return restricted;
    }
    public double getDistance(){
        return distance;
    }
    @Override
    public String toString(){
        return "Name: " + name +
                "\nStation Id: " + StId +
                "\nDistance: " + distance +
                "\nRestricted: " + restricted;

    }
}
