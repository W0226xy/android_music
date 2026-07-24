package com.music.server.controller;


import com.music.server.entity.Song;
import com.music.server.service.SongService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/songs")
public class SongController {


    private final SongService songService;


    public SongController(SongService songService){

        this.songService = songService;

    }


    @GetMapping
    public List<Song> getSongs(){

        return songService.getSongs();

    }

}