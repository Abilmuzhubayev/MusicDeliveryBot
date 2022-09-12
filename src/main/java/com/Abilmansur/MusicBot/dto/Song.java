package com.Abilmansur.MusicBot.dto;

import lombok.Data;

@Data
public class Song {
    private String name;
    private String downloadUrl;
    private String duration;
    private String artist;
}
