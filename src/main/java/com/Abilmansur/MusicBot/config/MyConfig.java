package com.Abilmansur.MusicBot.config;

import com.gargoylesoftware.htmlunit.WebClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyConfig {
    @Bean
    public WebClient webClient() {
        return new WebClient();
    }
}
