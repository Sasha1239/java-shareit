package ru.practicum.shareit.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemDto {
    private Long id;
    //@NotBlank(message = "Имя вещи не может быть пустым")
    private String name;
    //@NotBlank(message = "Описание вещи не может быть пустым")
    private String description;
    //@NotNull(message = "Статус не может быть null")
    private Boolean available;
    private Long requestId;
}