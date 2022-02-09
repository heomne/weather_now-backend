package com.weathernow.server.controller;

import com.google.gson.*;
import com.weathernow.server.domain.UltraSrtFcst;
import com.weathernow.server.domain.WeatherData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/")
public class APIController {

    @GetMapping("forecast")
    public List<WeatherData> forecast() throws IOException {

        URL url = buildURL();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        int responseCode = conn.getResponseCode();

        log.info("Response code: {}", responseCode);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();

        String temp = "";
        while((temp = br.readLine()) != null) {
            sb.append(temp);
        }

        JsonObject jsonObject = (JsonObject) JsonParser.parseString(sb.toString());
        JsonObject parse_response = (JsonObject) jsonObject.get("response");
        // JsonElement parse_header = (JsonObject) jsonObject.get("header");

        JsonObject parse_body = (JsonObject) parse_response.get("body");
        JsonObject parse_items = (JsonObject) parse_body.get("items");
        JsonArray array = (JsonArray) parse_items.get("item");

        List<WeatherData> weathers = new ArrayList<WeatherData>();
        for(int i=0; i<array.size(); i++) {
            JsonElement jsonElement = array.get(i);

            Gson gson = new Gson();
            WeatherData weatherData = gson.fromJson(jsonElement, WeatherData.class);
            weatherData.setCategory(UltraSrtFcst.valueOf(weatherData.getCategory()).getName());

            weathers.add(weatherData);
        }

        br.close();
        conn.disconnect();

        return weathers;
    }

    private URL buildURL() throws MalformedURLException {
        String endPointURI = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtFcst";

        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String hour = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH"));
        String min = LocalDateTime.now().format(DateTimeFormatter.ofPattern("mm"));

        // min이 30 미만일 경우 hour를 1 내림
        if(Integer.parseInt(min) < 30) {
            int modifiedHour = Integer.parseInt(hour) - 1;
            hour = String.format("%02d", modifiedHour);
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endPointURI)
                .queryParam("serviceKey", "WIhsB06waJJMJZMm/M4SkVOW7q/e0dtIWgG/jNK9eovNSpJl2jaCpkaUOpX6SSgDd4CbGTXZNEeYzl0RZ9e2Sg==")
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "60")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", date)
                .queryParam("base_time", hour + min) // 30분 발표
                .queryParam("nx", "36")
                .queryParam("ny", "127");

        log.info("Forcast Request URL = {}", builder.build().encode().toUri().toURL());

        return builder.build().encode().toUri().toURL();
    }
}
