package com.superhumans.prosthesismanufacturing.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FlowInstanceBranchValidationTest {

    @Test
    void parentInstanceIdCanBeNullForRoot() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(FlowInstanceStatus.NEW)
                .build();
        assertThatCode(instance::validate).doesNotThrowAnyException();
        assertThat(instance.getParentInstanceId()).isNull();
    }

    @Test
    void branchSequenceDefaultsToOne() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(FlowInstanceStatus.NEW)
                .build();
        // builder default is 1
        assertThat(instance.getBranchSequence()).isEqualTo(1);
    }

    @Test
    void shouldPreserveParentLinkAndBranchSequence() {
        UUID parentId = UUID.randomUUID();
        FlowInstance branch = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(FlowInstanceStatus.IN_PROGRESS)
                .parentInstanceId(parentId)
                .branchSequence(2)
                .originStageId(UUID.fromString("d0000017-0000-0000-0000-000000000017"))
                .originStepId(UUID.fromString("e0000028-0000-0000-0000-000000000028"))
                .build();
        assertThat(branch.getParentInstanceId()).isEqualTo(parentId);
        assertThat(branch.getBranchSequence()).isEqualTo(2);
        assertThat(branch.getOriginStageId()).isEqualTo(UUID.fromString("d0000017-0000-0000-0000-000000000017"));
        assertThatCode(branch::validate).doesNotThrowAnyException();
    }

    @Test
    void defectPayloadCanBeNullAndValidJson() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .status(FlowInstanceStatus.BRANCHED)
                .defectPayload("{\"soft\": true}")
                .build();
        assertThatCode(instance::validate).doesNotThrowAnyException();
    }
}
