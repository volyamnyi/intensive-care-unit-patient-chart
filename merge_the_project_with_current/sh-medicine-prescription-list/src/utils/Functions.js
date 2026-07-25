import { nanoid } from "nanoid";
import { jwtDecode } from "jwt-decode";

export const isTokenExpired = (token) => {

  if (!token) return true;

  try {

    const decodedToken = jwtDecode(token);

    const currentTime = Math.floor(Date.now() / 1000);

    return decodedToken.exp < currentTime;
  } catch (error) {

    return true;
  }
};

export function handleAddNewDayDetails(
  medicineDetails,
  rowIndex,
  setMedicineListItem,
  dates,
  isVital = false,
) {

  if (!isVital) {

    for (let i = 0; i < dates.length; i++)
      medicineDetails[rowIndex]?.medicineDetails?.push({
        id: nanoid(),
        date: dates[i],

        morning: {
          id: nanoid(),
          time: "",
          medicineDose: "",
          //pain: "",

          isPlanned: false,

          isPlannedAndFinished: false,

          isCompleted: false,
          //isCompletedAndFinished: false,

          doctorName: "",

          nurseName: "",
        },
        day: {
          id: nanoid(),
          time: "",
          medicineDose: "",
          //pain: "",
          isPlanned: false,
          isPlannedAndFinished: false,
          isCompleted: false,
          //isCompletedAndFinished: false,
          doctorName: "",
          nurseName: "",
        },
        evening: {
          id: nanoid(),
          time: "",
          medicineDose: "",
          //pain: "",
          isPlanned: false,
          isPlannedAndFinished: false,
          isCompleted: false,
          //isCompletedAndFinished: false,
          doctorName: "",
          nurseName: "",
        },
        night: {
          id: nanoid(),
          time: "",
          medicineDose: "",
          //pain: "",
          isPlanned: false,
          isPlannedAndFinished: false,
          isCompleted: false,
          //isCompletedAndFinished: false,
          doctorName: "",
          nurseName: "",
        },
      });

    setMedicineListItem([...medicineDetails]);
  } else {

    const newVitalList = dates.map((date) => ({
      id: nanoid(),
      date,
      morning: {
        id: nanoid(),
        temperature: "",
        bloodPressure: "",
        saturation: "",
        pulse: "",
        poop: "",
        pain: "",
      },
      evening: {
        id: nanoid(),
        temperature: "",
        bloodPressure: "",
        saturation: "",
        pulse: "",
        poop: "",
        pain: "",
      },
    }));

    setMedicineListItem((prev) => ({
      ...prev,
      vitalList: [...(prev.vitalList ?? []), ...newVitalList],
    }));
  }
}

export function handleAutofill(
  existingDay,
  medicineDetails,
  rowIndex,
  setMedicineListItem,
  dates,
  sub,
  autofillStartDate,
  autofillEndDate
) {

  if (!existingDay?.[0]?.medicineDetails?.length) return;

  if (!medicineDetails?.[rowIndex]?.medicineDetails?.length) return;

  if (!autofillStartDate || !autofillEndDate) return;

  const template = existingDay[0].medicineDetails[0];

  const dayDetails = medicineDetails[rowIndex].medicineDetails;

  const startIndex = dates.findIndex(d => d >= autofillStartDate);
  const endIndex = dates.findIndex(d => d > autofillEndDate);

  if (startIndex === -1) return;

  const finalEndIndex = endIndex === -1 ? dates.length : endIndex;

  for (let k = startIndex; k < finalEndIndex; k++) {
    const day = dayDetails[k];
    if (!day) continue;

    day.date = dates[k];

    day.morning = {
      ...day.morning,
      id: nanoid(),
      time: template.morning.time,
      medicineDose: template.morning.medicineDose,
      isPlanned: template.morning.isPlanned,

      isPlannedAndFinished: false,
      isCompleted: false,
      doctorName: sub,
      nurseName: '',
    };

    day.day = {
      ...day.day,
      id: nanoid(),
      time: template.day.time,
      medicineDose: template.day.medicineDose,
      isPlanned: template.day.isPlanned,
      isPlannedAndFinished: false,
      isCompleted: false,
      doctorName: sub,
      nurseName: '',
    };

    day.evening = {
      ...day.evening,
      id: nanoid(),
      time: template.evening.time,
      medicineDose: template.evening.medicineDose,
      isPlanned: template.evening.isPlanned,
      isPlannedAndFinished: false,
      isCompleted: false,
      doctorName: sub,
      nurseName: '',
    };

    day.night = {
      ...day.night,
      id: nanoid(),
      time: template.night.time,
      medicineDose: template.night.medicineDose,
      isPlanned: template.night.isPlanned,
      isPlannedAndFinished: false,
      isCompleted: false,
      doctorName: sub,
      nurseName: '',
    };
  }


  setMedicineListItem([...medicineDetails]);
}

