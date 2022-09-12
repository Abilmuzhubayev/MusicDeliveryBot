package com.Abilmansur.MusicBot;

import com.Abilmansur.MusicBot.dto.Song;
import com.Abilmansur.MusicBot.service.RedisService;
import com.Abilmansur.MusicBot.service.SongService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
@Data
public class Bot extends TelegramLongPollingBot {

    @Autowired
    private SongService songService;

    @Autowired
    private RedisService redisService;

    @Value("${owner_chat_id}")
    private String ownerChatId;

    @Value("${telegram_bot_key}")
    private String token;

    @Autowired
    private Gson gson;

    private final String navigation = "navigation";

    @Override
    public String getBotUsername() {
        return "Music Delivery";
    }

    @Override
    public String getBotToken() {
        return token;
    }


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            try {
                log.info("user {} made a request: {}", update.getMessage().getFrom().getUserName(), update.getMessage().getText());
                manageQuery(update);
            } catch (Exception e) {
                log.error("Exception in updateReceived: ", e);
                sendSorryMessage(update.getMessage().getChatId().toString());
            }
        } else if (update.hasCallbackQuery()) {
            manageCallback(update.getCallbackQuery());
        }
    }


    private void manageCallback(CallbackQuery callbackQuery) {
        String data = callbackQuery.getData();
        if (data.startsWith(navigation)) {
            loadPages(callbackQuery);
        } else {
            sendMusic(callbackQuery);
        }
    }

    private void loadPages(CallbackQuery callbackQuery) {
        String key = callbackQuery.getData();
        String chatId = callbackQuery.getMessage().getChatId().toString();;
        Integer messageId = callbackQuery.getMessage().getMessageId();

        List<List<InlineKeyboardButton>> keyboard = getKeyboardFromCache(key);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(keyboard);

        EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup();
        editMessageReplyMarkup.setChatId(chatId);
        editMessageReplyMarkup.setMessageId(messageId);
        editMessageReplyMarkup.setReplyMarkup(markup);

        try {
            execute(editMessageReplyMarkup);
        } catch (Exception e) {
            log.error("Exception in loadPages: ", e);
            sendSorryMessage(callbackQuery.getFrom().getId().toString());
        }

    }

    private void sendMusic(CallbackQuery callbackQuery) {

        SendAudio sendAudio = songService.getSongByUrl(callbackQuery.getData());
        try {
            if (sendAudio != null) {
                sendAudio.setChatId(callbackQuery.getFrom().getId());
                execute(sendAudio);
            } else {
                sendNoCacheFound(callbackQuery.getFrom().getId().toString());
            }
        } catch (Exception e) {
            log.error("Exception in sendMusic: ", e);
            sendSorryMessage(callbackQuery.getFrom().getId().toString());
        }
    }

    private void sendNoCacheFound(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Упс. Ты немного затянул со скачиванием, поэтому я подумал, что тебе эта песня стала не нужна. " +
                "Пожалуйста, повтори поиск и скачай заново - тогда всё точно получится:)");
        try {
            execute(sendMessage);
        } catch (Exception e) {
            log.error("Exception in noCacheFound: ", e);
        }
    }

    private void sendSorryMessage(String chatId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Что-то пошло не так, прошу прощения за неудобства. Повтори запрос или вернись чуть позже.");
        try {
            execute(sendMessage);
        } catch (Exception e) {
            log.error("Exception in sendSorryMessage: ", e);
        }
    }

    private void sendNoResultForSearch(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("К сожалению, в базе данных, которую я использую еще нет этой песни или исполнителя. " +
                "А еще, возможно, ты допустил грамматическую ошибку.");
        try {
            execute(message);
        } catch (Exception e) {
            log.error("Exception in noResultForSearch: ", e);
        }

    }
    private void manageQuery(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            if (text.equals("/start")) {
                greeting(chatId);
            } else {
                searchForMusic(chatId, text);
            }
        }
    }

    private void searchForMusic(String chatId, String query) {

        List<Song> songs = songService.getSongs(query);

        if (songs.isEmpty()) {
            sendNoResultForSearch(chatId);
            return;
        }

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Вот, что мне удалось найти:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        initializeKeyBoard(songs, chatId, query);
        List<List<InlineKeyboardButton>> keyboard = getKeyboardFromCache(navigation + "|" + chatId + "|" + query + "|"  + 1);
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("TelegramApiException in searchForMusic: ", e);
        }
    }

    private void initializeKeyBoard(List<Song> songs, String chatId, String query) {

        int totalPages = songs.size() % 10 == 0 ? songs.size() / 10 : songs.size() / 10 + 1;
        int currentPage = 1;

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (int i = 1; i <= songs.size(); i++) {
            Song currentSong = songs.get(i - 1);
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(currentSong.getArtist() + " - " + currentSong.getName() + " - " + currentSong.getDuration());
            button.setCallbackData(songService.setCallbackMappings(chatId, query, i, songs.get(i - 1).getDownloadUrl()));
            buttons.add(button);
            keyboard.add(buttons);
            if (i % 10 == 0 || i == songs.size()) {
                String key = navigation + "|" + chatId + "|" + query + "|" + currentPage;
                keyboard.add(getNavigationButtons(currentPage, chatId, query, totalPages));;
                redisService.setValue(key, gson.toJson(keyboard));
                keyboard.clear();
                currentPage += 1;
            }
        }
    }

    private List<List<InlineKeyboardButton>> getKeyboardFromCache(String key) {
        Type token = new TypeToken<List<List<InlineKeyboardButton>>>(){}.getType();
        return gson.fromJson(redisService.getValue(key), token);
    }

    private List<InlineKeyboardButton> getNavigationButtons(int currentPage, String chatId, String query, int totalPages) {
        List<InlineKeyboardButton> navigationButtons = new ArrayList<>();

        if (currentPage > 1) {
            InlineKeyboardButton backButton = new InlineKeyboardButton();
            backButton.setText("назад");
            backButton.setCallbackData(navigation + "|" + chatId + "|" + query + "|" + (currentPage - 1));
            navigationButtons.add(backButton);
        }

        if (currentPage < totalPages) {
            InlineKeyboardButton forwardButton = new InlineKeyboardButton();
            forwardButton.setText("вперед");
            forwardButton.setCallbackData(navigation + "|" + chatId + "|" + query + "|" + (currentPage + 1));
            navigationButtons.add(forwardButton);
        }

        return navigationButtons;
    }


    private void greeting(String chatId) {
        SendMessage message = new SendMessage();
        message.setText("Привет! Я музыкальный бот, который найдет нужные тебе треки." +
                " Для осуществления поиска просто введи название(больше 3х символов) песни или артиста.");
        message.setChatId(chatId);
        try {
            execute(message);
        } catch (Exception e) {
            log.error("Exception in greeting: ", e);
        }
    }

}
