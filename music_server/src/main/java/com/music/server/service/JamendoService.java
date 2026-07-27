package com.music.server.service;

import com.music.server.entity.Song;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class JamendoService {

    private static final String CLIENT_ID = "a98b754b";

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Song> getTracks() {

        int offset = ThreadLocalRandom.current()
                .nextInt(0, 500);

        String url =
                "https://api.jamendo.com/v3.0/tracks/"
                        + "?client_id=" + CLIENT_ID
                        + "&format=json"
                        + "&limit=20"
                        + "&offset=" + offset
                        + "&order=popularity_total"
                        + "&audioformat=mp32"
                        + "&imagesize=300"
                        + "&include=lyrics";

        System.out.println("Jamendo请求offset：" + offset);

        return requestSongs(url);
    }

    private List<Song> requestSongs(String url) {

        String json = restTemplate.getForObject(
                url,
                String.class
        );

        List<Song> songs = new ArrayList<>();

        try {

            JsonNode root = objectMapper.readTree(json);

            JsonNode results = root.get("results");

            if (results == null || !results.isArray()) {
                return songs;
            }

            for (JsonNode item : results) {

                Song song = new Song();

                song.setId(
                        item.get("id").asLong()
                );

                song.setName(
                        item.get("name").asText()
                );

                song.setArtist(
                        item.get("artist_name").asText()
                );

                song.setUrl(
                        item.get("audio").asText()
                );

                song.setCoverUrl(
                        item.get("image").asText()
                );


                JsonNode lyricsNode = item.get("lyrics");

                if (lyricsNode != null
                        && !lyricsNode.isNull()
                        && !lyricsNode.asText().isBlank()) {

                    song.setLyrics(lyricsNode.asText());

                } else {

                    song.setLyrics(null);
                }

                song.setLyricUrl(null);

                song.setLyricUrl(null);

                song.setSource("JAMENDO");

                songs.add(song);
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "解析 Jamendo 数据失败",
                    e
            );

        }

        return songs;
    }
}