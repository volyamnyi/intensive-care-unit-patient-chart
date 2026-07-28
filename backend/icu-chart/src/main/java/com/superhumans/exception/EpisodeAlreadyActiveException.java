package com.superhumans.exception;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EpisodeAlreadyActiveException extends BusinessException {    public EpisodeAlreadyActiveException(String message) {        super(ErrorCode.EPISODE_ALREADY_ACTIVE, message);    }}