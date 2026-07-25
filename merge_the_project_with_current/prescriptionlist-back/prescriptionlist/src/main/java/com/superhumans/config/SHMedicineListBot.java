package com.superhumans.config;

import com.superhumans.repository.MedicineListRepository;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashSet;
import java.util.Set;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SHMedicineListBot extends TelegramLongPollingBot {

    final MedicineListRepository medicineListRepository;

    @Value("${telegram.bot.key}")
    String botToken;

    @Value("${telegram.bot.username}")
    String botUsername;

    Set<Long> chatIds;

    public SHMedicineListBot(MedicineListRepository medicineListRepository) {
        this.medicineListRepository = medicineListRepository;
        this.chatIds = new HashSet<>(medicineListRepository.getAllChatIds());
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            sendMessage(chatId, "You've subscribed to notifications.");
            medicineListRepository.addNewChatId(chatId);
        }
    }

    public void sendNotification(String messageText) {
        chatIds = new HashSet<>(medicineListRepository.getAllChatIds());
        for (Long chatId : chatIds) {
            sendMessage(chatId, messageText);
        }
    }

    private void sendMessage(Long chatId, String messageText) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(messageText);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}