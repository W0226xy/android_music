package com.music.server.controller;


import com.music.server.entity.Song;
import com.music.server.service.JamendoService;
import com.music.server.service.OnlineSongService;
import com.music.server.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;


@RestController
@RequestMapping("/songs")
public class SongController {


    private final SongService songService;


    private final OnlineSongService onlineSongService;



    public SongController(
            SongService songService,
            OnlineSongService onlineSongService
    ){

        this.songService = songService;
        this.onlineSongService = onlineSongService;

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

    @GetMapping("/{id}/lyrics")
    public String getLyrics(@PathVariable Long id) {

        return songService.getLyrics(id);
    }

}