export function handleDelNewDayDetails(
  medicineDetails,
  rowIndex,
  colIndex,
  setMedicineListItem,
  isVital = false,
) {

  const count = parseInt(prompt("На скільки днів зменшити?"));

  if (medicineDetails[rowIndex]?.medicineDetails?.length < count + 1) {
    alert("Завелика кількість днів для видалення");
    return;
  }

  if (!isVital) {

    for (let i = 0; i < count; i++) {
      medicineDetails[rowIndex]?.medicineDetails?.pop();

      setMedicineListItem([...medicineDetails]);
    }
  } else {

    for (let i = 0; i < count; i++) {
      medicineDetails?.vitalList?.pop();
    }

     /**
     * Оновлення state для rerender компонента.
     */
    setMedicineListItem({ ...medicineDetails });
  }

}

export function handleAddNewMedicineItem(
  medicineList,
  setMedicineListItem,
  dates,
  setShowRegime,
) {

  setShowRegime(true);

  setMedicineListItem((prevValue) => [
    ...prevValue,

    {

      id: nanoid(),

      medicineListItemEditUser: medicineList.medicineListCreationUser,

      medicineListItemEditDate: new Date(),

      medicineName: "",

      regime: "",

      medicineMethod: "",
      duration: "",
      medicineDetails: [
        {
          id: nanoid(),
          date: dates[0],
          morning: {
            id: nanoid(),
            time: "",
            medicineDose: "",
            //pain: "",
            isPlanned: false,
            isPlannedAndFinished: false,
            isCompleted: false,
            //isCompletedAndFinished: false,
            doctorName: "",
            nurseName: "",
          },
          day: {
            id: nanoid(),
            time: "",
            medicineDose: "",
            //pain: "",
            isPlanned: false,
            isPlannedAndFinished: false,
            isCompleted: false,
            // isCompletedAndFinished: false,
            doctorName: "",
            nurseName: "",
          },
          evening: {
            id: nanoid(),
            time: "",
            medicineDose: "",
            //pain: "",
            isPlanned: false,
            isPlannedAndFinished: false,
            isCompleted: false,
            // isCompletedAndFinished: false,
            doctorName: "",
            nurseName: "",
          },
          night: {
            id: nanoid(),
            time: "",
            medicineDose: "",
            //pain: "",
            isPlanned: false,
            isPlannedAndFinished: false,
            isCompleted: false,
            // isCompletedAndFinished: false,
            doctorName: "",
            nurseName: "",
          },
        },
      ],
    },
  ]);
}

export function refreshDates(medicineList, setMedicineListItem, dates) {
  const medicineDetails = medicineList.medicineDetails;
  medicineDetails.forEach((md1) =>
    md1.medicineDetails.forEach((md2, i) => (md2.date = dates[i])),
  );
  medicineList.medicineDetails = medicineDetails;

  setMedicineListItem(medicineDetails);
}

export function handleAddNewMedicineItem2(
  i,
  medicineDetails,
  setMedicineList,
  setMedicineListItem,
  dates,
  sub,
  setShowRegime
) {
  setShowRegime(true);
  setMedicineList((prev) => {
    const approvedRowIndexes = prev.approvedRowIndexes ?? [];
    //if (/*sub == prev.medicineListCreationUser ||*/ sub != 's.borovska' && sub !='r.lishchuk') {
    approvedRowIndexes.push(i.toString());
    //}
    return { ...prev, approvedRowIndexes };
  });
  const newMedicineDetails = [];

  for (let i = 0; i < dates.length; i++) {
    newMedicineDetails.push({
      id: nanoid(),
      date: dates[i],
      morning: {
        id: nanoid(),
        time: "",
        medicineDose: "",
        //pain: "",
        isPlanned: false,
        isPlannedAndFinished: false,
        isCompleted: false,
        //isCompletedAndFinished: false,
        doctorName: "",
        nurseName: "",
      },
      day: {
        id: nanoid(),
        time: "",
        medicineDose: "",
        //pain: "",
        isPlanned: false,
        isPlannedAndFinished: false,
        isCompleted: false,
        //isCompletedAndFinished: false,
        doctorName: "",
        nurseName: "",
      },
      evening: {
        id: nanoid(),
        time: "",
        medicineDose: "",
        //pain: "",
        isPlanned: false,
        isPlannedAndFinished: false,
        isCompleted: false,
        //isCompletedAndFinished: false,
        doctorName: "",
        nurseName: "",
      },
      night: {
        id: nanoid(),
        time: "",
        medicineDose: "",
        //pain: "",
        isPlanned: false,
        isPlannedAndFinished: false,
        isCompleted: false,
        //isCompletedAndFinished: false,
        doctorName: "",
        nurseName: "",
      },
    });
  }
  setMedicineListItem((prevValue) => [
    ...prevValue,
    {
      id: nanoid(),
      medicineListItemEditUser: sub,
      medicineListItemEditDate: new Date(),
      medicineName: "",
      regime: "Режим: ",
      medicineMethod: "",
      medicineDetails: newMedicineDetails,
    },
  ]);
}

