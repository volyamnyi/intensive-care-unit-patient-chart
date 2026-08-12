package com.superhumans.mis.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * MIS service (послуга) from spzIBServiceList / spzIBServiceDetails.
 * Used to populate product/ISO-9999 data in prosthetics order templates.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceMisDTO {
    Long serviceId;
    String serviceName;
    String serviceDesc;
    String serviceCode;
    Long serviceParentId;
    Integer serviceDuration;
    String serviceExternalId;
    Double servicePrice;
}
