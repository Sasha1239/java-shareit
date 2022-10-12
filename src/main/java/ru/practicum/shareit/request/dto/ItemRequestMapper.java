package ru.practicum.shareit.request.dto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ItemRequestMapper {
    private final ItemRepository itemRepository;

    public ItemRequestDto toItemRequestDto(ItemRequest itemRequest) {
        return new ItemRequestDto(itemRequest.getId(), itemRequest.getDescription(), itemRequest.getCreated());
    }

    public ItemRequest toItemRequest(ItemRequestDto itemRequestDto) {
        return new ItemRequest(itemRequestDto.getId(), itemRequestDto.getDescription(), null,
                LocalDateTime.now());
    }

    public ItemRequestDtoWithItems toItemRequestDtoWithItems(ItemRequest itemRequest) {
        ItemRequestDtoWithItems itemRequestDtoWithItems = new ItemRequestDtoWithItems(itemRequest.getId(),
                itemRequest.getDescription(), itemRequest.getCreated(), new ArrayList<>()
        );

        List<ItemDto> items = itemRepository.findAllByItemRequestId(itemRequestDtoWithItems.getId())
                .stream().map(ItemMapper::toItemDto).collect(Collectors.toList());

        if (!items.isEmpty()) {
            itemRequestDtoWithItems.setItems(items);
        }

        return itemRequestDtoWithItems;
    }
}