export function handleAddNewVitalList(
  setMedicineList,
  vitalList,
  setVitalList,
  dates,
) {
  const newVitalList = [];

  for (let i = 0; i < dates.length; i++) {
    newVitalList.push({
      date: dates[i],
      temperature: "",
      bloodPressure: "",
      saturation: "",
      pulse: "",
      poop: "",
      pain: "",
    });
  }
  setVitalList((prevValue) => [...prevValue, newVitalList]);
  setMedicineList((prev) => {
    return { ...prev, vitalList };
  });
}

export function handleRemoveMedicineItem(
  setMedicineList,
  setMedicineListItem,
  index,
) {
  setMedicineList((prev) => {
    let approvedRowIndexes = prev.approvedRowIndexes ?? [];
    //if(localStorage.getItem("sub") == prev.medicineListCreationUser) {
    console.log("Before " + approvedRowIndexes);

    approvedRowIndexes = approvedRowIndexes.filter(
      (i) => i.toString() != index.toString(),
    );

    console.log("After " + approvedRowIndexes);

    //}
    return { ...prev, approvedRowIndexes };
  });

  setMedicineListItem((prevValue) => {
    const newArr = [...prevValue];

    if (index > -1) {
      newArr.splice(index, 1);
    }
    return newArr;
  });
}

export const handleMedicineMethodChange = (
  e,
  medicineId,
  field,
  setMedicineListItem,
) => {
  const { value } = e.target;

  setMedicineListItem((prevList) =>
    prevList.map((medicine) =>
      medicine.id === medicineId ? { ...medicine, [field]: value } : medicine,
    ),
  );
};

export const handleMedicineRegimeChange = (
  e,
  medicineId,
  field,
  setMedicineListItem,
) => {
  const { value } = e.target;

  setMedicineListItem((prevList) =>
    prevList.map((medicine) =>
      medicine.id === medicineId ? { ...medicine, [field]: value } : medicine,
    ),
  );
};

export const handleDetailChange = (
  e,
  medicineId,
  detailsId,
  period,
  field,
  userName,
  userName2P,
  businessRole,
  setMedicineListItem,
) => {
  const { type, checked, value } = e.target? e.target: e;
  const newValue = type === "checkbox" ? checked : value;

  setMedicineListItem((prevList) =>
    prevList.map((medicine) =>
      medicine.id === medicineId
        ? {
            ...medicine,
            medicineDetails: medicine.medicineDetails.map((detail) => {
              if (detail.id === detailsId) {
                if (e._reactName === "onAuxClick") {
                  if (field === "isCompletedAndFinished") {
                    return {
                      ...detail,
                      [period]: {
                        ...detail[period],
                        [field]: !detail[period][field],
                        ["isCompleted"]: !detail[period]["isCompleted"],
                        nurseName: userName2P? userName + '\n' + userName2P : userName,
                      },
                    };
                  } else if (field === "isPlannedAndFinished") {
                    return {
                      ...detail,
                      [period]: {
                        ...detail[period],
                        [field]: !detail[period][field],
                        doctorName: userName,
                      },
                    };
                  }
                } else {
                  if (!detail[period]["isPlannedAndFinished"]) {
                    return {
                      ...detail,
                      [period]: {
                        ...detail[period],
                        [field]: newValue,
                        ...(businessRole === "DOCTOR"
                          ? { doctorName: userName }
                          : { nurseName: userName2P? userName + '\n' + userName2P : userName }),
                      },
                    };
                  } else {
                    return detail;
                  }
                }
              } else {
                return detail;
              }
            }),
          }
        : medicine,
    ),
  );
};

