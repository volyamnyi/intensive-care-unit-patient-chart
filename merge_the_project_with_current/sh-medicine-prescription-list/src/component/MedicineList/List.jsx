import { nanoid } from "nanoid";
import { useState, useRef, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { handleAddNewDayDetails } from "../../utils/Functions";
import { handleDelNewDayDetails } from "../../utils/Functions";
import { handleAutofill } from "../../utils/Functions";
import { handleAddNewMedicineItem2 } from "../../utils/Functions";
import { generateDEDoc } from "../../utils/ApiFunctions";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faArrowLeft } from "@fortawesome/free-solid-svg-icons";
import { faArrowRight } from "@fortawesome/free-solid-svg-icons";
import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { formatDateToISO } from "../../utils/Functions";
import SuccessModal from "./SuccessModal";
import { refreshDates } from "../../utils/Functions";
import { handleApproveMedicine } from "../../utils/Functions";
import ListPrintWrapper from "./ListPrintWrapper";
import { handleAddNewVitalList } from "../../utils/Functions";
import { useReactToPrint } from "react-to-print";
import { handleVitalChange } from "../../utils/Functions";
import { getAllAllergiesByPatientId } from "../../utils/ApiFunctions";
import { getHighRiskMedicineByName } from "../../utils/ApiFunctions";
import { getConflictMedicineByName } from "../../utils/ApiFunctions";
import { closeMedicineListByListId } from "../../utils/ApiFunctions";
import Login2P from "../auth/Login2P";

