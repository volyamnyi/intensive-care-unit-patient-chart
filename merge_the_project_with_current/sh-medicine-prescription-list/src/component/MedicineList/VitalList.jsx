import { nanoid } from "nanoid";
import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";

import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faArrowLeft } from "@fortawesome/free-solid-svg-icons";
import { faArrowRight } from "@fortawesome/free-solid-svg-icons";
import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { formatDateToISO } from "../../utils/Functions";
import { handleVitalChange } from "../../utils/Functions";
import { handleAddNewDayDetails } from "../../utils/Functions";
import { handleDelNewDayDetails } from "../../utils/Functions";

export default function VitalList({
  isNew,
  isCopy,
  patientId,
  //handleItemChange,
  handleDetailChange,
  handleRemoveMedicineItem,
  handleMedicineMethodChange,
  handleSubmit,
  handleSearchedMedicineClick,
  handleCurrentRowClick,
  medicineList,
  medicineDetails,
  currentRowSuggestionIndex,
  ROLE,
  setMedicineListItem,
  setSearchedMedicine,
  isEmpty,
  setMedicineList,
  setTriggerSubmit,
  setCurrentRowSuggestionIndex,
  setMedicineName,
  searchMedicine,
  setTriggerSearchedMedicine,
  setShowSuggestions,
  showSuggestions,
  triggerSearchedMedicine,
  searchedMedicine,
  isScaled,
  handleMedicineRegimeChange,
  vitalList,
  setVitalList,
}) {
  const [startDate, setStartDate] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [endDate, setEndDate] = useState(() => {
    const today = new Date();
    const futureDate = new Date();
    futureDate.setDate(today.getDate() + 20);
    return futureDate.toISOString().split("T")[0];
  });

  const [dateRange, setDateRange] = useState([]);

  const [enableAddNewMedicine, setEnableAddNewMedicine] = useState(false);
  const [showDatePicker, setShowDatePicker] = useState(true);
  const navigate = useNavigate();

  const getDatesInRange = (start, end) => {
    const dates = [];
    let current = new Date(start);
    const endDate = new Date(end);

    const diffInTime = endDate - current;
    const diffInDays = diffInTime / (1000 * 60 * 60 * 24);

    while (current <= endDate) {
      dates.push(new Date(current).toISOString().split("T")[0]);
      current.setDate(current.getDate() + 1);
    }

    return dates;
  };

  const months = {
    "01": "Січ",
    "02": "Лют",
    "03": "Бер",
    "04": "Кві",
    "05": "Тра",
    "06": "Чер",
    "07": "Лип",
    "08": "Сер",
    "09": "Вер",
    10: "Жов",
    11: "Лис",
    12: "Гру",
  };

  const handleRangeChange = (range) => {
    setEnableAddNewMedicine(true);
  };

  function getDatePicker2() {
    return (
      <div>
        <label>
          Від:&nbsp;&nbsp;
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="border p-1 rounded"
          />
        </label>
        <label>
          &nbsp;&nbsp;До:&nbsp;&nbsp;
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="border p-1 rounded"
          />
        </label>
        <br></br>
      </div>
    );
  }

  useEffect(() => {
    if (startDate && endDate && new Date(startDate) <= new Date(endDate)) {
      const range = getDatesInRange(startDate, endDate);
      setDateRange(range);
      //handleRangeChange(range);
    } else {
      setDateRange([]);
      //handleRangeChange([]);
    }
  }, [startDate, endDate]);

  const vitalListInitializedRef = useRef(false);
  useEffect(() => {
    setTimeout(() => {
      if (isNew) {
        if (vitalListInitializedRef.current || !startDate || !endDate) {
          return;
        }

        vitalListInitializedRef.current = true;

        const dates = getDatesInRange(startDate, endDate);

        setVitalList({
          vitalList: dates.map((date) => ({
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
          })),
        });
      }
    }, 500);
  }, [startDate, endDate]);

  useEffect(() => {
    setMedicineList((prevValues) => ({ ...prevValues, vitalList }));
  }, [vitalList]);

  return (
    <form
      className="flex"
        onSubmit={(e) =>
          handleSubmit(
            e,
            setMedicineList,
            setTriggerSubmit,
            medicineDetails,
            `/patientdetails/${patientId}`,
            navigate,
          )
        }
      
    >
      <div className="medicine-container">
        <div className="medicine-regime-container">
          {
            <div className="date-picker-container">
              {!isScaled && getDatePicker2()}
              {!isScaled && (
                <div className="arrows-container">
                  <FontAwesomeIcon
                    icon={faArrowLeft}
                    onClick={() => {
                      setStartDate((prevValue) => {
                        const newDate = new Date(prevValue);
                        newDate.setDate(newDate.getDate() - 1);
                        return formatDateToISO(newDate);
                      });
                      setEndDate((prevValue) => {
                        const newDate = new Date(prevValue);
                        newDate.setDate(newDate.getDate() - 1);
                        return formatDateToISO(newDate);
                      });
                    }}
                  />
                  <FontAwesomeIcon
                    icon={faArrowRight}
                    onClick={() => {
                      setStartDate((prevValue) => {
                        const newDate = new Date(prevValue);
                        newDate.setDate(newDate.getDate() + 1);
                        return formatDateToISO(newDate);
                      });
                      setEndDate((prevValue) => {
                        const newDate = new Date(prevValue);
                        newDate.setDate(newDate.getDate() + 1);
                        return formatDateToISO(newDate);
                      });
                    }}
                  />
                </div>
              )}
            </div>
          }
        </div>
        <div className="medicine-row-container">
          <div className="medicine-method-column">Метод</div>
          {vitalList?.vitalList?.map((vl, j) => {
            return (
              <div
                className="medicine-presc-details-row "
                style={{ display: "inline-block" }}
              >
                {vl.date >= startDate && vl.date <= endDate && (
                  <div className="medicine-presc-details-row-first-div">
                    {(() => {
                      try {
                        return (
                          <div
                            style={{
                              fontWeight: "bold",
                              position: "relative",
                              textAlign: "center",
                              border: "1px solid black",
                            }}
                          >
                            {`${vl?.date?.split("-")[2] || "??"} ${
                              months[vl?.date?.split("-")[1]] || "??"
                            } ${vl.date?.split("-")[0] || "??"}`}

                            <div
                              className="day-period"
                              style={{
                                display: "flex",
                                gap: 0,
                                marginTop: 6,
                              }}
                            >
                              <div style={{ cursor: "pointer" }}>Р</div>
                              <div style={{ cursor: "pointer" }}> </div>
                              <div style={{ cursor: "pointer" }}>В</div>
                              <div style={{ cursor: "pointer" }}> </div>
                            </div>
                          </div>
                        );
                      } catch (error) {
                        console.error("Error rendering date:", error);
                        return (
                          <div
                            className="medicine-date"
                            key={""}
                            style={{ fontWeight: "bold", color: "red" }}
                          >
                            Не вірна дата
                          </div>
                        );
                      }
                    })()}
                    <div
                      key={"11241241441"}
                      className="medicine-presc-details-column"
                    >
                      <div className="day-cells-container">
                        {["morning", /*"day",*/ "evening" /*, "night"*/].map(
                          (period) => (
                            <div key={period} className="day-cell">
                              {
                                <textarea
                                  style={{ fontWeight: "bold" }}
                                  type="text"
                                  name="temperature"
                                  value={vl[period]?.temperature}
                                  disabled={isScaled}
                                  placeholder="t"
                                  onChange={(e) => {
                                    !isScaled &&
                                      handleVitalChange(
                                        e,
                                        vl.id,
                                        period,
                                        "temperature",
                                        setVitalList,
                                      );
                                  }}
                                  className="day-cell-dose-input"
                                ></textarea>
                              }
                              {
                                <input
                                  style={{ fontWeight: "bold" }}
                                  type="text"
                                  name="saturation"
                                  value={vl[period]?.saturation}
                                  disabled={isScaled}
                                  placeholder="SpO2"
                                  onChange={(e) => {
                                    !isScaled &&
                                      handleVitalChange(
                                        e,
                                        vl.id,
                                        period,
                                        "saturation",
                                        setVitalList,
                                      );
                                  }}
                                  className="day-cell-dose-input"
                                ></input>
                              }
                              {
                                <input
                                  style={{ fontWeight: "bold" }}
                                  type="text"
                                  name="poop"
                                  value={vl[period]?.poop}
                                  disabled={isScaled}
                                  placeholder="Кал"
                                  onChange={(e) => {
                                    !isScaled &&
                                      handleVitalChange(
                                        e,
                                        vl.id,
                                        period,
                                        "poop",
                                        setVitalList,
                                      );
                                  }}
                                  className="day-cell-dose-input"
                                ></input>
                              }
                              {
                                <input
                                  style={{ fontWeight: "bold" }}
                                  type="text"
                                  name="pain"
                                  value={vl[period]?.pain}
                                  disabled={isScaled}
                                  placeholder="Шб"
                                  onChange={(e) => {
                                    !isScaled &&
                                      handleVitalChange(
                                        e,
                                        vl.id,
                                        period,
                                        "pain",
                                        setVitalList,
                                      );
                                  }}
                                  className="day-cell-dose-input"
                                ></input>
                              }
                              {
                                <input
                                  style={{ fontWeight: "bold" }}
                                  type="text"
                                  name="bloodPressure"
                                  value={vl[period]?.bloodPressure}
                                  disabled={isScaled}
                                  placeholder="АТ"
                                  onChange={(e) => {
                                    !isScaled &&
                                      handleVitalChange(
                                        e,
                                        vl.id,
                                        period,
                                        "bloodPressure",
                                        setVitalList,
                                      );
                                  }}
                                  className="day-cell-dose-input"
                                ></input>
                              }
                              {
                                <input
                                  style={{ fontWeight: "bold" }}
                                  type="text"
                                  name="pulse"
                                  value={vl[period]?.pulse}
                                  disabled={isScaled}
                                  placeholder="PS"
                                  onChange={(e) => {
                                    !isScaled &&
                                      handleVitalChange(
                                        e,
                                        vl.id,
                                        period,
                                        "pulse",
                                        setVitalList,
                                      );
                                  }}
                                  className="day-cell-dose-input"
                                ></input>
                              }
                              {vitalList.vitalList.length - 1 == j &&
                                !isScaled && (
                                  <button
                                    className="plus"
                                    type="button"
                                    style={{ top: "10px" }}
                                    onClick={() => {
                                      const date = new Date(
                                        vitalList?.vitalList[j].date,
                                      );

                                      const lastDate = new Date(date);

                                      lastDate.setDate(
                                        date.getDate() +
                                          parseInt(
                                            prompt(
                                              "На скільки днів збільшити?",
                                            ),
                                          ),
                                      );

                                      date.setDate(date.getDate() + 1);

                                      const datesRange = getDatesInRange(
                                        date,
                                        lastDate,
                                      );

                                      if (
                                        datesRange.length +
                                          vitalList?.vitalList.length >
                                        90
                                      ) {
                                        alert(
                                          "Загальна кількість днів для одного призначення не може перевищувати 90",
                                        );
                                        return;
                                      }

                                      handleAddNewDayDetails(
                                        vitalList,
                                        0,
                                        setVitalList,
                                        datesRange,
                                        true,
                                      );

                                      endDate2 <= formatDateToISO(lastDate) &&
                                        setEndDate2(formatDateToISO(lastDate));
                                    }}
                                  >
                                    +
                                  </button>
                                )}

                              {vitalList.vitalList.length - 1 == j &&
                                vitalList.vitalList.length > 1 &&
                                !isScaled && (
                                  <button
                                    className="plus danger"
                                    type="button"
                                    onClick={() =>
                                      handleDelNewDayDetails(
                                        vitalList,
                                        0,
                                        j,
                                        setVitalList,
                                        true,
                                      )
                                    }
                                  >
                                    -
                                  </button>
                                )}
                            </div>
                          ),
                        )}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
      {!isScaled && (
        <div className="save-generate-buttons-container">
          <button
            className="save-button"
            
          >
            Зберегти
          </button>
        </div>
      )}
    </form>
  );
}