export const handleVitalChange = (e, vitalId, period, field, setVitalList) => {
  const { value } = e.target;

  setVitalList((prev) => {
    if (!prev?.vitalList) return prev;

    return {
      ...prev,
      vitalList: prev.vitalList.map((vital) =>
        vital.id === vitalId
          ? {
              ...vital,
              [period]: {
                ...vital[period],
                [field]: value,
              },
            }
          : vital,
      ),
    };
  });
};

export async function handleSubmit(
  e,
  setMedicineList,
  setTriggerSubmit,
  medicineDetails,
  redirectUrl,
  navigate,
) {
  e.preventDefault();
  setMedicineList((prevValue) => ({
    ...prevValue,
    medicineDetails: medicineDetails,
  }));
  setTriggerSubmit(true);

  setTimeout(() => {
    /*isNew &&*/
    navigate && redirectUrl && navigate(redirectUrl);
  }, 1000);
}

export function handleSearchedMedicineClick(
  e,
  medicineId,
  medicineName,
  setMedicineListItem,
  setShowSuggestions,
  setTriggerSearchedMedicine,
  textareaRef,
) {
  setMedicineListItem((prevList) =>
    prevList.map((medicine) =>
      medicine.id === medicineId
        ? { ...medicine, medicineName: medicineName }
        : medicine,
    ),
  );
  setShowSuggestions(false);
  setTriggerSearchedMedicine(false);
}

export function handleCurrentRowClick(
  e,
  index,
  medicineName,
  setSearchedMedicine,
  setCurrentRowSuggestionIndex,
  setMedicineName,
  setTriggerSearchedMedicine,
) {
  if (isEmpty(medicineName)) {
    setSearchedMedicine([]);
  }
  setCurrentRowSuggestionIndex(index);
  setMedicineName(medicineName);
  setTriggerSearchedMedicine(true);
}

export function getWeekDates(date, daysCount) {
  let dates = [];
  let today = "";
  if (date) {
    today = date;
  } else {
    today = new Date();
  }

  for (let i = 0; i < daysCount; i++) {
    let nextDate = new Date(today);
    nextDate.setDate(today.getDate() + i);
    dates.push(nextDate.toISOString().split("T")[0]);
  }

  return dates;
}

export function getCustomWeekDates(date, daysCount) {
  let dates = [];
  let today = "";
  if (date) {
    today = date;
  } else {
    today = new Date();
  }

  for (let i = 0; i < daysCount; i++) {
    let nextDate = new Date(today);
    nextDate.setDate(today.getDate() + i);
    dates.push(nextDate.toISOString().split("T")[0]);
  }

  return dates;
}

export function formatDate(list) {
  return new Date(
    list.medicineListCreationDate[0],
    list.medicineListCreationDate[1] - 1,
    list.medicineListCreationDate[2],
    list.medicineListCreationDate[3],
    list.medicineListCreationDate[4],
    list.medicineListCreationDate[5],
    Math.floor(list.medicineListCreationDate[6] / 1_000_000),
  );
}

export function formatDate2(list) {
  if (list)
    return new Date(
      list[0],
      list[1] - 1,
      list[2],
      list[3],
      list[4],
      list[5],
      Math.floor(list[6] / 1_000_000),
    );
}

export function formatDateToISO(date) {
  console.log(date);
  return date.toISOString().split("T")[0];
}

export function isoToTimestampSeconds(isoString) {
  return Math.floor(new Date(isoString).getTime() / 1000);
}

export function isLessThanOneHour(timestampInSeconds) {
  const currentTimestampInSeconds = Math.floor(Date.now() / 1000);
  const differenceInSeconds = currentTimestampInSeconds - timestampInSeconds;
  return differenceInSeconds < 3600; // 3600 seconds = 1 hour
}

export function isEmpty(str) {
  return str === null || str.match(/^ *$/) !== null;
}

export function handleApproveMedicine(rowIndex, setMedicineList) {
  setMedicineList((prev) => {
    const list = prev.approvedRowIndexes ?? [];
    const approvedRowIndexes = list.includes(rowIndex.toString())
      ? list.filter((i) => i.toString() != rowIndex.toString())
      : [...list, rowIndex.toString()];

    return { ...prev, approvedRowIndexes };
  });
}
