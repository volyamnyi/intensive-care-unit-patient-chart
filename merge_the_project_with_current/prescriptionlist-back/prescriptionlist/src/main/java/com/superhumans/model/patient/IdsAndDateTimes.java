package com.superhumans.model.patient;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IdsAndDateTimes {

    Integer patientId;
    LocalDateTime editDateTime;

    @Override
    public String toString() {
        return "IdsAndDateTimes{" +
                "patientId=" + patientId +
                ", editDateTime=" + editDateTime +
                '}';
    }
}
