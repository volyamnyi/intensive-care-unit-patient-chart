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
public class Medicine {

    Integer id;

    String name;

    Integer categoryRef;

    String PTG;

}
