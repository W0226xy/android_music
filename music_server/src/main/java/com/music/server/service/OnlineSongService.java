package com.music.server.service;


import com.music.server.entity.Song;
import com.music.server.mapper.SongMapper;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class OnlineSongService {


    private final SongMapper songMapper;

    private final JamendoService jamendoService;



    public OnlineSongService(
            SongMapper songMapper,
            JamendoService jamendoService
    ) {

        this.songMapper = songMapper;
        this.jamendoService = jamendoService;

    }



    /**
     * 获取在线歌曲
     *
     * 优先读取数据库缓存
     * 没有缓存则请求Jamendo
     */
    public List<Song> getOnlineSongs() {


        // 1. 查询缓存
        List<Song> songs =
                songMapper.findJamendoSongs();



        if (!songs.isEmpty()) {

            return songs;

        }



        // 2. 没有缓存，请求Jamendo

        songs =
                jamendoService.getTracks();



        // 3. 保存缓存

        for (Song song : songs) {

            songMapper.insert(song);

        }



        return songs;

    }

    /**
     * 强制刷新在线歌曲缓存
     */
    public List<Song> refreshOnlineSongs(){


        //1. 删除旧缓存
        songMapper.deleteJamendoSongs();


        //2. 重新获取Jamendo歌曲
        List<Song> songs =
                jamendoService.getTracks();



        //3. 保存新的缓存
        for(Song song : songs){

            songMapper.insert(song);

        }


        return songs;

    }
}