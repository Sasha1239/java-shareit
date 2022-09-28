package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.shareit.booking.enums.Status;
import ru.practicum.shareit.booking.model.Booking;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByBookerIdOrderByStartDesc(long userId);

    List<Booking> searchBookingByItemOwnerId(long id);

    List<Booking> searchBookingByBookerIdAndItemIdAndEndIsBefore(long id, long itemId, LocalDateTime time);

    List<Booking> searchBookingByItemOwnerIdAndStartIsAfterOrderByStartDesc(long id, LocalDateTime time);

    List<Booking> findByBookerIdAndStartAfterOrderByStartDesc(long userId, LocalDateTime time);

    List<Booking> findBookingsByItemIdAndEndIsBeforeOrderByEndDesc(long id, LocalDateTime time);

    List<Booking> findBookingsByItemIdAndStartIsAfterOrderByStartDesc(long id, LocalDateTime time);

    List<Booking> findBookingsByBookerIdAndStatusOrderByStartDesc(long userId, Status status);

    List<Booking> findBookingsByItemOwnerIdOrderByStartDesc(long id);

    @Query("select b " +
            "from Booking b left join User as us on b.booker.id = us.id " +
            "where us.id = ?1 " +
            "and ?2 between b.start and b.end " +
            "order by b.start DESC")
    List<Booking> findCurrentBookingsByBookerIdOrderByStartDesc(long userId, LocalDateTime time);

    @Query("select b " +
            "from Booking b left join Item as i on b.item.id = i.id " +
            "left join User as us on i.owner.id = us.id " +
            "where us.id = ?1 " +
            "and ?2 between b.start and b.end " +
            "order by b.start DESC")
    List<Booking> findCurrentBookingsByItemOwnerIdOrderByStartDesc(long userId, LocalDateTime time);

    List<Booking> findBookingsByBookerIdAndEndIsBeforeOrderByStartDesc(long userId, LocalDateTime time);

    List<Booking> findBookingsByItemOwnerIdAndEndIsBeforeOrderByStartDesc(long userId, LocalDateTime time);
}
