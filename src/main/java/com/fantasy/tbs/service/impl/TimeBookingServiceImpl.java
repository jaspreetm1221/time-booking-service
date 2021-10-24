package com.fantasy.tbs.service.impl;

import static java.util.stream.Collectors.groupingBy;

import com.fantasy.tbs.domain.TimeBookDTO;
import com.fantasy.tbs.domain.TimeBooking;
import com.fantasy.tbs.repository.TimeBookingRepository;
import com.fantasy.tbs.service.TimeBookingService;
import com.fantasy.tbs.service.mapper.TimeBookMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link TimeBooking}.
 */
@Service
@Transactional
public class TimeBookingServiceImpl implements TimeBookingService {

    private final Logger log = LoggerFactory.getLogger(TimeBookingServiceImpl.class);

    private final TimeBookingRepository timeBookingRepository;
    private final TimeBookMapper timeBookMapper;

    public TimeBookingServiceImpl(TimeBookingRepository timeBookingRepository, TimeBookMapper timeBookMapper) {
        this.timeBookingRepository = timeBookingRepository;
        this.timeBookMapper = timeBookMapper;
    }

    @Override
    public TimeBooking save(TimeBooking timeBooking) {
        log.debug("Request to save TimeBooking : {}", timeBooking);
        return timeBookingRepository.save(timeBooking);
    }

    @Override
    public Optional<TimeBooking> partialUpdate(TimeBooking timeBooking) {
        log.debug("Request to partially update TimeBooking : {}", timeBooking);

        return timeBookingRepository
            .findById(timeBooking.getId())
            .map(
                existingTimeBooking -> {
                    if (timeBooking.getBooking() != null) {
                        existingTimeBooking.setBooking(timeBooking.getBooking());
                    }
                    if (timeBooking.getPersonalNumber() != null) {
                        existingTimeBooking.setPersonalNumber(timeBooking.getPersonalNumber());
                    }

                    return existingTimeBooking;
                }
            )
            .map(timeBookingRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeBooking> findAll() {
        log.debug("Request to get all TimeBookings");
        return timeBookingRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TimeBooking> findOne(Long id) {
        log.debug("Request to get TimeBooking : {}", id);
        return timeBookingRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        log.debug("Request to delete TimeBooking : {}", id);
        timeBookingRepository.deleteById(id);
    }

    @Override
    public void bookTime(TimeBookDTO timeBookDTO) {
        timeBookingRepository.save(timeBookMapper.toTimeBooking(timeBookDTO));
    }

    @Override
    public long workingTime(String personalNumber) {
        Map<LocalDate, List<TimeBooking>> groupedBooking = timeBookingRepository
            .findAllByPersonalNumberOrderByBooking(personalNumber)
            .stream()
            .collect(
                groupingBy(
                    timeBooking -> {
                        return timeBooking.getBooking().toLocalDate();
                    }
                )
            );
        long time = 0;
        for (LocalDate localDate : groupedBooking.keySet()) {
            List<TimeBooking> timeBookings = groupedBooking.get(localDate);
            // Assuming that a valid day consists of a minimum of 2 bookings and every checkin has a corresponding checkout.
            // Failing to fulfill the above conditions does not count as a valid day, and all the hours for that day are ignored.
            if (timeBookings.size() > 1 && timeBookings.size() % 2 == 0) {
                for (int i = 1; i < timeBookings.size(); i += 2) {
                    time += Duration.between(timeBookings.get(i).getBooking(), timeBookings.get(i - 1).getBooking()).toMillis();
                }
            }
        }
        return time;
    }

    @Override
    public boolean hasWorkedOnDay(String personalNumber, LocalDate localDate) {
        // Assuming that the user has entered at least one booking for a specific day.
        return timeBookingRepository.existsByPersonalNumberAndBookingIsBetweenOrderByBooking(
            personalNumber,
            localDate.atStartOfDay(ZoneId.systemDefault()),
            localDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault())
        );
    }
}
