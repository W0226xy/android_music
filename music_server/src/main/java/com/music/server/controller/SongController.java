package com.music.server.controller;

import com.music.server.entity.Song;
import com.music.server.service.JamendoService;
import com.music.server.service.OnlineSongService;
import com.music.server.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

//Spring MVC REST API:通过 Controller 接收 Android 客户端发送的 HTTP 请求，
// 然后调用业务层处理数据，最后以 JSON 等格式返回结果

//SongController 是 Spring Boot 服务端中的控制层（Controller Layer），
// 主要负责接收 Android 客户端的 HTTP 请求，并调用对应的 Service 完成功能，然后返回结果。

@RestController//表示这是一个 REST API 控制器。作用是接收HTTP请求，返回JSON数据
//eg:客户端请求GET http://localhost:8080/songs
//controller返回[
// {
//  "id":1,
//  "name":"晴天",
//  "artist":"xxx"
// }
//]

@RequestMapping("/songs")//表示该 Controller 所有接口统一加前缀
//eg:@GetMapping实际访问GET /songs
//eg:@GetMapping("/jamendo")实际访问GET /songs/jamendo
public class SongController {


    private final SongService songService;//本地数据库歌曲管理


    private final OnlineSongService onlineSongService;//第三方在线音乐获取



    public SongController(
            SongService songService,
            OnlineSongService onlineSongService
    ){

        this.songService = songService;
        this.onlineSongService = onlineSongService;
        //这里使用的是 Spring 的依赖注入（Dependency Injection）
        //Spring启动时发现SongController需要SongService、OnlineSongService
        //会自动创建SongService Bean和OnlineSongService Bean
        //Bean是由Spring容器创建和管理的Java对象
        //普通java对象：SongService songService = new SongService();我们自己创建和管理生命周期
        //Bean是由Spring管理
    }



    /**
     * 获取数据库中的歌曲
     */
    @GetMapping
    public List<Song> getSongs(){

        return songService.getSongs();

    }



    /**
     * 获取 Jamendo 在线歌曲
     */
    @GetMapping("/jamendo")
    public List<Song> jamendo(){

        return onlineSongService.getOnlineSongs();

    }

    /**
     * 刷新Jamendo歌曲缓存
     */
    @GetMapping("/jamendo/refresh")
    public List<Song> refreshJamendo(){

        return onlineSongService.refreshOnlineSongs();

    }

    @GetMapping("/{id}/lyrics")//获取歌词
    public String getLyrics(@PathVariable Long id) {

        return songService.getLyrics(id);
    }

}