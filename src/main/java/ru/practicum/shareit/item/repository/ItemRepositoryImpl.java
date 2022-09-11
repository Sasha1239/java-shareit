package ru.practicum.shareit.item.repository;

import org.springframework.stereotype.Component;
import ru.practicum.shareit.item.model.Item;

import java.util.*;

@Component
public class ItemRepositoryImpl implements ItemRepository {
    private final Map<Long, Item> itemList = new HashMap<>();
    private Long itemId = 0L;

    //Создание вещи
    @Override
    public Item create(Item item) {
        item.setId(++itemId);
        itemList.put(item.getId(), item);
        return item;
    }

    //Получение вещи
    @Override
    public Optional<Item> getItem(Long itemId) {
        return Optional.ofNullable(itemList.get(itemId));
    }

    //Обновление вещи
    @Override
    public Optional<Item> update(Item item) {
        itemList.put(item.getId(), item);
        return Optional.of(item);
    }

    //Получение всех вещей
    @Override
    public List<Item> getAll() {
        return new ArrayList<>(itemList.values());
    }
}