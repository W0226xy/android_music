package com.music.server.service;


import com.music.server.entity.Song;
import com.music.server.mapper.SongMapper;
import org.springframework.stereotype.Service;

import java.util.List;


//从数据库缓存中获取在线歌曲，如果没有缓存，则调用 Jamendo 在线音乐接口获取歌曲，并将结果保存到数据库，实现在线歌曲的缓存管理。
//为什么要缓存：如果每次打开在线音乐页面都直接请求第三方音乐平台：请求速度慢、第三方接口压力大、可能受到接口访问限制

@Service//表示这是 Spring 的业务层组件。
public class OnlineSongService {


    private final SongMapper songMapper;

    private final JamendoService jamendoService;



    public OnlineSongService(
            SongMapper songMapper,//操作数据库中的歌曲数据
            JamendoService jamendoService//调用Jamendo在线音乐API（eg：jamendoService.getTracks();）
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


        // 1. 先查数据库有没有已经保存的在线歌曲缓存。
        List<Song> songs =
                songMapper.findJamendoSongs();



        if (!songs.isEmpty()) {

            return songs;

        }



        // 2. 没有缓存，请求Jamendo获取歌曲

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