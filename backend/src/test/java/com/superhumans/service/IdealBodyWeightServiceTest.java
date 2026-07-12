package com.superhumans.service;

import com.superhumans.entity.IcuCard;
import com.superhumans.repository.IcuCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class IdealBodyWeightServiceTest {

    @InjectMocks private IcuCardService icuCardService;

    @Test
    void calculateIdealBodyWeight_male175cm() {
        int ibw = icuCardService.calculateIdealBodyWeight(175, "M");
        assertEquals(70, ibw);
    }

    @Test
    void calculateIdealBodyWeight_female165cm() {
        int ibw = icuCardService.calculateIdealBodyWeight(165, "F");
        assertEquals(57, ibw);
    }

    @Test
    void calculateIdealBodyWeight_male152cm_shouldGive50() {
        int ibw = icuCardService.calculateIdealBodyWeight(152, "M");
        assertEquals(50, ibw);
    }

    @Test
    void calculateIdealBodyWeight_female152cm_shouldGive45dot5() {
        int ibw = icuCardService.calculateIdealBodyWeight(152, "F");
        assertEquals(46, ibw);
    }
}
