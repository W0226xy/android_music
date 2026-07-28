package com.music.server.service;

import com.music.server.entity.Song;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

//JamendoService负责调用第三方在线音乐平台 Jamendo API，
// 并将返回的数据转换成项目内部 Song 对象的服务类。

@Service
//Spring 启动时扫描到@Service会创建JamendoService Bean然后可以被其他类调用
public class JamendoService {

    private static final String CLIENT_ID = "a98b754b";//Jamendo API认证信息

    private final RestTemplate restTemplate = new RestTemplate();
    //Spring提供的HTTP客户端，用于向其他服务器发送HTTP请求
    //这里是我们服务器访问Jamendo服务器（HTTP GET）获取json字符串

    private final ObjectMapper objectMapper = new ObjectMapper();//json解析工具
    //jamendo返回json，将之转化为Song对象

    public List<Song> getTracks() {//获取在线歌曲列表

        int offset = ThreadLocalRandom.current()//随机选取一个歌曲起始位置（方便后面每次刷新获取不同歌曲）
                .nextInt(0, 500);

        //拼接Jamendo请求URL
        String url =
                "https://api.jamendo.com/v3.0/tracks/"//歌曲接口
                        + "?client_id=" + CLIENT_ID//身份认证
                        + "&format=json"//返回指定json格式
                        + "&limit=20"//一次获取20个歌曲
                        + "&offset=" + offset//从第几个歌曲开始获取
                        + "&order=popularity_total"//歌曲按流行音乐热度排序
                        + "&audioformat=mp32"//指定音频格式
                        + "&imagesize=300"//指定封面大小
                        + "&include=lyrics";//要求返回歌词

        System.out.println("Jamendo请求offset：" + offset);

        return requestSongs(url);
    }

    private List<Song> requestSongs(String url) {

        String json = restTemplate.getForObject(//发送HTTP请求
                url,
                String.class
        );

        List<Song> songs = new ArrayList<>();

        try {

            JsonNode root = objectMapper.readTree(json);//解析json

            JsonNode results = root.get("results");//获取歌曲数组

            if (results == null || !results.isArray()) {//遍历歌曲
                return songs;
            }

            for (JsonNode item : results) {//json转化为song对象

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


                JsonNode lyricsNode = item.get("lyrics");//有些音乐是纯音乐没有歌词，做一下对应处理

                if (lyricsNode != null
                        && !lyricsNode.isNull()
                        && !lyricsNode.asText().isBlank()) {//有歌词就保存

                    song.setLyrics(lyricsNode.asText());

                } else {//没歌词就置空

                    song.setLyrics(null);
                }



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