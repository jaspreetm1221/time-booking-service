package com.fantasy.tbs.repository;

import com.fantasy.tbs.domain.TimeBooking;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data SQL repository for the TimeBooking entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TimeBookingRepository extends JpaRepository<TimeBooking, Long> {
    List<TimeBooking> findAllByPersonalNumberOrderByBooking(String personalNumber);

    boolean existsByPersonalNumberAndBookingIsBetweenOrderByBooking(String personalNumber, ZonedDateTime startTime, ZonedDateTime endTime);
}
