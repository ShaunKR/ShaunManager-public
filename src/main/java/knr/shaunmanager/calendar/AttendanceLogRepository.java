package knr.shaunmanager.calendar;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, String> {
}
