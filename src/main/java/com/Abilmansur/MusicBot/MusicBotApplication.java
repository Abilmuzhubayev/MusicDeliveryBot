package com.Abilmansur.MusicBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@SpringBootApplication
@ComponentScan(value = "com.Abilmansur")
@Slf4j
public class MusicBotApplication {


	public static void main(String[] args){
		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
			SpringApplicationBuilder builder = new SpringApplicationBuilder(MusicBotApplication.class);
			builder.headless(false);
			ConfigurableApplicationContext ctx = builder.run(args);
			Bot bot = ctx.getBean(Bot.class);
			botsApi.registerBot(bot);
			builder.run(args);
		} catch (TelegramApiException e) {
			log.error("Exception in main: ", e);
		}

	}

}
