package com.example.weathervane;

import org.junit.Test;
import com.example.weathervane.APIhandler;
import com.example.weathervane.LocationResponse;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testAPI(){
        APIhandler api = new APIhandler();
         String test = api.findStations("44.155","-73.294", 20);
         assertEquals(test, "GB");
    }
}