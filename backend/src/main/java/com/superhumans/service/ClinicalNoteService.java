package com.superhumans.service;

import com.superhumans.entity.ClinicalNote;
import com.superhumans.entity.IcuDay;
import com.superhumans.repository.ClinicalNoteRepository;
import com.superhumans.repository.IcuDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClinicalNoteService {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final IcuDayRepository icuDayRepository;

    public ClinicalNote addNote(Long dayId, String content, String noteType, String createdBy) {
        IcuDay day = icuDayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("IcuDay not found: " + dayId));
        ClinicalNote note = ClinicalNote.builder()
                .icuDay(day)
                .content(content)
                .noteType(noteType)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();
        return clinicalNoteRepository.save(note);
    }

    public List<ClinicalNote> getNotesByDay(Long dayId) {
        return clinicalNoteRepository.findByIcuDayIdOrderByCreatedAtDesc(dayId);
    }
}
