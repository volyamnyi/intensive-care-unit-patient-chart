package com.superhumans.model.patient;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Patient {

    Integer id;

    String name;

    String historyNumber;

    String address;

    String phone;

    String department;

    String roomNumber;

    String bedNumber;

    String gender;

    String age;

    String birthDate;

    String doctor;

    String doctorUserName;

    List<LocalDateTime> medicineListEditDates = new ArrayList<>();


    public void setGender(String gender) {
        if (gender != null) {
            this.gender = gender.equals("MAL") ? "Чоловіча" : "Жіноча";
        } else {
            this.gender = "Не вказано";
        }
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate.split(" ")[0];
    }

    String residenceStatus;

    @Override
    public String toString() {
        return "Patient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", historyNumber='" + historyNumber + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", department='" + department + '\'' +
                ", roomNumber='" + roomNumber + '\'' +
                ", bedNumber='" + bedNumber + '\'' +
                ", gender='" + gender + '\'' +
                ", age='" + age + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", doctor='" + doctor + '\'' +
                '}';
    }
}
