package com.music.server.service;


import com.music.server.entity.Song;
import com.music.server.mapper.SongMapper;
import org.springframework.stereotype.Service;


import java.util.List;


@Service
public class SongService {


    private final SongMapper songMapper;


    public SongService(SongMapper songMapper){
        this.songMapper = songMapper;
    }


    public List<Song> getSongs(){

        return songMapper.findAll();

    }

}