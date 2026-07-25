package com.superhumans.model.medicinelist;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalListDayPart {

    String id;

    // Vital fields (default empty string)
    String temperature = "";
    String bloodPressure = "";
    String saturation = "";
    String pulse = "";
    String poop = "";
    String pain = "";

    @Override
    public String toString() {
        return "VitalListDayPart{" +
                "id='" + id + '\'' +
                ", temperature='" + temperature + '\'' +
                ", bloodPressure='" + bloodPressure + '\'' +
                ", saturation='" + saturation + '\'' +
                ", pulse='" + pulse + '\'' +
                ", poop='" + poop + '\'' +
                ", pain='" + pain + '\'' +
                '}';
    }
}
