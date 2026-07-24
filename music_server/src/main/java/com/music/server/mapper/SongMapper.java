package com.music.server.mapper;


import com.music.server.entity.Song;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface SongMapper {


    @Select("select * from song")
    List<Song> findAll();

}