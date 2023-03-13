package ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.service;

import com.spire.doc.FileFormat;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.net.URL;

//Это файл исключительно для Шабулина и постоянного ее расписания!
public class SiteCommunication {
    private static final String timetablesPath = "C:\\Users\\Sasalomka\\Downloads\\TimetableBot2\\data\\";
    public static String downloadFile() throws IOException {
        Document site = Jsoup.connect("https://polytech-rzn.ru/?page_id=14410").get();

        Element dateElement = site.select("div.ramka:nth-child(10) > p:nth-child(3) > font:nth-child(1)").first();
        TelegramBot.date = dateElement.text();

        Element mainTimetableButton = site.select("div.ramka:nth-child(10) > p:nth-child(1) > a:nth-child(6)").first();
        String mainTimetableUrl = mainTimetableButton.attr("href");
        String[] urlArray1 = mainTimetableUrl.split("/");
        FileUtils.copyURLToFile(new URL(mainTimetableUrl), new File(timetablesPath + "actualTimetable.xlsx"));
        System.out.println("Загружен файл по ссылке: " + mainTimetableUrl);

        Element editTimetableButton = site.select("div.ramka:nth-child(10) > p:nth-child(3) > a:nth-child(8)").first();
        String editTimetableUrl = editTimetableButton.attr("href");
        String[] urlArray2 = editTimetableUrl.split("/");
        String fileName2 = urlArray2[urlArray2.length-1];
        FileUtils.copyURLToFile(new URL(editTimetableUrl), new File(timetablesPath + "timetables/" + fileName2));
        System.out.println("Загружен файл по ссылке: " + mainTimetableUrl);

        com.spire.doc.Document doc = new com.spire.doc.Document();
        doc.loadFromFile(timetablesPath + "timetables/" + fileName2, FileFormat.Doc);
        doc.saveToFile(timetablesPath + "editedTimetable.docx", FileFormat.Docx);

        return timetablesPath + "timetables/" + fileName2;
    }
}
