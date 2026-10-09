package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Showtime;
import com.rexchain.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimes;

    public ShowtimeService(ShowtimeRepository showtimes) {
        this.showtimes = showtimes;
    }

    @Transactional(readOnly = true)
    public void validateShowtimeSlot(Showtime candidate, Long currentId) {
        candidate.calculateEndTime();
        LocalDateTime start = candidate.getStartTime();
        LocalDateTime end = candidate.getEndTime();

        List<Showtime> nearby = showtimes.findByAuditoriumIdAndActiveTrueAndStartTimeBetweenOrderByStartTimeAsc(
                candidate.getAuditorium().getId(),
                start.minusHours(6),
                end.plusHours(6));

        for (Showtime other : nearby) {
            if (currentId != null && other.getId().equals(currentId)) {
                continue;
            }

            LocalDateTime otherEnd = other.getEndTime() != null
                    ? other.getEndTime()
                    : other.getStartTime().plusMinutes(other.getMovie().getDurationMinutes() + 15L);

            if (start.isBefore(otherEnd) && end.isAfter(other.getStartTime())) {
                throw new IllegalArgumentException(
                        "Phòng chiếu đã có suất phim '" + other.getMovie().getTitle() + "' từ "
                                + other.getStartTime().toLocalTime() + " đến " + otherEnd.toLocalTime()
                                + ". Cần cách nhau đủ thời lượng phim + 15 phút dọn phòng.");
            }
        }
    }
}
