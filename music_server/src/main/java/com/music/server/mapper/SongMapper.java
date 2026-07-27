package com.music.server.mapper;


import com.music.server.entity.Song;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import java.util.List;


@Mapper
public interface SongMapper {


    @Select("select * from song")
    List<Song> findAll();

    @Select(
            "select * from song where source='JAMENDO'"
    )
    List<Song> findJamendoSongs();

    @Insert("""
            insert into song
            (id,name,artist,url,cover_url,lyric_url,lyrics,source)
            values
            (#{id},
             #{name},
             #{artist},
             #{url},
             #{coverUrl},
             #{lyricUrl},
             #{lyrics},
             #{source})
            """)
    void insert(Song song);

    @Delete(
            "delete from song where source='JAMENDO'"
    )
    void deleteJamendoSongs();

    @Select("select lyrics from song where id = #{id}")
    String findLyricsById(Long id);
}