export default function List({
  isNew,
  isCopy,
  patientId,
  documentId,
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
}) {
  const sub = localStorage.getItem("sub");
  const sub2P = localStorage.getItem("sub2P");
  const doctorUserName = JSON.parse(localStorage.getItem("patient"))?.doctorUserName?.split("\\")[1];
  
  const businessRole = localStorage.getItem("businessRole");
  const [presentation, setPresentation] = useState(3);
  const textareaRef = useRef(null);
  const navigate = useNavigate();

  const redirectTo = `/patientdetails/${patientId}`;
  const timeout = 5 * 60 * 1000;
  const timerRef = useRef(null);

  const resetTimer = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
    }
    timerRef.current = setTimeout(() => {
      navigate(redirectTo);
    }, timeout);
  };

  useEffect(() => {
    const events = ["mousemove", "keydown", "scroll", "click", "touchstart"];
    const handleActivity = () => resetTimer();

    events.forEach((event) => window.addEventListener(event, handleActivity));
    resetTimer();

    return () => {
      clearTimeout(timerRef.current);
      events.forEach((event) =>
        window.removeEventListener(event, handleActivity),
      );
    };
  }, []);

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

  function handleAutoPlan(event) {
    const selectedValue = event.target.value;
    if (selectedValue === "autoPlan1") {
      setPresentation(1);
      setMedicineListItem([]);
    } else if (selectedValue === "autoPlan2") {
      setPresentation(2);
      setMedicineListItem([]);
    }
    if (selectedValue === "autoPlan1") {
      setPresentation(1);
      setMedicineListItem([]);
    } else if (selectedValue === "autoPlan3") {
      setPresentation(3);
      setMedicineListItem([]);
    }
  }

  const [startDate, setStartDate] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [endDate, setEndDate] = useState(() => {
    const today = new Date();
    const futureDate = new Date();
    futureDate.setDate(today.getDate() + 20);
    return futureDate.toISOString().split("T")[0];
  });

  const [startDate2, setStartDate2] = useState("");
  const [endDate2, setEndDate2] = useState("");

  useEffect(() => {
    const timer = setTimeout(() => {
      if (ROLE !== "DOCTOR") {
        setStartDate2(new Date().toISOString().split("T")[0]);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [ROLE]);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (ROLE !== "DOCTOR") {
        setEndDate2(new Date().toISOString().split("T")[0]);
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [ROLE]);

  const [dateRange, setDateRange] = useState([]);
  const [dateRange2, setDateRange2] = useState([]);

  const [enableAddNewMedicine, setEnableAddNewMedicine] = useState(false);
  const [showDatePicker, setShowDatePicker] = useState(true);
  const [showRegime, setShowRegime] = useState(!isNew);

  const [showSuccessModal, setShowSuccessModal] = useState(false);

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

  const handleRangeChange = (range) => {
    setEnableAddNewMedicine(true);
  };

  useEffect(() => {
    if (!startDate) {
      setStartDate(medicineDetails[0]?.medicineDetails[0]?.date);
    }
    if (!endDate) {
      setEndDate(
        medicineDetails[0]?.medicineDetails[
          medicineDetails[0]?.medicineDetails.length - 1
        ]?.date,
      );
    }
  }, [medicineDetails]);

  useEffect(() => {
    if (!startDate2) {
      setStartDate2(medicineDetails[0]?.medicineDetails[0]?.date);
    }
    if (!endDate2) {
      setEndDate2(
        medicineDetails[0]?.medicineDetails[
          medicineDetails[0]?.medicineDetails.length - 1
        ]?.date,
      );
    }
  }, [medicineDetails]);

  useEffect(() => {
    if (startDate && endDate && new Date(startDate) <= new Date(endDate)) {
      const range = getDatesInRange(startDate, endDate);
      setDateRange(range);
      handleRangeChange(range);
    } else {
      setDateRange([]);
      handleRangeChange([]);
    }
  }, [startDate, endDate, endDate2]);

  useEffect(() => {
    if (startDate2 && endDate2 && new Date(startDate2) <= new Date(endDate2)) {
      const range = getDatesInRange(startDate2, endDate2);
      setDateRange2(range);
      handleRangeChange(range);
    } else {
      setDateRange2([]);
      handleRangeChange([]);
    }
  }, [startDate2, endDate2]);

  function getDatePicker() {
    return (
      <div>
        <label className="medicine-date-picker">
          Від:&nbsp;&nbsp;
          <input
            type="date"
            value={startDate}
            onChange={(e) => {
              setStartDate(e.target.value);
              const date = new Date(e.target.value);
              date.setDate(date.getDate() + 20);
              const result = formatDateToISO(date);
              setEndDate(result);
            }}
            className="border p-1 rounded"
          />
        </label>
        {
          <label>
            До:&nbsp;&nbsp;
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="border p-1 rounded"
            />
          </label>
        }
        <br></br>
      </div>
    );
  }

  function getDatePicker2() {
    return (
      <div>
        <label>
          Від:&nbsp;&nbsp;
          <input
            type="date"
            value={startDate2}
            onChange={(e) => setStartDate2(e.target.value)}
            className="border p-1 rounded"
          />
        </label>
        <label>
          &nbsp;&nbsp;До:&nbsp;&nbsp;
          <input
            type="date"
            value={endDate2}
            onChange={(e) => setEndDate2(e.target.value)}
            className="border p-1 rounded"
          />
        </label>
        <br></br>
      </div>
    );
  }

  function medicineDatePicker() {
    return (
      <>
        <h3 className="date-picker-heading">Оберіть початкову дату</h3>
        {getDatePicker()}
      </>
    );
  }
  function handleGenerateDEDoc() {
    const now = new Date();
    const plusThreeHours = new Date(
      now.getTime() + 3 * 60 * 60 * 1000,
    ).toISOString();
    generateDEDoc(medicineList.medicineListID, plusThreeHours);
    setShowSuccessModal(true);
    setGenerateDe(true);
  }

  const [generateDe, setGenerateDe] = useState(false);

  const printRef = useRef();

  const handlePrint = useReactToPrint({
    contentRef: printRef,
    documentTitle: "Medicine List",
  });

  const [active, setActive] = useState({ period: null, details: null });
  const overlayRef = useRef(null);
  const textareaRef2 = useRef(null);

  const showPersonHandler = (detailsObj, period) => {
    console.log(detailsObj);
    setActive({ details: detailsObj, period });
  };

  const hidePersonHandler = () => setActive({ period: null, details: null });

  const getPersonText = () => {
    const period = active.period;
    const det = active.details;
    if (!period || !det?.[period]) return "Дані відсутні";
    const doctor = det[period]?.doctorName || "—";
    const nurse = det[period]?.nurseName || "—";
    return `Запланував(ла): ${doctor}\nВиконав(ла): ${nurse}`;
  };

  useEffect(() => {
    if (!active.period) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    if (textareaRef2.current) {
      try {
        textareaRef2.current.focus({ preventScroll: true });
      } catch {
        textareaRef2.current.focus();
      }
    }

    return () => {
      document.body.style.overflow = prevOverflow;
    };
  }, [active]);

  useEffect(() => {
    function onDocClick(e) {
      if (
        active.period &&
        overlayRef.current &&
        !overlayRef.current.contains(e.target)
      ) {
        hidePersonHandler();
      }
    }
    function onKey(e) {
      if (e.key === "Escape") hidePersonHandler();
    }
    document.addEventListener("mousedown", onDocClick);
    document.addEventListener("touchstart", onDocClick);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDocClick);
      document.removeEventListener("touchstart", onDocClick);
      document.removeEventListener("keydown", onKey);
    };
  }, [active]);

  const [inputValues, setInputValues] = useState({});

  const [allergies, setAllergies] = useState([]);

  useEffect(() => {
    const fetch = async () => {
      try {
        const data = await getAllAllergiesByPatientId(patientId);

        const flattenedAllergies = data.flatMap(
          (item) => item.allergiesList || [],
        );

        setAllergies(flattenedAllergies);
      } catch (error) {
        console.error("Error fetching documents:", error);
      }
    };

    fetch();
  }, [patientId]);

  const [showErytro, setShowErytro] = useState([]);

  function handleShowErytro(i, showErytro) {
    setShowErytro((prev) => {
      const updated = [...prev];
      updated[i] = showErytro;
      return updated;
    });
  }

  const [showExistingDay, setShowExistingDay] = useState(false);
  function handleShowExistingDay() {
    setShowExistingDay((prevValue) => !prevValue);
  }
  const [autofillStartDate, setAutofillStartDate] = useState("");
  const [autofillEndDate, setAutofillEndDate] = useState("");

  const [existingDay, setExistingDay] = useState([
    {
      medicineDetails: [
        {
          id: nanoid(),
          date: autofillStartDate,
          morning: {
            id: nanoid(),
            time: "",
            medicineDose: "",
            isPlanned: false,
            isPlannedAndFinished: false,
            isCompleted: false,
            doctorName: "",
            nurseName: "",
          },
          day: {
            id: nanoid(),
            time: "",
            medicineDose: "",
            isPlanned: false,
            isPlannedAndFinished: false,
            isCompleted: false,
            doctorName: "",
            nurseName: "",
          },
          evening: {
            id: nanoid(),
            time: "",
            medicineDose: "",
            isPlanned: false,
            isPlannedAndFinished: false,
            isCompleted: false,
            doctorName: "",
            nurseName: "",
          },
          night: {
            id: nanoid(),
            time: "",
            medicineDose: "",
            isPlanned: false,
            isPlannedAndFinished: false,
            isCompleted: false,
            doctorName: "",
            nurseName: "",
          },
        },
      ],
    },
  ]);
  const [autofillRowNumber, setAutofillRowNumber] = useState(0);

  const existingDayRender = existingDay[0].medicineDetails?.map(
    (details, j) => {
      return (
        <div
          className="medicine-presc-details-row"
          style={{
            display: "flex",
            flexDirection: "column",
            position: "fixed",
            zIndex: 10,
            backgroundColor: "white",
          }}
        >
          <div className="autofill-dates-container">
            <input
              style={{
                position: "fixed",
                left: "515px",
                zIndex: 10,
                backgroundColor: "white",
              }}
              type="date"
              value={autofillStartDate}
              onChange={(e) => setAutofillStartDate(e.target.value)}
            />
            <input
              style={{
                position: "fixed",
                left: "515px",
                marginTop: "25px",
                zIndex: 10,
                backgroundColor: "white",
              }}
              type="date"
              value={autofillEndDate}
              onChange={(e) => setAutofillEndDate(e.target.value)}
            />
            <span
              style={{
                display: "block",
                position: "fixed",
                left: "515px",
                marginTop: "50px",
                zIndex: 10,
                backgroundColor: "white",
                width: "85px",
                fontWeight: "bold",
              }}
            >
              Номер рядка:
            </span>
            <input
              style={{
                position: "fixed",
                left: "607px",
                marginTop: "50px",
                zIndex: 10,
                backgroundColor: "white",
                width: "20px",
              }}
              type="number"
              value={autofillRowNumber}
              onChange={(e) => {
                setAutofillRowNumber(e.target.value);
              }}
            />
          </div>
          <div
            className="medicine-presc-details-row-first-div"
            style={{ position: "fixed", left: "650px", zIndex: 10 }}
          >
            {(() => {
              return (
                <div
                  style={{
                    fontWeight: "bold",
                    textAlign: "center",
                    border: "1px solid black",
                    width: "120px",
                  }}
                >
                  <div
                    className="day-period"
                    style={{
                      display: "flex",
                      gap: 0,
                    }}
                  >
                    <div
                      onClick={() =>
                        ROLE == "DOCTOR" &&
                        showPersonHandler(details, "morning")
                      }
                      style={{ cursor: "pointer", backgroundColor: "white" }}
                    >
                      Р
                    </div>
                    <div
                      onClick={() =>
                        ROLE == "DOCTOR" && showPersonHandler(details, "day")
                      }
                      style={{ cursor: "pointer", backgroundColor: "white" }}
                    >
                      Д
                    </div>
                    <div
                      onClick={() =>
                        ROLE == "DOCTOR" &&
                        showPersonHandler(details, "evening")
                      }
                      style={{ cursor: "pointer", backgroundColor: "white" }}
                    >
                      В
                    </div>
                    <div
                      onClick={() =>
                        ROLE == "DOCTOR" && showPersonHandler(details, "night")
                      }
                      style={{ cursor: "pointer", backgroundColor: "white" }}
                    >
                      Н
                    </div>
                  </div>

                  {active.period && active.details && (
                    <div
                      ref={overlayRef}
                      style={{
                        position: "fixed",
                        inset: 0,
                        zIndex: 9999,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        background: "rgba(0,0,0,0.25)",
                      }}
                    >
                      <textarea
                        ref={textareaRef2}
                        readOnly
                        value={getPersonText()}
                        onBlur={hidePersonHandler}
                        style={{
                          width: "90%",
                          maxWidth: 700,
                          minHeight: 120,
                          fontWeight: "bold",
                          padding: 12,
                          borderRadius: 8,
                          boxShadow: "0 6px 18px rgba(0,0,0,0.12)",
                          resize: "none",
                          whiteSpace: "pre-wrap",
                        }}
                      />
                    </div>
                  )}
                </div>
              );
            })()}
            <div key={details.id} className="medicine-presc-details-column">
              <div className="day-cells-container">
                {["morning", "day", "evening", "night"].map((period) => (
                  <div key={period} className="day-cell">
                    <label
                      className="day-cell-input-label"
                      style={(() => {
                        const status = details[period];

                        if (
                          status.isPlanned &&
                          !status.isCompleted &&
                          !status.isPlannedAndFinished
                        ) {
                          return {
                            backgroundColor: "lightblue",
                          };
                        } else if (
                          status.isCompleted &&
                          !status.isCompletedAndFinished
                        ) {
                          return {
                            backgroundColor: "lightgreen",
                          };
                        }

                        if (
                          status.isPlannedAndFinished &&
                          !status.isCompletedAndFinished
                        ) {
                          return {
                            backgroundColor: "purple",
                          };
                        } else if (status.isCompletedAndFinished) {
                          return {
                            backgroundColor: "darkgreen",
                          };
                        } else {
                          return { backgroundColor: "white" };
                        }
                      })()}
                    >
                      <input
                        className="day-cell-input"
                        type="checkbox"
                        checked={details[period].isPlanned}
                        onChange={(e) => {
                          if (
                            ROLE === "DOCTOR" &&
                            !details[period].isCompleted
                          ) {
                            !isScaled &&
                              handleDetailChange(
                                e,
                                existingDay.id,
                                details.id,
                                period,
                                "isPlanned",
                                sub,
                                sub2P,
                                businessRole,
                                setExistingDay,
                              );
                          }
                        }}
                      />
                    </label>
                    {
                      <textarea
                        style={{ fontWeight: "bold" }}
                        type="text"
                        name="medicineDose"
                        value={details[period].medicineDose}
                        disabled={isScaled}
                        placeholder="доз."
                        onChange={(e) => {
                            !isScaled &&
                            handleDetailChange(
                              e,
                              existingDay.id,
                              details.id,
                              period,
                              "medicineDose",
                              sub,
                              sub2P,
                              businessRole,
                              setExistingDay,
                            );
                        }}
                        className="day-cell-dose-input"
                      ></textarea>
                    }
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      );
    },
  );

  const [pendingAction, setPendingAction] = useState(null);
  const handleLoginSuccess = () => {
    if (pendingAction) {
      if (localStorage.getItem("sub") === localStorage.getItem("sub2P")) {
        alert(
          "Додатвкова перевірка повинна проводитись виключно іншою особою!",
        );
        return;
      }
      pendingAction();
      setPendingAction(null);
    }
    setShowLogin2P(false);
  };

  function handleRiskMedicineCheck(medicineName, isCompleted, func) {
    getHighRiskMedicineByName(medicineName)
      .then((result) => {
        if (result?.length > 0) {
          if (!isCompleted) {
            setPendingAction(() => func);
            setShowLogin2P(true);
          } else {
            func();
          }
        } else {
          func();
        }
      })
      .catch((error) => {
        console.error(error);
      });
  }

  const [highRiskMap, setHighRiskMap] = useState({});

  useEffect(() => {
    const fetchHighRisk = async () => {
      const map = {};
      await Promise.all(
        (medicineDetails || []).map(async (row) => {
          try {
            const result = await getHighRiskMedicineByName(row.medicineName);
            map[row.medicineName] = result?.length > 0;
          } catch (e) {
            console.error(e);
            map[row.medicineName] = false;
          }
        }),
      );
      setHighRiskMap(map);
    };

    fetchHighRisk();
  }, [medicineDetails]);

  const [conflictMedicineMap, setConflictMedicineMap] = useState({});

  useEffect(() => {
    const fetchConflictMedicine = async () => {
      const map = {};
      await Promise.all(
        (medicineDetails || []).map(async (row, idx) => {
          try {
            const result = await getConflictMedicineByName(row.medicineName);
            //map[row.medicineName] = result?.length > 0;
            map[row.medicineName] = result[0]?.ptg;
          } catch (e) {
            console.error(e);
            map[row.medicineName] = false;
          }
        }),
      );
      setConflictMedicineMap(map);
    };

    fetchConflictMedicine();
  }, [medicineDetails]);

  const [conflictPairs, setConflictPairs] = useState({});

  useEffect(() => {
    const ptgValues = Object.values(conflictMedicineMap);
   

    const hasPair = (a, b) => ptgValues.includes(a) && ptgValues.includes(b);

    const pairs = {
      "1-2": hasPair('1', '2'),
      "1-2,3": hasPair('1','2,3'),
      "2,3-4": hasPair('2,3', '4'),
      "3-4": hasPair('3', '4'),
      "5-6": hasPair('5', '6'),
    };

    setConflictPairs(pairs);
  }, [conflictMedicineMap]);

  const isConflictingRow = (medicineName) => {
    const ptg = conflictMedicineMap[medicineName];

    return (
      (conflictPairs["1-2"] && (ptg == 1 || ptg == 2)) ||
      (conflictPairs["1-2,3"] && (ptg == 1 || ptg == '2,3')) ||
      (conflictPairs["2,3-4"] && (ptg == '2,3' || ptg == 4)) ||
      (conflictPairs["3-4"] && (ptg == 3 || ptg == 4)) ||
      (conflictPairs["5-6"] && (ptg == 5 || ptg == 6))
    );
  };

  //console.log(conflictPairs);

  const [showLogin2P, setShowLogin2P] = useState(false);

  function handleCloseList() {
    closeMedicineListByListId(documentId);
    navigate(`/patientdetails/${patientId}`)
  }

  return (
    <>
      {showLogin2P && <Login2P onSuccess={handleLoginSuccess} />}
      {showExistingDay && (
        <div
          style={{
            position: "fixed",
            left: "430px",
            backgroundColor: "white",
            width: "345px",
            height: "80px",
            zIndex: 999999999,
          }}
        >
          {showExistingDay && existingDayRender}
          <button
            style={{
              position: "fixed",
              left: "430px",
              marginTop: "20px",
              backgroundColor: "white",
            }}
            type="button"
            className="fill-all-button"
            onClick={handleShowExistingDay}
          >
            Закрити план
          </button>
          {!isScaled && ROLE == "DOCTOR" && showExistingDay && (
            <>
              <button
                type="button"
                style={{
                  position: "fixed",
                  left: "435px",
                  marginTop: "55px",
                  zIndex: 10,
                  fontSize: "11px",
                }}
                onClick={() =>
                  handleAutofill(
                    existingDay,
                    medicineDetails,
                    autofillRowNumber - 1,
                    setMedicineListItem,
                    dateRange2,
                    sub,
                    autofillStartDate,
                    autofillEndDate,
                  )
                }
              >
                Заповнити
              </button>
            </>
          )}{" "}
        </div>
      )}
      <div style={{ display: "none" }}>
        {<ListPrintWrapper ref={printRef} medicineList={medicineList} />}
      </div>
      {/* first*/}
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
          {!showExistingDay && (
            <button
              style={{
                position: "sticky",
                left: "2px",
                marginTop: "20px",
                backgroundColor: "white",
              }}
              type="button"
              className="fill-all-button"
              onClick={handleShowExistingDay}
            >
              Відкрити план
            </button>
          )}
          <div className="medicine-regime-container">
            {
              <div className="date-picker-container">
                {!isScaled && showRegime && getDatePicker2()}
                {!isScaled && showRegime && (
                  <div className="arrows-container">
                    <FontAwesomeIcon
                      icon={faArrowLeft}
                      onClick={() => {
                        setStartDate2((prevValue) => {
                          const newDate = new Date(prevValue);
                          newDate.setDate(newDate.getDate() - 1);
                          return formatDateToISO(newDate);
                        });
                        setEndDate2((prevValue) => {
                          const newDate = new Date(prevValue);
                          newDate.setDate(newDate.getDate() - 1);
                          return formatDateToISO(newDate);
                        });
                      }}
                    />
                    <FontAwesomeIcon
                      icon={faArrowRight}
                      onClick={() => {
                        setStartDate2((prevValue) => {
                          const newDate = new Date(prevValue);
                          newDate.setDate(newDate.getDate() + 1);
                          return formatDateToISO(newDate);
                        });
                        setEndDate2((prevValue) => {
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
            <div className="medicine-date-picker">
              {(isNew || isCopy) && showDatePicker && medicineDatePicker()}
            </div>
            <div className="medicine-method-column">Метод</div>
            {medicineDetails?.map((medicineRow, i) => (
              <div
                key={i}
                style={{
                  backgroundColor: highRiskMap[medicineRow.medicineName]
                    ? "#FFCCCB"
                    : isConflictingRow(medicineRow.medicineName)
                      ? "#ffe680" // yellow for conflict
                      : "transparent",
                }}
                className={
                  isConflictingRow(medicineRow.medicineName) ? "medicine-row blink" : "medicine-row"
                }
              >
                {!isScaled &&
                isNew &&
                ROLE == "DOCTOR" &&
                (sub == medicineRow.medicineListItemEditUser ||
                  sub == medicineList.medicineListCreationUser ||
                  sub == "v.yamnyi") ? (
                  <div className="trash-ico-container">
                    <FontAwesomeIcon
                      icon={faTrash}
                      onClick={() =>
                        handleRemoveMedicineItem(
                          setMedicineList,
                          setMedicineListItem,
                          i,
                        )
                      }
                    />
                  </div>
                ) : (
                  <div>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</div>
                )}
                <div
                  style={{
                    position: "sticky",
                    left: "1px",
                    zIndex: 10,
                    width: "179px",
                    overflow: "hidden",
                    marginRight: "3px",
                    marginLeft: "10px",
                    backgroundColor: "white",
                  }}
                >
                  <div style={{ position: "sticky", left: 1 }}>
                    <h4 style={{ fontWeight: "bold" }}>Рядок № {i + 1}</h4>
                  </div>
                  <div className="medicine-name">
                    <textarea
                      style={{ fontWeight: "bold", height: "62px" }}
                      className="auto-resize-textarea"
                      ref={textareaRef}
                      key={i}
                      name="medicineName"
                      data-key={i}
                      disabled={isScaled}
                      value={
                        inputValues[medicineRow.id] ??
                        medicineRow.medicineName ??
                        ""
                      }
                      onChange={(e) => {
                        const value = e.target.value;

                        if (allergies.includes(value)) {
                          alert("У пацієнта алергія на препарат " + value);
                          return;
                        }

                        setInputValues((prev) => {
                          return {
                            ...prev,
                            [medicineRow.id]: value,
                          };
                        });

                        if (isEmpty(value)) {
                          setSearchedMedicine([]);
                          setShowSuggestions(false);
                          return;
                        }

                        searchMedicine(value.trim()).then((results) => {
                          setSearchedMedicine(results);
                          setShowSuggestions(true);
                          setCurrentRowSuggestionIndex(i);
                          setTriggerSearchedMedicine(true);
                        });
                      }}
                      onFocus={() => {
                        setShowSuggestions(true);
                        setCurrentRowSuggestionIndex(i);
                      }}
                      onBlur={() => {
                        setTimeout(() => {
                          const currentInput = inputValues[medicineRow.id];
                          if (isEmpty(currentInput)) {
                            handleShowErytro(i, false);
                            setMedicineListItem((prev) =>
                              prev.map((item) =>
                                item.id === medicineRow.id
                                  ? {
                                      ...item,
                                      medicineName: "",
                                      medicineMethod: "",
                                    }
                                  : item,
                              ),
                            );
                          } else {
                            setInputValues((prev) => ({
                              ...prev,
                              [medicineRow.id]: medicineRow.medicineName || "",
                            }));
                          }

                          setShowSuggestions(false);
                          setTriggerSearchedMedicine(false);
                        }, 100);
                      }}
                    />

                    {currentRowSuggestionIndex === i &&
                      showSuggestions &&
                      triggerSearchedMedicine &&
                      searchedMedicine.length > 0 && (
                        <div
                          className="searched-patients-dropdown"
                          style={{ overflowX: "scroll", width: "100%" }}
                        >
                          {searchedMedicine.map((medicine, index) => (
                            <a
                              key={medicine.id}
                              href="#"
                              onMouseDown={(e) => {
                                let foundAllergy = false;
                                if (!isScaled) {
                                  allergies.forEach((a) => {
                                    if (
                                      a.includes(medicine.name.split(" ")[0])
                                    ) {
                                      alert(
                                        "У пацієнта алергія на препарат " +
                                          medicine.name,
                                      );
                                      foundAllergy = true;
                                      return;
                                    }
                                  });
                                  if (foundAllergy) return;

                                  setMedicineListItem((prev) =>
                                    prev.map((item) =>
                                      item.id === medicineRow.id
                                        ? {
                                            ...item,
                                            medicineName: medicine.name,
                                          }
                                        : item,
                                    ),
                                  );

                                  setInputValues((prev) => ({
                                    ...prev,
                                    [medicineRow.id]: medicine.name,
                                  }));
                                  if (medicine.name.includes("Еритро")) {
                                    handleShowErytro(i, true);
                                  } else {
                                    handleShowErytro(i, false);
                                  }
                                  setShowSuggestions(false);
                                  setTriggerSearchedMedicine(false);
                                }
                              }}
                            >
                              {medicine.name}
                            </a>
                          ))}
                        </div>
                      )}
                  </div>
                </div>
                <div className="medicine-method flex flex-column">
                  {(showErytro[i] ||
                    medicineRow.medicineName.includes("Еритро")) && (
                    <div>
                      <h5 className="bold">
                        Діагноз:
                        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                      </h5>
                      <select
                        disabled={isScaled}
                        value={medicineRow.regime}
                        onChange={(e) => {
                          !isScaled &&
                            ROLE === "DOCTOR" &&
                            handleMedicineRegimeChange(
                              e,
                              medicineRow.id,
                              "regime",
                              setMedicineListItem,
                            );
                        }}
                      >
                        <option value={"Постгеморагічна анемія"} selected>
                          Постгеморагічна анемія
                        </option>
                        <option value={"Анемія хронічних хвороб"}>
                          Анемія хронічних хвороб
                        </option>
                      </select>
                    </div>
                  )}
                  {
                    /*showErytro[i] ||
                  medicineRow.medicineName.includes("Еритро") ? (
                    <div>
                      <h5 className="bold" style={{ marginTop: "5px" }}>
                        Метод введення:
                      </h5>
                      <select
                        disabled={isScaled}
                        value={medicineRow.medicineMethod}
                        onChange={(e) => {
                          !isScaled &&
                            ROLE === "DOCTOR" &&
                            handleMedicineMethodChange(
                              e,
                              medicineRow.id,
                              "regime",
                              setMedicineListItem,
                            );
                        }}
                      >
                        <option value={"Внутрішньовенно"} selected>
                          Внутрішньовенно
                        </option>
                      </select>
                    </div>
                  ) : */ <textarea
                      type="text"
                      name="medicineMethod"
                      style={{ fontWeight: "bold" }}
                      className="auto-resize-textarea"
                      disabled={isScaled}
                      value={medicineRow.medicineMethod}
                      onChange={(e) => {
                        !isScaled &&
                          ROLE === "DOCTOR" &&
                          handleMedicineMethodChange(
                            e,
                            medicineRow.id,
                            "medicineMethod",
                            setMedicineListItem,
                          );
                      }}
                    ></textarea>
                  }
                </div>
                <div className="flex flex-column">
                  {/*!isScaled &&
                    ROLE == "DOCTOR" &&
                    medicineList.medicineListCreationUser === sub &&
                    medicineRow.medicineListItemEditUser !== sub && (
                      <button
                        type="button"
                        className="fill-all-button"
                        onClick={() =>
                          handleApproveMedicine(
                            i,
                            setMedicineList
                          )
                        }
                      >
                        {medicineList.approvedRowIndexes.includes(i.toString())
                          ? "Розтвердити"
                          : "Затвердити"}
                      </button>
                    )*/}
                </div>
                {medicineRow.medicineDetails?.map((details, j) => {
                  return (
                    <div className="medicine-presc-details-row">
                      {details.date >= startDate2 &&
                        details.date <= endDate2 && (
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
                                    {`${details.date?.split("-")[2] || "??"} ${
                                      months[details.date?.split("-")[1]] ||
                                      "??"
                                    } ${details.date?.split("-")[0] || "??"}`}

                                    <div
                                      className="day-period"
                                      style={{
                                        display: "flex",
                                        gap: 0,
                                        marginTop: 6,
                                      }}
                                    >
                                      <div
                                        onClick={() =>
                                          ROLE == "DOCTOR" &&
                                          showPersonHandler(details, "morning")
                                        }
                                        style={{ cursor: "pointer" }}
                                      >
                                        Р
                                      </div>
                                      <div
                                        onClick={() =>
                                          ROLE == "DOCTOR" &&
                                          showPersonHandler(details, "day")
                                        }
                                        style={{ cursor: "pointer" }}
                                      >
                                        Д
                                      </div>
                                      <div
                                        onClick={() =>
                                          ROLE == "DOCTOR" &&
                                          showPersonHandler(details, "evening")
                                        }
                                        style={{ cursor: "pointer" }}
                                      >
                                        В
                                      </div>
                                      <div
                                        onClick={() =>
                                          ROLE == "DOCTOR" &&
                                          showPersonHandler(details, "night")
                                        }
                                        style={{ cursor: "pointer" }}
                                      >
                                        Н
                                      </div>
                                    </div>

                                    {active.period && active.details && (
                                      <div
                                        ref={overlayRef}
                                        style={{
                                          position: "fixed",
                                          inset: 0,
                                          zIndex: 9999,
                                          display: "flex",
                                          alignItems: "center",
                                          justifyContent: "center",
                                          background: "rgba(0,0,0,0.25)",
                                        }}
                                      >
                                        <textarea
                                          ref={textareaRef2}
                                          readOnly
                                          value={getPersonText()}
                                          onBlur={hidePersonHandler}
                                          style={{
                                            width: "90%",
                                            maxWidth: 700,
                                            minHeight: 120,
                                            fontWeight: "bold",
                                            padding: 12,
                                            borderRadius: 8,
                                            boxShadow:
                                              "0 6px 18px rgba(0,0,0,0.12)",
                                            resize: "none",
                                            whiteSpace: "pre-wrap",
                                          }}
                                        />
                                      </div>
                                    )}
                                  </div>
                                );
                              } catch (error) {
                                console.error("Error rendering date:", error);
                                return (
                                  <div
                                    className="medicine-date"
                                    key={j}
                                    style={{ fontWeight: "bold", color: "red" }}
                                  >
                                    Не вірна дата
                                  </div>
                                );
                              }
                            })()}
                            <div
                              key={details.id}
                              className="medicine-presc-details-column"
                            >
                              <div className="day-cells-container">
                                {["morning", "day", "evening", "night"].map(
                                  (period) => (
                                    <div key={period} className="day-cell">
                                      <label
                                        className="day-cell-input-label"
                                        style={(() => {
                                          const status = details[period];

                                          if (
                                            status.isPlanned &&
                                            !status.isCompleted &&
                                            !status.isPlannedAndFinished
                                          ) {
                                            return {
                                              backgroundColor: "lightblue",
                                            };
                                          } else if (
                                            status.isCompleted &&
                                            !status.isCompletedAndFinished
                                          ) {
                                            return {
                                              backgroundColor: "lightgreen",
                                            };
                                          }

                                          if (
                                            status.isPlannedAndFinished &&
                                            !status.isCompletedAndFinished
                                          ) {
                                            return {
                                              backgroundColor: "purple",
                                            };
                                          } else if (
                                            status.isCompletedAndFinished
                                          ) {
                                            return {
                                              backgroundColor: "darkgreen",
                                            };
                                          } else {
                                            return {};
                                          }
                                        })()}
                                        onAuxClick={(e) => {
                                          if (
                                            medicineList.approvedRowIndexes.includes(
                                              i.toString(),
                                            ) &&
                                            ROLE === "NURSE" &&
                                            details[period].isPlannedAndFinished
                                          ) {
                                            !isScaled &&
                                              handleDetailChange(
                                                e,
                                                medicineRow.id,
                                                details.id,
                                                period,
                                                "isCompletedAndFinished",
                                                sub,
                                                sub2P,
                                                businessRole,
                                                setMedicineListItem,
                                              );
                                          } else if (
                                            ROLE === "DOCTOR" &&
                                            details[period].isPlanned
                                          ) {
                                            !isScaled &&
                                              handleDetailChange(
                                                e,
                                                medicineRow.id,
                                                details.id,
                                                period,
                                                "isPlannedAndFinished",
                                                sub,
                                                sub2P,
                                                businessRole,
                                                setMedicineListItem,
                                              );
                                          }
                                        }}
                                      >
                                        <input
                                          className="day-cell-input"
                                          type="checkbox"
                                          checked={
                                            ROLE === "NURSE"
                                              ? details[period].isCompleted
                                              : details[period].isPlanned
                                          }
                                          onChange={(e) => {
                                            if (
                                              // medicineList.approvedRowIndexes.includes(
                                              //   i.toString(),
                                              // ) &&
                                              ROLE === "NURSE" &&
                                              details[period].isPlanned
                                            ) {
                                              const eventData = {
                                                type: e.target.type,
                                                checked: e.target.checked,
                                                value: e.target.value,
                                              };

                                              localStorage.setItem(
                                                "pendingEvent",
                                                JSON.stringify(eventData),
                                              );

                                              !isScaled &&
                                                handleRiskMedicineCheck(
                                                  medicineRow.medicineName,
                                                  details[period].isCompleted,
                                                  () => {
                                                    const saved = JSON.parse(
                                                      localStorage.getItem(
                                                        "pendingEvent",
                                                      ),
                                                    );

                                                    handleDetailChange(
                                                      saved,
                                                      medicineRow.id,
                                                      details.id,
                                                      period,
                                                      "isCompleted",
                                                      sub,
                                                      sub2P,
                                                      businessRole,
                                                      setMedicineListItem,
                                                    );
                                                  },
                                                );
                                              /*!isScaled && !sub2P
                                                handleDetailChange(
                                                  e,
                                                  medicineRow.id,
                                                  details.id,
                                                  period,
                                                  "isCompleted",
                                                  sub,
                                                  sub2P,
                                                  businessRole,
                                                  setMedicineListItem,
                                                );
                                              localStorage.removeItem(
                                                "accessToken2P",
                                              );
                                              localStorage.removeItem("sub2P");*/
                                            } else if (
                                              ROLE === "DOCTOR" &&
                                              !details[period].isCompleted
                                            ) {
                                              !isScaled &&
                                                handleDetailChange(
                                                  e,
                                                  medicineRow.id,
                                                  details.id,
                                                  period,
                                                  "isPlanned",
                                                  sub,
                                                  sub2P,
                                                  businessRole,
                                                  setMedicineListItem,
                                                );
                                            }
                                          }}
                                        />
                                      </label>
                                      {
                                        <textarea
                                          style={{ fontWeight: "bold" }}
                                          type="text"
                                          name="medicineDose"
                                          value={details[period].medicineDose}
                                          disabled={isScaled}
                                          placeholder="доз."
                                          onChange={(e) => {
                                              !isScaled &&
                                              handleDetailChange(
                                                e,
                                                medicineRow.id,
                                                details.id,
                                                period,
                                                "medicineDose",
                                                sub,
                                                sub2P,
                                                businessRole,
                                                setMedicineListItem,
                                              );
                                          }}
                                          className="day-cell-dose-input"
                                        ></textarea>
                                      }
                                      {/*(period === "morning" ||
                                        period === "evening") && (
                                        <input
                                          style={{ fontWeight: "bold" }}
                                          type="number"
                                          name="pain"
                                          min={0}
                                          max={10}
                                          value={details[period].pain}
                                          disabled={isScaled}
                                          placeholder="шб."
                                          onChange={(e) => {
                                            !isScaled &&
                                              handleDetailChange(
                                                e,
                                                medicineRow.id,
                                                details.id,
                                                period,
                                                "pain",
                                                sub,
                                                businessRole,
                                                setMedicineListItem
                                              );
                                          }}
                                          className="day-cell-dose-input"
                                        ></input>
                                      )*/}
                                      {medicineRow.medicineDetails.length - 1 ==
                                        j &&
                                        ROLE === "DOCTOR" &&
                                        !isScaled && (
                                          <button
                                            className="plus"
                                            type="button"
                                            style={{ top: "10px" }}
                                            onClick={() => {
                                              const date = new Date(
                                                medicineDetails[i]
                                                  .medicineDetails[j].date,
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

                                              const datesRange =
                                                getDatesInRange(date, lastDate);

                                              if (
                                                datesRange.length +
                                                  medicineDetails[i]
                                                    ?.medicineDetails.length >
                                                90
                                              ) {
                                                alert(
                                                  "Загальна кількість днів для одного призначення не може перевищувати 90",
                                                );
                                                return;
                                              }

                                              handleAddNewDayDetails(
                                                medicineDetails,
                                                i,
                                                setMedicineListItem,
                                                datesRange,
                                              );

                                              endDate2 <=
                                                formatDateToISO(lastDate) &&
                                                setEndDate2(
                                                  formatDateToISO(lastDate),
                                                );
                                            }}
                                          >
                                            +
                                          </button>
                                        )}

                                      {medicineRow.medicineDetails.length - 1 ==
                                        j &&
                                        medicineRow.medicineDetails.length >
                                          1 &&
                                        ROLE === "DOCTOR" &&
                                        !isScaled && (
                                          <button
                                            className="plus danger"
                                            type="button"
                                            onClick={() =>
                                              handleDelNewDayDetails(
                                                medicineDetails,
                                                i,
                                                j,
                                                setMedicineListItem,
                                              )
                                            }
                                          >
                                            -
                                          </button>
                                        )}

                                      {/*<input
                                          style={{ fontWeight: "bold" }}
                                          name="time"
                                          type="time"
                                          className="day-cell-time-input"
                                          value={
                                            details[period].time === "" &&
                                            period === "morning"
                                              ? "06:00"
                                              : details[period].time === "" &&
                                                period === "day"
                                              ? "12:00"
                                              : details[period].time === "" &&
                                                period === "evening"
                                              ? "18:00"
                                              : details[period].time === "" &&
                                                period === "night"
                                              ? "00:00"
                                              : details[period].time
                                          }
                                          onChange={(e) => {
                                            !isScaled &&
                                              handleDetailChange(
                                                e,
                                                medicineRow.id,
                                                details.id,
                                                period,
                                                "time",
                                                setMedicineListItem
                                              );
                                          }}
                                        />*/}
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
            ))}
            {/* return (
                    <div className="medicine-presc-details-row">
                      {details.date >= startDate2 &&
                        details.date <= endDate2 && ( */}

            {!isScaled && (
              <button
                style={{
                  visibility: ROLE === "DOCTOR" ? "visible" : "hidden",
                  opacity: ROLE === "DOCTOR" ? 1 : 0,
                  display: "inline-block",
                  marginRight: "270px",
                }}
                className="add-button"
                disabled={!enableAddNewMedicine}
                type="button"
                onClick={() => {
                  setShowDatePicker(false);
                  //refreshDates(medicineList, setMedicineListItem, dateRange)
                  handleAddNewMedicineItem2(
                    medicineDetails.length,
                    medicineDetails,
                    setMedicineList,
                    setMedicineListItem,
                    isNew || isCopy
                      ? dateRange
                      : getDatesInRange(
                          medicineList.medicineDetails[0].medicineDetails[0]
                            .date,
                          medicineList.medicineDetails[0].medicineDetails[
                            medicineList.medicineDetails[0].medicineDetails
                              .length - 1
                          ].date,
                        ),

                    sub,
                    setShowRegime,
                  );
                }}
              >
                Додати
              </button>
            )}
            {/*!isScaled && ROLE === "DOCTOR" && medicineDetails.length > 1 && (
              <button
                type="button"
                className="danger"
                onClick={() => handleRemoveMedicineItem(setMedicineListItem)}
                style={{ marginLeft: "1px" }}
              >
                Видалити
              </button>
            )*/}
          </div>
        </div>
        {!isScaled && (
          <div className="save-generate-buttons-container">
            <button
              type={medicineDetails.length == 0 ? "button" : "submit"}
              className="save-button"
              onClick={() =>
                medicineDetails.length == 0 && alert("Додайте принаймі 1 рядок")
              }
            >
              Зберегти
            </button>

            {/*<button
              type="button"
              className="save-button"
              onClick={handleGenerateDEDoc}
            >
              Згенерувати листок в Доктор Елекс
            </button>*/}

            <button type="button" className="save-button" onClick={handlePrint}>
              Роздрукувати
            </button>
            {(sub==doctorUserName || sub=='v.yamnyi') && <button type="button" className="save-button" onClick={handleCloseList}>
              Закрити листок
            </button>}
          </div>
        )}
      </form>

      {showSuccessModal && (
        <SuccessModal
          generateDe={generateDe}
          setShowSuccessModal={setShowSuccessModal}
        />
      )}
    </>
  );
}
