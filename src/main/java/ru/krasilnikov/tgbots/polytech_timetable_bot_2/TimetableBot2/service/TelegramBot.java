package ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.config.BotConfig;
import ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.excel.XLSXFileReader;
import ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.model.User;
import ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.model.UserRepository;
import ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.word.DocFileReader;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    XLSXFileReader excelFileReader;
    DocFileReader docFileReader;
    static String date = "{тут должна быть дата}";
    String oldPath;//тут крч записывается старое расписание, и када новое приходит, он названия сравнивает
    @Autowired
    private UserRepository userRepository;
    final static String END_TIMETABLE_MESSAGE = "\n" +
            "Не ваше расписание?\n" +
            "/mygroup - ваша группа\n" +
            "/changegroup [номер_группы] - выбрать вашу группу\n\n" +
            "Пожертвовать на хостинг можно на Сбер или ВТБ:\n" +
            "+7 (910) 560-53-36";

    final static String HELP_TXT =
                    "Доступные команды:\n\n"+

                    "\t/changegroup [номер_группы]\n(напр. /changegroup 71)- подписаться на уведомления по расписанию для вашей группы. ПРИ ПЕРВОМ ИСПОЛЬЗОВАНИИ БОТА ДЕЛАТЬ ОБЯЗАТЕЛЬНО\n"+
                    "\t/mygroup - узнать свою текущую группу\n"+
                    "\t/timetable - узнать расписание своей группы\n\n"+

                    "Если есть желание поддержать разработку бота, то пожертвования на оплату хостинга можно скинуть на карту но номеру 89105605336 сбер\n\n" +

                    "При обнаружении багов, или если есть какие-либо пожелания, то пишите мне в тг:\n"+
                    "@Sasalomka";
    Timer timer;
    final BotConfig config;
    public TelegramBot(BotConfig config) {
        this.config = config;
        try{
            SiteCommunication.downloadFile();
            this.excelFileReader = new XLSXFileReader();
            this.docFileReader = new DocFileReader();

            timer = new Timer();

        }catch (IOException ex){
            ex.printStackTrace();
        }
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText() && !update.getMessage().hasDocument()){

            String[] messageText = update.getMessage().getText().split(" ");
            long chatId = update.getMessage().getChatId();

            switch (messageText[0]){
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "/help":
                    helpCommandReceived(chatId);
                    break;
                case "/changegroup":
                    if(messageText.length == 2) {
                        String stringGroupId = messageText[1];

                        changeGroupCommandReceiver(update.getMessage(), stringGroupId);
                    }
                    else{
                        sendMessage(update.getMessage().getChatId(), "Используйте шаблон /changegroup [номер группы]");
                    }
                    break;
                case "/mygroup":
                    myGroupCommandReceiver(update.getMessage().getChatId());
                    break;
                case "/timetable":
                    timetableCommandReceiver(chatId);
                    break;
                case "/update":
                    checkNewTimetable();
                    break;
                default:
                    sendMessage(chatId, "Простите, эта команда неправильна, или не поддерживается.");
            }
        }
    }

    private void registerUser(Message msg) {

        if(userRepository.findById(msg.getChatId()).isEmpty()){

            Long chatId = msg.getChatId();
            Chat chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            user.setNotice(true);

            userRepository.save(user);
        }
    }
    private void startCommandReceived(long chatId, String name){
        String answer = "Привет, " + name + ", будем знакомы!\n" +
                "Скоро ты сможешь смотреть расписание в нашем ЗАМЕЧАТЕЛЬНОМ КОЛЛЕДЖЕ\n\n" +
                "P.S. ОБЯЗАТЕЛЬНО отправь /help, чтобы узнать больше о боте, а еще не забудь выбрать группу, без этого бот работать не будет";

        sendMessage(chatId, answer);
    }
    private void helpCommandReceived(long chatId){
        sendMessage(chatId, HELP_TXT);
    }
    private void myGroupCommandReceiver(long chatId){

        Optional<User> optionalUser = userRepository.findById(chatId);
        User user = optionalUser.get();

        if(user.getGroupId() == 0){
            sendMessage(chatId, "Вы еще не выбрали свою группуб используйте /changegroup [номер группы] для выбора своей группы");
        }
        else {
            sendMessage(chatId, "Номер вашей группы: " + user.getGroupId());
        }
    }
    private void changeGroupCommandReceiver(Message msg, String stringGroupId){

        int intGroupId = 0;
        try {
            intGroupId = Integer.parseInt(stringGroupId);
        }catch (Exception e){
            log.error("Group Id not changed: exception");
            sendMessage(msg.getChatId(), "Ошибка в прочтении номера группы, возможно вы использовали буквы, а не числа, попробуйте еще раз.");
            return;
        }

        ArrayList<Integer> groupList = excelFileReader.getGroupIdList();

        for(int i : groupList){
            if(intGroupId == i){
                Optional<User> optionalUser = userRepository.findById(msg.getChatId());

                User user = optionalUser.get();
                user.setGroupId(intGroupId);

                userRepository.save(user);

                sendMessage(msg.getChatId(), "Ваша группа успешно изменена.");
                return;
            }
        }
        sendMessage(msg.getChatId(), "Такой группы не найдено, проверьте корректность введенных данных");
    }
    private void timetableCommandReceiver(long chatId){

        Optional<User> optionalUser = userRepository.findById(chatId);
        User user = optionalUser.get();
        int userGroup = user.getGroupId();

        sendMessage(chatId,findEditedGroupTimetable(userGroup) + END_TIMETABLE_MESSAGE);

    }

    private void noticeCommandReceiver(){


        ArrayList<Integer> list = excelFileReader.getGroupIdList();
        Iterable<User> users = userRepository.findAll();

        for (Integer i:
                list) {

            String timetable = findEditedGroupTimetable(i);

            for (User user : users) {
                if (user.getGroupId() == i && user.isNotice()) {

                    sendMessage(user.getChatId(), timetable + END_TIMETABLE_MESSAGE);
                    sendFile(user.getChatId(), new java.io.File(oldPath));
                }
            }
        }
    }
    private void sendMessage(long chatId, String textToSend){
        SendMessage message = new SendMessage();

        message.setChatId(chatId);
        message.setText(textToSend);

        try {
            execute(message);
        }catch(TelegramApiException e){

        }
    }
    private void sendMessage(SendMessage sendMessage){
        try{
            execute(sendMessage);
        }catch (TelegramApiException e){
            e.printStackTrace();
        }
    }
    private void sendFile(long chatId, java.io.File file){

        Long longChatId = chatId;

        InputFile inputFile = new InputFile(file);

        SendDocument sendDocument = new SendDocument(longChatId.toString(), inputFile);
        try{
            execute(sendDocument);
        }catch (TelegramApiException e){
            System.out.println(e.getMessage());
            sendMessage(634876835, "ЧТО-ТО СЛОМАЛОСЬ!!! Отправка файла");
        }
    }

    public String findEditedGroupTimetable(int groupId){

        //прочитать измененное расписание
        Map<Integer, String> timetableChanges = docFileReader.getChanges(groupId);
        //узнать текущий день недели
        int[] dateInt = docFileReader.getData();
        LocalDate localDate = LocalDate.of(dateInt[2],dateInt[1],dateInt[0]);
        //запросить дефолтное расписание для этого дня
        Map<Integer, String> defaultTimetable;
        switch(localDate.getDayOfWeek()){
            case MONDAY:
                defaultTimetable = excelFileReader.getGroupTimetable(groupId, 1);
                break;
            case TUESDAY:
                defaultTimetable = excelFileReader.getGroupTimetable(groupId, 2);
                break;
            case WEDNESDAY:
                defaultTimetable = excelFileReader.getGroupTimetable(groupId, 3);
                break;
            case THURSDAY:
                defaultTimetable = excelFileReader.getGroupTimetable(groupId, 4);
                break;
            case FRIDAY:
                defaultTimetable = excelFileReader.getGroupTimetable(groupId, 5);
                break;
            default:
                defaultTimetable = null;
        }
        //изменить дефолтное расписание
        for (int i = 1; i < 15; i++){
            if(timetableChanges.get(i)!=null){
                defaultTimetable.put(i,timetableChanges.get(i));
            }
        }
        //сделать красивый вывод

        String answer = "Расписание для группы №" + groupId + " на " + localDate + ":\n\n";

        for (int i = 1; i < 15; i++) {

            if(defaultTimetable.get(i) != null) {


                String[] lesionName = defaultTimetable.get(i).split(",");

                answer += i + " - ";
                for (String str : lesionName) {
                    str = str.trim();
                    str = str.replaceAll("\\s+", " ");
                    answer += str + " | ";
                }
                answer += "\n";
            }
        }
        return answer;
    }

    private void checkNewTimetable(){
        try{
            String path = SiteCommunication.downloadFile();
            if(!path.equals(oldPath)) {
                timer.cancel();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        checkNewTimetable();
                    }
                }, 72000000 , 7200000);
                oldPath = path;

                System.out.println("Файл загружен");
                excelFileReader.update();
                noticeCommandReceiver();
            }

        }catch (Exception e){
            e.printStackTrace();
            sendMessage(634876835, "ЧТО-ТО СЛОМАЛОСЬ!!!");
        }
    }
}
