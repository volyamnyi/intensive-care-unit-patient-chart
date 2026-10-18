package com.superhumans.prosthesismanufacturing.dto;

import java.util.List;

/** Bounded instance-list page; metadata is derived from the filtered DB query. */
public record FlowInstancePageResponse(
        List<FlowInstanceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
