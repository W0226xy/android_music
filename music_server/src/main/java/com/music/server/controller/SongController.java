package com.music.server.controller;


import com.music.server.entity.Song;
import com.music.server.service.JamendoService;
import com.music.server.service.SongService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/songs")
public class SongController {


    private final SongService songService;


    private final JamendoService jamendoService;



    public SongController(
            SongService songService,
            JamendoService jamendoService
    ){

        this.songService = songService;
        this.jamendoService = jamendoService;

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

        return jamendoService.getTracks();

    }

}