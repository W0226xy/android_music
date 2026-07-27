package com.music.server.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.server.entity.Song;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;


@Service
public class JamendoService {


    private static final String CLIENT_ID =
            "a98b754b";


    private final RestTemplate restTemplate =
            new RestTemplate();


    private final ObjectMapper objectMapper =
            new ObjectMapper();



    public List<Song> getTracks() {


        String url =
                "https://api.jamendo.com/v3.0/tracks"
                        + "?client_id=" + CLIENT_ID
                        + "&format=json"
                        + "&limit=5";


        List<Song> songs =
                new ArrayList<>();


        try {


            String json =
                    restTemplate.getForObject(
                            url,
                            String.class
                    );


            JsonNode root =
                    objectMapper.readTree(json);


            JsonNode tracks =
                    root
                            .path("results");



            for (JsonNode track : tracks) {


                Song song =
                        new Song();


                song.setId(
                        track
                                .path("id")
                                .asLong()
                );


                song.setName(
                        track
                                .path("name")
                                .asText()
                );


                song.setArtist(
                        track
                                .path("artist_name")
                                .asText()
                );


                song.setUrl(
                        track
                                .path("audio")
                                .asText()
                );


                song.setCoverUrl(
                        track
                                .path("image")
                                .asText()
                );


                songs.add(song);

            }


        } catch (Exception e) {


            e.printStackTrace();

        }


        return songs;

    }

}