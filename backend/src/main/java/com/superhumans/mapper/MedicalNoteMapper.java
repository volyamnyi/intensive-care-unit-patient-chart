package com.superhumans.mapper;

import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.entity.MedicalNote;

public class MedicalNoteMapper {

    public static MedicalNoteResponse toResponse(MedicalNote entity) {
        return MedicalNoteResponse.builder()
                .id(entity.getId())
                .clinicalDayId(entity.getClinicalDay().getId())
                .authorId(entity.getAuthorId())
                .role(entity.getRole())
                .noteType(entity.getNoteType())
                .text(entity.getText())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}
