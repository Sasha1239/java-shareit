package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoSimple;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.booking.enums.Status;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.*;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    //Создание брони
    @Override
    public BookingDto create(BookingDtoSimple bookingDtoSimple, long userId) {
        if (bookingDtoSimple.getEnd().isBefore(bookingDtoSimple.getStart())) {
            throw new ValidationException("Время окончания не может быть больше времени начала");
        }

        Booking booking = BookingMapper.fromSimpleToBooking(bookingDtoSimple);

        booking.setBooker(userRepository.findById(userId).orElseThrow());

        Item item = itemRepository.findById(bookingDtoSimple.getItemId())
                .orElseThrow(() -> new NotFoundException("Неверный идентификатор вещи"));

        if (!item.getAvailable()) {
            throw new AvailableException("Эта вещь недоступна для аренды");
        }
        if (item.getOwner().getId() == userId) {
            throw new NotFoundException("Владелец вещи не может забронировать свою вещь");
        }

        booking.setItem(item);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    //Получение брони
    @Override
    public BookingDto getBooking(long bookingId, long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Неверный идентификатор брони"));

        if (booking.getBooker().getId() != userId && booking.getItem().getOwner().getId() != userId) {
            throw new NotFoundException("Неверный идентификатор пользователя");
        }

        return BookingMapper.toBookingDto(booking);
    }

    //Получение всех бронирований
    public List<BookingDto> getAll(long userId, String state, int from, int size) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Неверный идентификатор пользователя"));

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("start").descending());

        switch (Status.valueOf(state)) {
            case ALL:
                return bookingRepository.findByBookerId(userId, pageable)
                        .stream()
                        .map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case CURRENT:
                return  bookingRepository.findCurrentBookingsByBookerId(userId, LocalDateTime.now(),
                        pageable).stream().map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case PAST:
                return bookingRepository.findBookingsByBookerIdAndEndIsBefore(userId, LocalDateTime.now(),
                        pageable).stream().map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case FUTURE:
                return bookingRepository.findByBookerIdAndStartAfter(userId, LocalDateTime.now(), pageable)
                        .stream().map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case WAITING:
                return bookingRepository.findBookingsByBookerIdAndStatus(userId,
                                Status.WAITING, pageable)
                        .stream()
                        .map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case REJECTED:
                return bookingRepository.findBookingsByBookerIdAndStatus(userId,
                                Status.REJECTED, pageable)
                        .stream()
                        .map(BookingMapper::toBookingDto).collect(Collectors.toList());
            default:
                throw new ValidationException("Unknown state: UNSUPPORTED_STATUS");
        }
    }

    //Получение всех бронирований пользователя
    @Override
    public List<BookingDto> getAllBookingByOwner(long userId, String state, int from, int size) {
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Неверный идентификатор пользователя"));

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size, Sort.by("start").descending());

        List<BookingDto> result = bookingRepository.searchBookingByItemOwnerId(userId, pageable).stream()
                .map(BookingMapper::toBookingDto).collect(Collectors.toList());

        if (result.isEmpty()) {
            throw new NotFoundException("У пользователя нет вещей");
        }

        switch (Status.valueOf(state)) {
            case ALL:
                return result;
            case CURRENT:
                return bookingRepository.findCurrentBookingsByItemOwnerId(userId, LocalDateTime.now(),
                        pageable).stream().map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case PAST:
                return bookingRepository.findBookingsByItemOwnerIdAndEndIsBefore(userId, LocalDateTime.now(),
                        pageable).stream().map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case FUTURE:
                return bookingRepository.searchBookingByItemOwnerIdAndStartIsAfter(userId, LocalDateTime.now(),
                        pageable).stream().map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case WAITING:
                return bookingRepository.findBookingsByItemOwnerId(userId, pageable).stream().skip(from)
                        .filter(booking -> booking.getStatus().equals(Status.WAITING))
                        .map(BookingMapper::toBookingDto).collect(Collectors.toList());
            case REJECTED:
                return bookingRepository.findBookingsByItemOwnerId(userId, pageable).stream().skip(from)
                        .filter(booking -> booking.getStatus().equals(Status.REJECTED))
                        .map(BookingMapper::toBookingDto).collect(Collectors.toList());
            default:
                throw new ValidationException("Unknown state: UNSUPPORTED_STATUS");
        }
    }


    //Подтверждение брони
    @Override
    public BookingDto approve(long userId, long bookingId, Boolean approved) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Неверный идентификатор брони"));

        BookingDto bookingDto = BookingMapper.toBookingDto(booking);

        if (bookingDto.getItem().getOwner().getId() != userId) {
            throw new NotFoundException("Подтвердить бронирование может только владелец вещи");
        }
        if (bookingDto.getStatus().equals(Status.APPROVED)) {
            throw new ValidationException("Бронирование уже подтверждено");
        }
        if (approved == null) {
            throw new ValidationException("Необходимо подтвердить бронирование");
        } else if (approved) {
            bookingDto.setStatus(Status.APPROVED);
        } else {
            bookingDto.setStatus(Status.REJECTED);
        }
        return BookingMapper.toBookingDto(bookingRepository.save(BookingMapper.toBooking(bookingDto)));
    }
}
