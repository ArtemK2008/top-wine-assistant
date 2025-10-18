package ru.topwine.assistant.model.util;

import ru.topwine.assistant.model.enums.UserRequestType;

public record UserRequest(UserRequestType type, String text) {
}