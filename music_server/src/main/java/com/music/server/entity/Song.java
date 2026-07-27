package com.music.server.entity;


public class Song {

    private Long id;

    private String name;

    private String artist;

    private String url;

    private String coverUrl;

    private String lyricUrl;

    private String source;

    private String lyrics;

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