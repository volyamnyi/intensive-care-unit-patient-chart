package com.superhumans.service;

import com.superhumans.entity.ClinicalNote;
import com.superhumans.entity.IcuDay;
import com.superhumans.repository.ClinicalNoteRepository;
import com.superhumans.repository.IcuDayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalNoteServiceTest {

    @Mock private ClinicalNoteRepository clinicalNoteRepository;
    @Mock private IcuDayRepository icuDayRepository;
    @InjectMocks private ClinicalNoteService clinicalNoteService;

    @Test
    void addNote_shouldCreateNote() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(clinicalNoteRepository.save(any())).thenAnswer(i -> {
            ClinicalNote n = i.getArgument(0);
            n.setId(1L);
            return n;
        });

        ClinicalNote result = clinicalNoteService.addNote(1L, "Test note content", "GENERAL", "doctor1");
        assertNotNull(result);
        assertEquals("Test note content", result.getContent());
        assertEquals("doctor1", result.getCreatedBy());
        assertNotNull(result.getCreatedAt());
    }

    @Test
    void addNote_shouldThrow_whenDayNotFound() {
        when(icuDayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
                clinicalNoteService.addNote(999L, "content", "GENERAL", "doctor1"));
    }

    @Test
    void getNotesByDay_shouldReturnOrderedDesc() {
        when(clinicalNoteRepository.findByIcuDayIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                ClinicalNote.builder().id(1L).content("Note 1").createdAt(LocalDateTime.now()).build()
        ));
        List<ClinicalNote> notes = clinicalNoteService.getNotesByDay(1L);
        assertEquals(1, notes.size());
        assertEquals("Note 1", notes.get(0).getContent());
    }

    @Test
    void getNotesByDay_shouldReturnEmptyList_whenNoNotes() {
        when(clinicalNoteRepository.findByIcuDayIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        assertTrue(clinicalNoteService.getNotesByDay(1L).isEmpty());
    }
}
