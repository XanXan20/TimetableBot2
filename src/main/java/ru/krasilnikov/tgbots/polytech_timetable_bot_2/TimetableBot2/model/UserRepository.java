package ru.krasilnikov.tgbots.polytech_timetable_bot_2.TimetableBot2.model;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {
}
