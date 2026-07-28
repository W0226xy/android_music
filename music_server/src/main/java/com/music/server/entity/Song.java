package com.music.server.entity;


public class Song {

    private Long id;//歌曲id

    private String name;//歌曲名称

    private String artist;//歌手

    private String url;//歌曲播放地址

    private String coverUrl;//歌曲封面地址

    private String lyricUrl;//歌词地址，实际客户端没有用到这个，服务器从jamendo获取到歌词之后存到了mysql，后续客户端要查歌词，是通过歌曲id去mysql里查

    private String source;//标记是本地测试音乐还是Jamendo获取的音乐

    private String lyrics;//歌词

    public String getSource() {
        return source;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }


    public void setSource(String source) {
        this.source = source;
    }

    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getArtist() {
        return artist;
    }


    public void setArtist(String artist) {
        this.artist = artist;
    }


    public String getUrl() {
        return url;
    }


    public void setUrl(String url) {
        this.url = url;
    }


    public String getCoverUrl() {
        return coverUrl;
    }


    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }


    public String getLyricUrl() {
        return lyricUrl;
    }


    public void setLyricUrl(String lyricUrl) {
        this.lyricUrl = lyricUrl;
    }
}