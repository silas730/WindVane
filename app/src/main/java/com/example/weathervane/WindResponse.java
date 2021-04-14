package com.example.weathervane;

public class WindResponse {

    private double windSpeed;
    private double windDir;
    private double windGust;
    public WindResponse(double windSpeed, double windGust, double windDir){
        this.windDir = windDir;
        this.windGust = windGust;
        this.windSpeed = windSpeed;
    }

    public double getWindSpeed() {
        return windSpeed;
    }
    public double getWindDir() {
        return windDir;
    }
    public double getWindGust() {
        return windGust;
    }
}
