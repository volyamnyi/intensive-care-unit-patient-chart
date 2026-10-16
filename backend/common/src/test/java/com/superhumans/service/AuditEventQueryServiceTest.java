package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Pagination contract of the audit console query service (#397 item 6). */
class AuditEventQueryServiceTest {

    @Test
    void oversizedPage_clampedToMax() {
        Pageable result = AuditEventQueryService.stablePageable(PageRequest.of(2, 100000));

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(AuditEventQueryService.MAX_PAGE_SIZE);
    }

    @Test
    void normalPage_preserved() {
        Pageable result = AuditEventQueryService.stablePageable(PageRequest.of(0, 20));

        assertThat(result.getPageNumber()).isZero();
        assertThat(result.getPageSize()).isEqualTo(20);
    }

    @Test
    void unpagedRequest_defaultsToFirstPage() {
        Pageable result = AuditEventQueryService.stablePageable(Pageable.unpaged());

        assertThat(result.getPageNumber()).isZero();
        assertThat(result.getPageSize()).isEqualTo(20);
    }
}
