package ru.practicum.shareit.bookingTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingDtoSimple;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.booking.service.BookingServiceImpl;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static ru.practicum.shareit.booking.enums.Status.*;

@ExtendWith(MockitoExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BookingServiceTest {
    private BookingService bookingService;
    private BookingRepository bookingRepository;
    private ItemRepository itemRepository;
    private UserRepository userRepository;
    private Booking booking;


    @BeforeEach
    void beforeEach() {
        itemRepository = mock(ItemRepository.class);
        userRepository = mock(UserRepository.class);
        bookingRepository = mock(BookingRepository.class);
        bookingService = new BookingServiceImpl(bookingRepository, itemRepository, userRepository);
        booking = createBookingExample();
    }

    private Booking createBookingExample() {
        User owner = new User(1L, "testOwner", "testOwner@yandex.ru");
        User booker = new User(2L, "testBooker", "testBooker@yandex.ru");
        Item item = new Item(1L, "testItem", "testDescription", true, owner, null);

        LocalDateTime start = LocalDateTime.parse("2022-09-10T10:42");
        LocalDateTime end = LocalDateTime.parse("2022-09-12T10:42");
        return new Booking(1L, start, end, item, booker, APPROVED);
    }

    private static BookingDtoSimple toBookingDtoSimple(Booking booking) {
        return new BookingDtoSimple(booking.getId(), booking.getStart(), booking.getEnd(), booking.getItem().getId());
    }

    //Создание брони
    @Test
    public void createValidBooking() {
        Long bookerId = booking.getBooker().getId();
        User booker = booking.getBooker();
        Long itemId = booking.getItem().getId();
        Item item = booking.getItem();

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingDto bookingDto = bookingService.create(toBookingDtoSimple(booking), bookerId);

        assertEquals(booking.getId(), bookingDto.getId(), "Идентификаторы не совпадают");
        assertEquals(booking.getStart(), bookingDto.getStart(), "Время начала не совпадает");
        assertEquals(booking.getEnd(), bookingDto.getEnd(), "Время конца не совпадает");
        assertEquals(booking.getBooker().getName(), bookingDto.getBooker().getName(), "Имена не совпадают");
        assertEquals(booking.getStatus(), bookingDto.getStatus(), "Статусы не совпадают");

        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    //Получение брони
    @Test
    void getBooking() {
        Long bookerId = booking.getBooker().getId();
        Long bookingId = booking.getId();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingDto bookingDto = bookingService.getBooking(bookingId, bookerId);

        assertEquals(booking.getId(), bookingDto.getId(), "Идентификаторы не совпадают");
        assertEquals(booking.getStart(), bookingDto.getStart(), "Время начала не совпадает");
        assertEquals(booking.getEnd(), bookingDto.getEnd(), "Время конца не совпадает");
        assertEquals(booking.getBooker().getName(), bookingDto.getBooker().getName(), "Имена не совпадают");
        assertEquals(booking.getStatus(), bookingDto.getStatus(), "Статусы не совпадают");

        verify(bookingRepository, times(1)).findById(bookingId);

    }

    //Получение всех броней
    @Test
    void getAllBookings() {
        Long bookerId = booking.getBooker().getId();
        User booker = booking.getBooker();

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerId(bookerId, PageRequest.of(0, 20, Sort.by("start")
                .descending()))).thenReturn(Collections.singletonList(booking));

        final List<BookingDto> bookingDtoList = bookingService.getAll(bookerId, "ALL", 0, 20);

        assertEquals(bookingDtoList.size(), 1, "Бронь отсутствует");
        assertEquals(booking.getId(), bookingDtoList.get(0).getId(), "Идентификаторы не совпадают");
        assertEquals(booking.getStart(), bookingDtoList.get(0).getStart(), "Время начала не совпадает");
        assertEquals(booking.getEnd(), bookingDtoList.get(0).getEnd(), "Время конца не совпадает");
        assertEquals(booking.getBooker().getName(), bookingDtoList.get(0).getBooker().getName(),
                "Имена не совпадают");
        assertEquals(booking.getStatus(), bookingDtoList.get(0).getStatus(), "Статусы не совпадают");

        verify(bookingRepository, times(1)).findByBookerId(bookerId,
                PageRequest.of(0, 20, Sort.by("start").descending()));
    }

    //Получение всех броней со статусом WAITING
    @Test
    void getAllBookingWaiting() {
        Long bookerId = booking.getBooker().getId();
        User booker = booking.getBooker();

        booking.setStatus(WAITING);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(bookingRepository.findBookingsByBookerIdAndStatus(bookerId, WAITING,
                PageRequest.of(0, 20, Sort.by("start").descending())))
                .thenReturn(Collections.singletonList(booking));

        List<BookingDto> bookingDtoList = bookingService.getAll(bookerId, "WAITING", 0, 20);

        assertEquals(bookingDtoList.size(), 1, "Бронь отсутствует");
        assertEquals(booking.getId(), bookingDtoList.get(0).getId(), "Идентификаторы не совпадают");
        assertEquals(booking.getStart(), bookingDtoList.get(0).getStart(), "Время начала не совпадает");
        assertEquals(booking.getEnd(), bookingDtoList.get(0).getEnd(), "Время конца не совпадает");
        assertEquals(booking.getBooker().getName(), bookingDtoList.get(0).getBooker().getName(),
                "Имена не совпадают");

        verify(bookingRepository, times(1)).findBookingsByBookerIdAndStatus(bookerId, WAITING,
                PageRequest.of(0, 20, Sort.by("start").descending()));
    }

    //Получение всех броней со статусом REJECTED
    @Test
    void getAllBookingsRejected() {
        Long bookerId = booking.getBooker().getId();
        User booker = booking.getBooker();

        booking.setStatus(REJECTED);

        when(userRepository.findById(bookerId)).thenReturn(Optional.of(booker));
        when(bookingRepository.findBookingsByBookerIdAndStatus(bookerId, REJECTED,
                PageRequest.of(0, 20, Sort.by("start").descending())))
                .thenReturn(Collections.singletonList(booking));

        List<BookingDto> bookingDtoList = bookingService.getAll(bookerId, "REJECTED", 0, 20);

        assertEquals(bookingDtoList.size(), 1, "Бронь отсутствует");
        assertEquals(booking.getId(), bookingDtoList.get(0).getId(), "Идентификаторы не совпадают");
        assertEquals(booking.getStart(), bookingDtoList.get(0).getStart(), "Время начала не совпадает");
        assertEquals(booking.getEnd(), bookingDtoList.get(0).getEnd(), "Время конца не совпадает");
        assertEquals(booking.getBooker().getName(), bookingDtoList.get(0).getBooker().getName(),
                "Имена не совпадают");

        verify(bookingRepository, times(1)).findBookingsByBookerIdAndStatus(bookerId, REJECTED,
                PageRequest.of(0, 20, Sort.by("start").descending()));
    }

    //Получение всех бронирований пользователя
    @Test
    void getAllUserBookings() {
        User booker = booking.getBooker();
        Long itemUserId = booking.getItem().getOwner().getId();

        when(userRepository.findById(itemUserId)).thenReturn(Optional.of(booker));
        when(bookingRepository.searchBookingByItemOwnerId(itemUserId,
                PageRequest.of(0, 20, Sort.by("start").descending())))
                .thenReturn(Collections.singletonList(booking));

        List<BookingDto> bookingDtoList = bookingService.getAllBookingByOwner(itemUserId, "ALL", 0, 20);


        assertEquals(bookingDtoList.size(), 1, "Бронь отсутствует");
        assertEquals(booking.getId(), bookingDtoList.get(0).getId(), "Идентификаторы не совпадают");
        assertEquals(booking.getStart(), bookingDtoList.get(0).getStart(), "Время начала не совпадает");
        assertEquals(booking.getEnd(), bookingDtoList.get(0).getEnd(), "Время конца не совпадает");
        assertEquals(booking.getBooker().getName(), bookingDtoList.get(0).getBooker().getName(),
                "Имена не совпадают");

        verify(bookingRepository, times(1)).searchBookingByItemOwnerId(itemUserId,
                PageRequest.of(0, 20, Sort.by("start").descending()));
    }

    //Подтверждение подтвержденной брони
    @Test
    public void approveBookingApprovedTest() {
        Long bookingId = booking.getId();
        Long itemUserId = booking.getItem().getOwner().getId();

        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        Throwable throwable = assertThrows(ValidationException.class, () -> bookingService.approve(itemUserId,
                bookingId, true));
        assertNotNull(throwable.getMessage());

        assertEquals("Бронирование уже подтверждено", throwable.getMessage(),
                "Текст ошибки не совпадает");

        verify(bookingRepository, times(1)).findById(bookingId);
    }
}
