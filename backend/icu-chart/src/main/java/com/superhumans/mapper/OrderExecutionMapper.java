package com.superhumans.mapper;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.entity.OrderExecution;
import com.superhumans.entity.OrderExecutionStatus;
import com.superhumans.entity.OrderExecutionStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = OrderExecutionStatus.class)
public interface OrderExecutionMapper {

    @Mapping(target = "orderId", source = "order.id")
    OrderExecutionResponse toResponse(OrderExecution entity);

    @Mapping(target = "status", expression = "java(OrderExecutionStatus.COMPLETED)")
    OrderExecution toEntity(OrderExecutionCreateRequest request);
}
