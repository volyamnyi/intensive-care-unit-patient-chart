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
public class Day {

    String id;

    String date;

    @JsonProperty("morning")
    DayPart morning;

    @JsonProperty("day")
    DayPart day;

    @JsonProperty("evening")
    DayPart evening;

    @JsonProperty("night")
    DayPart night;

    @Override
    public String toString() {
        return "Day{" +
                "id=" + id +
                ", date=" + date +
                ", morning=" + morning +
                ", day=" + day +
                ", evening=" + evening +
                ", night=" + night +
                '}';
    }
}
