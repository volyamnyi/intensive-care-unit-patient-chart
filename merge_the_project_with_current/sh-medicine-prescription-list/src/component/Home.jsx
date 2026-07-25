import { useState, useRef, useEffect } from "react";
import { getMedicineListById, searchPatients } from "../utils/ApiFunctions";
import { getPatientById } from "../utils/ApiFunctions";
import {
  getAllDocumentsByPatientId,
  getAllInpatients,
} from "../utils/ApiFunctions";
import { deleteDocumentById } from "../utils/ApiFunctions";
import { Link, useParams } from "react-router-dom";
import { isEmpty } from "../utils/Functions";
import { formatDate } from "../utils/Functions";
import { formatDate2 } from "../utils/Functions";
import { formatDateToISO } from "../utils/Functions";
import { isoToTimestampSeconds } from "../utils/Functions";
import { isLessThanOneHour } from "../utils/Functions";
import ListDetails from "./MedicineList/ListDetails";
import { useMemo } from "react";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faTrash } from "@fortawesome/free-solid-svg-icons";
import ApproveModal from "./MedicineList/ApproveModal";
import MedicineListPrint from "./MedicineList/MedicineListPrint";
import { useReactToPrint } from "react-to-print";

export default function Home() {
  const [presentation, setPresentation] = useState(1);

  const { id } = useParams();
  const [patientStateId, setPatientStateId] = useState(id);
  const ROLE = localStorage.getItem("businessRole");

  const [showApproveModal, setShowApproveModal] = useState(false);

  const [documentsList, setDocumentsLList] = useState([
    {
      medicineListID: null,
      patientRef: null,
      documentName: "",
      medicineListCreationUser: "",
      medicineListCreationDate: null,
      medicineDetails: [],
    },
  ]);

  const [patientName, setPatientName] = useState("");
  const [searchedPatients, setSearchedPatients] = useState([
    { id: "", name: "" },
  ]);

  const [patient, setPatient] = useState({
    id: "",
    name: "",
    historyNumber: "",
    address: "",
    phone: "",
    department: "",
    roomNumber: "",
    bedNumber: "",
    gender: "",
    age: "",
    birthDate: "",
    doctor: "",
    doctorUserName: "",
    residenceStatus: "",
  });

  const [patients, setPatients] = useState([]);
  const [localSearchedPatients, setLocalSearchedPatients] = useState([]);
  const [patientsDocuments, setPatientsDocuments] = useState([]);

  const [residence, setResidence] = useState(
    localStorage.getItem("residence") ? localStorage.getItem("residence") : "19"
  );

  function handleResidenceClick(residence) {
    localStorage.setItem("residence", residence);
    setResidence(residence);
  }

  useEffect(() => {
    if (residence === "19" || residence === "37")
      getAllInpatients("byName:ASC", residence).then((patients) => {
        return setPatients(patients);
      });
  }, [residence]);

  const [selectDocument, setSelectDocument] = useState("1|1");
  const [selectedDocument, setSelectedDocument] = useState([]);

  function handleSelectDocument(link, selectedIndex) {
    setSelectDocument(link);
    localStorage.setItem("selectedIndex", selectedIndex);
    const newSelectedDocumentArr = Array(selectedDocument.length).fill(false);
    newSelectedDocumentArr[selectedIndex] = true;
    setSelectedDocument(newSelectedDocumentArr);
  }

  useEffect(() => {
    if (id && patientStateId) {
      getPatientById(patientStateId).then((patient) => {
        setPatientStateId(null);
        return setPatient(patient);
      });
    }
  }, [patient.id]);

  const [showSuggestions, setShowSuggestions] = useState(false);

  useEffect(() => {
    const fetchDocuments = async () => {
      if (!patient.id) return;

      try {
        const data = await getAllDocumentsByPatientId(patient.id);
        setDocumentsLList(data);
        setSelectedDocument(Array(data.length).fill(false));
        handleSelectDocument(
          data[0].medicineListID + "|" + data[0].patientRef,
          0
        );
      } catch (error) {
        console.error("Error fetching documents:", error);
      }
    };

    fetchDocuments();
  }, [patient.id]);

  const handleSearchPatientsInputChange = (event) => {
    const value = event.target.value;

    setPatientName(event.target.value);
    if (!isEmpty(value)) {
      searchPatients(value.trim()).then((searchedPatients) => {
        setSearchedPatients((prevValues) => {
          return [...searchedPatients];
        });
      });
    } else {
      setSearchedPatients([{ id: 0, name: "" }]);
    }
  };

  const handleLocalSearchPatientsInputChange = (event) => {
    const value = event.target.value;

    setPatientName(event.target.value);
    if (!isEmpty(value)) {
      seachLocalPatients(value);
    }
  };

  const seachLocalPatients = (value) => {
    const filteredPatients = patients.filter((patient) =>
      patient.name.toLowerCase().includes(value.toLowerCase())
    );
    setLocalSearchedPatients(filteredPatients);
  };

  async function handleSearchedPatientClick(event) {
    setShowSuggestions(false);
    const patientId = event.currentTarget.dataset.patientid;
    const data = await getPatientById(patientId);
    setPatient(data);
  }

  async function handleDeleteDocument() {
    await deleteDocumentById(selectDocument.split("|")[0]);
    const data = await getAllDocumentsByPatientId(patient.id);

    setDocumentsLList(data);

    handleSelectDocument(
      documentsList[localStorage.getItem("selectedIndex") - 1].medicineListID +
        "|" +
        documentsList[localStorage.getItem("selectedIndex") - 1].patientRef,
      localStorage.getItem("selectedIndex")
    );

    const newSelectedDocumentArr = Array(data.length).fill(false);
    newSelectedDocumentArr[localStorage.getItem("selectedIndex") - 1] = true;
    setSelectedDocument(newSelectedDocumentArr);
  }

  function handleSetPresentation(event) {
    const selectedValue = event.target.value;
    if (selectedValue === "presentation1") {
      setPresentation(1);
    } else if (selectedValue === "presentation2") {
      setPresentation(2);
    }
  }

  const [patientIdOrder, setPatientIdOrder] = useState("byId:ASC");
  const [nameOrder, setNameOrder] = useState("byName:DESC");
  const [statusOrder, setStatusOrder] = useState("byStatus:DESC");
  const [doctorOrder, setDoctorOrder] = useState("byDoctor:ASC");
  const [roomOrder, setRoomOrder] = useState("byRoom:ASC");
  const [bedOrder, setBedOrder] = useState("byBed:ASC");

  function handlePatientIdSort() {
    getAllInpatients(patientIdOrder, residence).then((patients) => {
      return setPatients(patients);
    });

    patientIdOrder == "byId:ASC"
      ? setPatientIdOrder("byId:DESC")
      : setPatientIdOrder("byId:ASC");
  }

  function handlePatientsSort() {
    getAllInpatients(nameOrder, residence).then((patients) => {
      return setPatients(patients);
    });

    nameOrder == "byName:ASC"
      ? setNameOrder("byName:DESC")
      : setNameOrder("byName:ASC");
  }

  function handleStatusSort() {
    getAllInpatients(statusOrder, residence).then((patients) => {
      return setPatients(patients);
    });

    statusOrder == "byStatus:DESC"
      ? setStatusOrder("byStatus:ASC")
      : setStatusOrder("byStatus:DESC");
  }

  function handleDoctorSort() {
    getAllInpatients(doctorOrder, residence).then((patients) => {
      return setPatients(patients);
    });

    doctorOrder == "byDoctor:ASC"
      ? setDoctorOrder("byDoctor:DESC")
      : setDoctorOrder("byDoctor:ASC");
  }

  function handleRoomSort() {
    getAllInpatients(roomOrder, residence).then((patients) => {
      return setPatients(patients);
    });

    roomOrder == "byRoom:ASC"
      ? setRoomOrder("byRoom:DESC")
      : setRoomOrder("byRoom:ASC");
  }

  function handleBedSort() {
    getAllInpatients(bedOrder, residence).then((patients) => {
      return setPatients(patients);
    });

    bedOrder == "byBed:ASC"
      ? setBedOrder("byBed:DESC")
      : setBedOrder("byBed:ASC");
  }

  const highLightIds = useMemo(() => {
    const ids = [];

    patients.forEach((p) => {
      p.medicineListEditDates?.forEach((mled) => {
        try {
          if (
            isLessThanOneHour(
              isoToTimestampSeconds(formatDate2(mled).toISOString())
            )
          ) {
            ids.push(p.id);
          }
        } catch (error) {
          console.log(error);
        }
      });
    });

    return ids;
  }, [patients]);

  const [preparedMedicine, setPreparedMedicine] = useState([]);
  const [printSelectedDate, setPrintSelectedDate] = useState(
    new Date().toISOString().split("T")[0]
  );

  const [readyToPrint, setReadyToPrint] = useState(false);
  const printRef = useRef(null);

  const handlePrintDateChange = (event) => {
    setPrintSelectedDate(event.target.value);
  };

  async function handlePrepareMedicineToPrint() {
    const patientsInPrg = patients.filter(patient=>patient.residenceStatus=="PRG")
    const allItems = await Promise.all(
      patientsInPrg.map(async (patient) => {
        const allDocs = await getAllDocumentsByPatientId(patient.id);
        if (!allDocs[0]?.medicineListID) return [];

        const medicineRows = await getMedicineListById(
          allDocs[0].medicineListID
        );

        return medicineRows.medicineDetails
          .flatMap((med) =>
            med.medicineDetails.map((detail) => ({
              ...detail,
              medicineName: med.medicineName,
              medicineMethod: med.medicineMethod,
              roomNumber: patient.roomNumber,
              bedNumber: patient.bedNumber,
              patientName: patient.name,
            }))
          )
          .filter(
            (row) =>
              row.date === printSelectedDate &&
              (row.morning?.isPlanned && !row.morning?.isPlannedAndFinished ||
                row.day?.isPlanned && !row.day?.isPlannedAndFinished ||
                row.evening?.isPlanned && !row.evening?.isPlannedAndFinished ||
                row.night?.isPlanned && !row.night?.isPlannedAndFinished)
          );
      })
    );

    const flattenedItems = allItems.flat();
    const map = new Map();
    flattenedItems.forEach((item) => map.set(item.id, item));

    setPreparedMedicine(Array.from(map.values()));
    setReadyToPrint(true);
  }

  const handlePrint = useReactToPrint({
    contentRef: printRef,
    documentTitle: "Medicine List To Print",
  });

  useEffect(() => {
    if (readyToPrint && preparedMedicine.length > 0 && printRef.current) {
      handlePrint();
      setReadyToPrint(false);
    }
  }, [readyToPrint, preparedMedicine, handlePrint]);

  const presentationA = (
    <>
      <div style={{ display: "none" }}>
        {
          <MedicineListPrint
            ref={printRef}
            preparedMedicine={preparedMedicine}
          />
        }
      </div>
      <label>
        <input
          className="radio-input"
          type="radio"
          style={{ marginLeft: "10px", visibility: "hidden" }}
          name="presentation"
          value="presentation1"
          onChange={handleSetPresentation}
        />
        <span
          className={`venue ${residence === "19" && "underline"}`}
          onClick={() => handleResidenceClick("19")}
        >
          Реконструктивна хірургія{" "}
        </span>
        <span
          className={`venue ${residence === "37" && "underline"}`}
          onClick={() => handleResidenceClick("37")}
        >
          Реабілітація
        </span>
      </label>
      <div>
        <div>
          <meta charSet="UTF-8" />
          <meta
            name="viewport"
            content="width=device-width, initial-scale=1.0"
          />
          <title>Листок медичних призначень</title>
          <style>{`
body {
  font-family: Arial, sans-serif;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  border: 1px solid #ccc;
  padding: 6px;
  text-align: left;
}

th {
  background-color: #e0f0ff;
}

tr:nth-child(even) {
  background-color: #ccffcc;
}

tr:nth-child(odd) {
  /*background-color: #e6ecff;*/
  background-color: #ccffcc;
}

.header {
  background-color: #b0d0ff;
}

.highlight {
  background-color: #ccffcc !important;
  font-weight: bold;
}
`}</style>
          {presentation === 2 && (
            <div className="sidebar">
              <div className="search-container">
                <div>
                  <input
                    type="text"
                    placeholder="Пошук..."
                    id="search"
                    className="search-input"
                    name="search"
                    value={patientName}
                    onChange={handleSearchPatientsInputChange}
                    onFocus={() => setShowSuggestions(true)}
                    autoComplete="off"
                  />
                  {!isEmpty(patientName) && showSuggestions && (
                    <div className="searched-patients-dropdown">
                      {searchedPatients.map((patient, i) => (
                        <a
                          onClick={handleSearchedPatientClick}
                          data-patientid={patient.id}
                          href="#"
                          key={i}
                        >
                          {patient.name}
                        </a>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}
          <div
            className="content"
            style={presentation === 1 ? { marginLeft: "0" } : {}}
          >
            <h2>Список пацієнтів</h2>
            <div className="search-container ">
              <div>
                <input
                  type="text"
                  placeholder="Пошук..."
                  id="search"
                  className="local-patients-search-input"
                  name="search"
                  value={patientName}
                  onChange={handleLocalSearchPatientsInputChange}
                  autoComplete="off"
                />
                {!isEmpty(patientName) && showSuggestions && (
                  <div className="searched-patients-dropdown">
                    {searchedPatients.map((patient, i) => (
                      <a
                        onClick={handleSearchedPatientClick}
                        data-patientid={patient.id}
                        href="#"
                        key={i}
                      >
                        {patient.name}
                      </a>
                    ))}
                  </div>
                )}
              </div>
              {<button
                style={{ float: "right", margin: "5px"}}
                onClick={() => handlePrepareMedicineToPrint(patient.id)}
              >
                Роздрукувати призначення на день
              </button>}
              {<input
                style={{ float: "right", margin: "5px" }}
                type="date"
                value={printSelectedDate}
                onChange={(e) => handlePrintDateChange(e)}
              ></input>}
            </div>
            <table>
              <thead>
                <tr className="header">
                  <th onClick={handlePatientIdSort} className="sort">
                    Номер
                  </th>
                  <th onClick={handlePatientsSort} className="sort">
                    Пацієнт
                  </th>
                  <th onClick={handleRoomSort} className="sort">
                    Палата
                  </th>
                  <th onClick={handleBedSort} className="sort">
                    Ліжко
                  </th>
                  <th onClick={handleDoctorSort} className="sort">
                    Лікар
                  </th>
                  <th onClick={handleStatusSort} className="sort">
                    Статус
                  </th>
                </tr>
              </thead>
              <tbody>
                {!isEmpty(patientName)
                  ? localSearchedPatients.map((patient, i) => (
                      <tr
                        key={i}
                        style={
                          highLightIds.includes(patient.id)
                            ? { backgroundColor: "orange" }
                            : patient.residenceStatus === "CMP"
                            ? { backgroundColor: "lightgrey" }
                            : patient.residenceStatus === "PLN"
                            ? { backgroundColor: "#FAFAD2" }
                            : {}
                        }
                        className={
                          highLightIds.includes(patient.id) ? "blink_me" : ""
                        }
                      >
                        <td>{patient.id}</td>
                        <td>
                          {
                            <Link
                              data-patientid={patient.id}
                              to={`/patientdetails/${patient.id}`}
                              key={i}
                            >
                              {patient.name}
                            </Link>
                          }
                        </td>
                        <td>{patient.roomNumber}</td>
                        <td>{patient.bedNumber}</td>
                        <td>{patient.doctor}</td>
                        <td>
                          {patient.residenceStatus === "CMP"
                            ? "Завершено"
                            : patient.residenceStatus === "PLN"
                            ? "Заплановано"
                            : "В ході"}
                        </td>
                      </tr>
                    ))
                  : patients.map((patient, i) => (
                      <tr
                        key={i}
                        style={
                          highLightIds.includes(patient.id)
                            ? { backgroundColor: "orange" }
                            : patient.residenceStatus === "CMP"
                            ? { backgroundColor: "lightgrey" }
                            : patient.residenceStatus === "PLN"
                            ? { backgroundColor: "#FAFAD2" }
                            : {}
                        }
                        className={
                          highLightIds.includes(patient.id) ? "blink_me" : ""
                        }
                      >
                        <td>{patient.id}</td>
                        <td>
                          {
                            <Link
                              data-patientid={patient.id}
                              to={`/patientdetails/${patient.id}`}
                              key={i}
                            >
                              {patient.name}
                            </Link>
                          }
                        </td>
                        <td>{patient.roomNumber}</td>
                        <td>{patient.bedNumber}</td>
                        <td>{patient.doctor}</td>
                        <td>
                          {patient.residenceStatus === "CMP"
                            ? "Завершено"
                            : patient.residenceStatus === "PLN"
                            ? "Заплановано"
                            : "В ході"}
                        </td>
                      </tr>
                    ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  );
  const presentationB = (
    <>
      <div style={{ display: "none" }}>
        {
          <MedicineListPrint
            ref={printRef}
            preparedMedicine={preparedMedicine}
          />
        }
      </div>
      {showApproveModal && (
        <ApproveModal
          setShowApproveModal={setShowApproveModal}
          handleDeleteDocument={handleDeleteDocument}
        />
      )}
      <label>
        <input
          className="radio-input"
          type="radio"
          style={{ marginLeft: "10px", visibility: "hidden" }}
          name="presentation"
          value="presentation1"
          onChange={handleSetPresentation}
        />

        <span
          className={`venue ${residence === "19" && "underline"}`}
          onClick={() => handleResidenceClick("19")}
        >
          Реконструктивна хірургія{" "}
        </span>
        <span
          className={`venue ${residence === "37" && "underline"}`}
          onClick={() => handleResidenceClick("37")}
        >
          Реабілітація
        </span>
      </label>
      <div>
        <div>
          <meta charSet="UTF-8" />
          <meta
            name="viewport"
            content="width=device-width, initial-scale=1.0"
          />
          <title>Листок медичних призначень</title>
          <div className="sidebar">
            <div className="search-container">
              <div>
                <input
                  type="text"
                  placeholder="Пошук..."
                  id="search"
                  className="search-input"
                  name="search"
                  value={patientName}
                  onChange={handleSearchPatientsInputChange}
                  onFocus={() => setShowSuggestions(true)}
                  autoComplete="off"
                />
                {!isEmpty(patientName) && showSuggestions && (
                  <div className="searched-patients-dropdown">
                    {searchedPatients.map((patient, i) => (
                      <a
                        onClick={handleSearchedPatientClick}
                        data-patientid={patient.id}
                        href="#"
                        key={i}
                      >
                        {patient.name}
                      </a>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
  return presentation === 1 ? presentationA : presentationB;
}
