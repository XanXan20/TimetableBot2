package ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimetableBot2Application {

	public static void main(String[] args) {
		try {
			SpringApplication.run(TimetableBot2Application.class, args);
			System.out.println("Проект запущен");
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

}
