package ru.practicum.shareit.itemRequestTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestDtoWithItems;
import ru.practicum.shareit.request.dto.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.request.service.ItemRequestServiceImpl;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ItemRequestServiceTest {
    private ItemRequestService itemRequestService;
    private ItemRequestRepository itemRequestRepository;
    private ItemRequestMapper itemRequestMapper;
    private UserRepository userRepository;
    private ItemRequest itemRequest;

    @BeforeEach
    public void beforeEach() {
        itemRequestRepository = mock(ItemRequestRepository.class);
        userRepository = mock(UserRepository.class);
        ItemRepository itemRepository = mock(ItemRepository.class);
        itemRequestMapper = new ItemRequestMapper(itemRepository);
        itemRequestService = new ItemRequestServiceImpl(itemRequestRepository, itemRequestMapper, userRepository);
        itemRequest = createItemRequestExample();
    }

    private ItemRequest createItemRequestExample() {
        User user = new User(1L, "test", "test@yandex.ru");
        return new ItemRequest(1L, "itemRequestDescription", user, LocalDateTime.now());
    }

    @Test
    void createValidItemRequest() {
        Long itemRequestId = itemRequest.getId();
        Long userId = itemRequest.getRequestor().getId();
        User user = itemRequest.getRequestor();

        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(itemRequest);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ItemRequestDto itemRequestDto = itemRequestService.create(itemRequestMapper.toItemRequestDto(itemRequest),
                userId);

        assertEquals(itemRequestId, itemRequestDto.getId(), "Идентификаторы не совпадают");
        assertEquals(itemRequest.getDescription(), itemRequestDto.getDescription(), "Описания не совпадают");
        assertEquals(itemRequest.getCreated(), itemRequestDto.getCreated(), "Время не совпадает");

        verify(itemRequestRepository, times(1)).save(any(ItemRequest.class));
    }

    //Получение запроса
    @Test
    void getItemRequest() {
        Long itemRequestId = itemRequest.getId();
        Long userId = itemRequest.getRequestor().getId();
        User user = itemRequest.getRequestor();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(itemRequestRepository.findById(itemRequestId)).thenReturn(Optional.of(itemRequest));

        ItemRequestDtoWithItems itemRequestDtoWithItems = itemRequestService.getItemRequest(userId, itemRequestId);

        assertEquals(itemRequestId, itemRequestDtoWithItems.getId(), "Идентификаторы не совпадают");
        assertEquals(itemRequest.getDescription(), itemRequestDtoWithItems.getDescription(),
                "Описания не совпадают");
        assertEquals(itemRequest.getCreated(), itemRequestDtoWithItems.getCreated(), "Время не совпадает");

        verify(itemRequestRepository, times(1)).findById(itemRequestId);
    }

    //Получение всех запросов
    @Test
    void getAllItemRequests() {
        Long itemRequestId = itemRequest.getId();
        Long userId = itemRequest.getRequestor().getId();
        User user = itemRequest.getRequestor();

        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(itemRequest);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(userId))
                .thenReturn(Collections.singletonList(itemRequest));

        itemRequestService.create(itemRequestMapper.toItemRequestDto(itemRequest), userId);

        final List<ItemRequestDtoWithItems> itemRequestDtoWithItems = itemRequestService.getAll(userId);

        assertEquals(itemRequestDtoWithItems.size(), 1, "Запрос отсутствует");
        assertEquals(itemRequestId, itemRequestDtoWithItems.get(0).getId(), "Идентификаторы не совпадают");
        assertEquals(itemRequest.getDescription(), itemRequestDtoWithItems.get(0).getDescription(),
                "Описания не совпадают");
        assertEquals(itemRequest.getCreated(), itemRequestDtoWithItems.get(0).getCreated(),
                "Время не совпадает");

        verify(itemRequestRepository, times(1))
                .findAllByRequestorIdOrderByCreatedDesc(userId);
    }

    //Получение всех запросов с страниц (пустой)
    @Test
    void getAllItemRequestWithPageableEmpty() {
        Long userId = itemRequest.getRequestor().getId();
        User user = itemRequest.getRequestor();

        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(itemRequest);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAll(PageRequest.of(0, 20, Sort.by("created"))))
                .thenReturn(Page.empty());

        itemRequestService.create(itemRequestMapper.toItemRequestDto(itemRequest), userId);

        final List<ItemRequestDtoWithItems> itemRequestDtoWithItems = itemRequestService
                .getAllWithPageable(userId, 0, 20);

        assertTrue(itemRequestDtoWithItems.isEmpty(), "Есть запрос");

        verify(itemRequestRepository, times(1))
                .findAll(PageRequest.of(0, 20, Sort.by("created")));
    }
}
