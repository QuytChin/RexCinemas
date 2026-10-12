package com.rexchain.cinema.service;

import com.rexchain.cinema.entity.Showtime;
import com.rexchain.cinema.repository.AuditoriumRepository;
import com.rexchain.cinema.repository.ShowtimeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShowtimeService {

    private final ShowtimeRepository showtimes;
    private final AuditoriumRepository auditoriums;

    public ShowtimeService(ShowtimeRepository showtimes, AuditoriumRepository auditoriums) {
        this.showtimes = showtimes;
        this.auditoriums = auditoriums;
    }

    @Transactional
    public void validateShowtimeSlot(Showtime candidate, Long currentId) {
        if (candidate.getAuditorium() == null || candidate.getAuditorium().getId() == null
                || candidate.getMovie() == null || candidate.getStartTime() == null)
            throw new IllegalArgumentException("Thiếu phim, phòng chiếu hoặc thời gian bắt đầu");
        if (candidate.getMovie().getDurationMinutes() <= 0)
            throw new IllegalArgumentException("Thời lượng phim phải lớn hơn 0");

        // Serializes validation and save operations for one auditorium.
        candidate.setAuditorium(auditoriums.findByIdForUpdate(candidate.getAuditorium().getId())
                .orElseThrow(() -> new IllegalArgumentException("Phòng chiếu không tồn tại")));
        candidate.calculateEndTime();
        LocalDateTime start = candidate.getStartTime();
        LocalDateTime end = candidate.getEndTime();

        List<Showtime> nearby = showtimes.findByAuditoriumIdAndActiveTrueOrderByStartTimeAsc(
                candidate.getAuditorium().getId());

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
