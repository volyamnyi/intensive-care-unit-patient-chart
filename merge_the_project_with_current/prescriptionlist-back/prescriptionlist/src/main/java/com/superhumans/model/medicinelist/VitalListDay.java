package com.superhumans.model.medicinelist;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalListDay {


    String id;

    String date;

    @JsonProperty("morning")
    VitalListDayPart morning;

    @JsonProperty("evening")
    VitalListDayPart evening;

    @Override
    public String toString() {
        return "VitalListDay{" +
                "id='" + id + '\'' +
                ", date='" + date + '\'' +
                ", morning=" + morning +
                ", evening=" + evening +
                '}';
    }
}
