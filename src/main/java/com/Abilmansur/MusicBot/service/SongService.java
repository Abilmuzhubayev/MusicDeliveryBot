package com.Abilmansur.MusicBot.service;

import com.Abilmansur.MusicBot.dto.Song;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.*;

@Slf4j
@Service
public class SongService {
    @Value("${source_url}")
    private String sourceURL;
    @Autowired
    WebScraper webScraper;

    @Autowired
    RedisService redisService;

    public SongService() {
    }

    public String setCallbackMappings(String chatId, String query, int number, String url) {
        String key = chatId + "|" + query + "|" + number;
        redisService.setValue(key, url);
        return key;
    }

    public List<Song> constructSongs(List<String> downloadURLs, List<String> durations, List<String> songNames, List<String> artists) {

        List<Song> songs = new ArrayList<>();
        for (int i = 0; i < downloadURLs.size(); i++) {
            Song song = new Song();
            song.setName(songNames.get(i));
            song.setDownloadUrl(downloadURLs.get(i));
            song.setDuration(durations.get(i));
            song.setArtist(artists.get(i));
            songs.add(song);
        }
        return songs;
    }


    public SendAudio getSongByUrl(String key) {
        String url = redisService.getValue(key);
        if (url == null) {
            return null;
        }
        SendAudio sendAudio = new SendAudio();
        sendAudio.setAudio(new InputFile(url));
        return sendAudio;
    }

    public List<Song> getSongs(String query) {
        try {
            webScraper.setUrl(sourceURL + query);
            List<String> downloadUrls = webScraper.extractUrls();
            List<String> songNames = webScraper.extractSongNames();
            List<String> durations = webScraper.extractDurations();
            List<String> artists = webScraper.extractArtists();
            return constructSongs(downloadUrls, durations, songNames, artists);
        } catch (Exception e) {
            log.error("Exception in getSongs: ", e);
        }
        return null;
    }

    @Override
    public String toString() {
        return "SongService{}";
    }
}
