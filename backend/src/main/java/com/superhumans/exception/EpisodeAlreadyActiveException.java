package com.superhumans.exception;

public class EpisodeAlreadyActiveException extends BusinessException {
    public EpisodeAlreadyActiveException(String message) {
        super(ErrorCode.EPISODE_ALREADY_ACTIVE, message);
    }
}
