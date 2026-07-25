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
public class DayPart {

    String id;
    //LocalTime time;

    String time = "";

    String medicineDose = "";
    String pain = "";

    Boolean isPlanned = false;
    Boolean isPlannedAndFinished = false;

    Boolean isCompleted = false;
    Boolean isCompletedAndFinished = false;

    String doctorName = "";
    String nurseName = "";

    @Override
    public String toString() {
        return "DayPart{" +
                "id='" + id + '\'' +
                ", time='" + time + '\'' +
                ", medicineDose='" + medicineDose + '\'' +
                ", pain='" + pain + '\'' +
                ", isPlanned=" + isPlanned +
                ", isPlannedAndFinished=" + isPlannedAndFinished +
                ", isCompleted=" + isCompleted +
                ", isCompletedAndFinished=" + isCompletedAndFinished +
                ", doctorName='" + doctorName + '\'' +
                ", nurseName='" + nurseName + '\'' +
                '}';
    }
}
