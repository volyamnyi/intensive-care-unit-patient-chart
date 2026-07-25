package com.superhumans.model.medicinelist;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalList {

    @JsonProperty("vitalList")
    List<VitalListDay> vitalList;

    @Override
    public String toString() {
        return "VitalList{" +
                ", vitalList=" + vitalList +
                '}';
    }
}
