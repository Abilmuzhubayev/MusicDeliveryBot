package com.Abilmansur.MusicBot.service;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class WebScraper {

    @Autowired
    WebClient webClient;
    HtmlPage page;


    public void setUrl(String url) {
        page = getWebPage(url);
    }

    private HtmlPage getWebPage(String url) {
        try {
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setJavaScriptEnabled(false);
            return webClient.getPage(url);
        } catch (Exception e) {
            log.error("Exception thrown in getWebPage: ", e);
        }
        return null;
    }

    public List<String> extractArtists() {
        List<HtmlDivision> items = page.getByXPath("/html/body/div[1]/main/div/div/div[4]/div[2]/div/div[1]/div[1]/div[2]/div");
        List<String> songNames = new ArrayList<>();

        for (HtmlDivision domText : items) {
            String text = domText.getVisibleText();
            if (!StringUtils.isEmpty(text)) {
                songNames.add(text);
            }
        }
        return songNames;
    }

    public List<String> extractSongNames() {
        List<DomText> items = page.getByXPath("/html/body/div[1]/main/div/div/div[4]/div[2]/div/div[1]/div[1]/div[2]/a/text()");
        List<String> songNames = new ArrayList<>();

        for (DomText domText : items) {
            String text = domText.toString();
            if (!StringUtils.isEmpty(text)) {
                songNames.add(text);
            }
        }
        return songNames;
    }

    public List<String> extractDurations() {
        List<DomText> items = page.getByXPath("/html/body/div[1]/main/div/div/div[4]/div[2]/div/div[1]/div[2]/div[2]/text()");
        List<String> durations = new ArrayList<>();


        for (DomText domText: items) {
            String text = domText.toString();
            if (!StringUtils.isEmpty(text)) {
                durations.add(text);
            }
        }
        return durations;
    }


    public List<String> extractUrls() {
        List<HtmlAnchor> items = page.getByXPath("/html/body/div[1]/main/div/div/div[4]/div[2]/div/div[1]/div[1]/div[1]/a");
        List<String> urls = new ArrayList<>();

        for (HtmlAnchor anchor: items) {
            String text = anchor.toString();
            if (!StringUtils.isEmpty(text)) {
                text = text.substring(24, text.length() - 30);
                urls.add(text);
            }
        }
        return urls;
    }
}
