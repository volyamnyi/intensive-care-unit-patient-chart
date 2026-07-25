import { useState, useEffect } from "react";
import { searchPatients } from "../../utils/ApiFunctions";
import { getPatientById } from "../../utils/ApiFunctions";
import { getAllDocumentsByPatientId } from "../../utils/ApiFunctions";
import { deleteDocumentById } from "../../utils/ApiFunctions";
import { Link, useParams } from "react-router-dom";
import { isEmpty } from "../../utils/Functions";
import { formatDate } from "../../utils/Functions";

import ListDetails from "./ListDetails";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faTrash } from "@fortawesome/free-solid-svg-icons";
import ApproveModal from "./ApproveModal";

export default function PatientDetails() {
  const { id } = useParams();
  const [patientStateId, setPatientStateId] = useState(id);

  const ROLE = localStorage.getItem("businessRole");

  const [showApproveModal, setShowApproveModal] = useState(false);

  const [documentsList, setDocumentsLList] = useState([
    /*
    {
      medicineListID: null,
      patientRef: null,
      documentName: "",
      medicineListCreationUser: "",
      medicineListCreationDate: null,
      medicineDetails: [],
      approvedRowIndexes: [],
    },
  */
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

  const sub = localStorage.getItem("sub");

  const doctorUserName = patient?.doctorUserName?.split("\\")[1]

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
          0,
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
        setSearchedPatients(() => {
          return [...searchedPatients];
        });
      });
    } else {
      setSearchedPatients([{ id: 0, name: "" }]);
    }
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
      localStorage.getItem("selectedIndex"),
    );

    const newSelectedDocumentArr = Array(data.length).fill(false);
    newSelectedDocumentArr[localStorage.getItem("selectedIndex") - 1] = true;
    setSelectedDocument(newSelectedDocumentArr);
  }

  return (
    <>
      {showApproveModal && (
        <ApproveModal
          setShowApproveModal={setShowApproveModal}
          handleDeleteDocument={handleDeleteDocument}
        />
      )}
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
          <div className="content">
            {
              <Link style={{ color: "#007bff", fontWeight: "bold" }} to={`/`}>
                Головна
              </Link>
            }
            {patient.id && (
              <>
                <div className="profile">
                  <h2>
                    <span>
                      [{patient.id}] {patient.name}
                    </span>
                  </h2>
                  <p>
                    <span>{patient.historyNumber}</span>
                  </p>
                  <p>
                    Адреса: <span>{patient.address} </span>
                  </p>
                  <p>
                    Тел.: <span>{patient.phone}</span>
                  </p>
                  <p>
                    Відділення: <span>{patient.department}</span>
                  </p>
                  <p>
                    Номер палати: <span>{patient.roomNumber}</span>
                  </p>
                  <p>
                    Ліжко: <span>{patient.bedNumber}</span>
                  </p>
                  <p>
                    Лікар: <span>{patient.doctor}</span>
                  </p>
                  <p>
                    Стать: <span>{patient.gender}</span>
                  </p>
                  <p>
                    Вік: <span>{patient.age}</span>
                  </p>
                  <p>
                    Дата народження: <span>{patient.birthDate}</span>
                  </p>
                </div>
                <div style={{ display: "flex", flexDirection: "row" }}>
                  <div className="documents">
                    <h3>Список документів пацієнта</h3>
                    {documentsList.map((document, i) => (
                      <a
                        href="#"
                        key={i}
                        onClick={() => {
                          handleSelectDocument(
                            document.medicineListID + "|" + document.patientRef,
                            i,
                          );
                        }}
                      >
                        <div
                          className={`document-item ${
                            selectedDocument[i] ? "selected" : ""
                          }`}
                          key={i}
                        >
                          <p>
                            {document.medicineListCreationDate &&
                              formatDate(document).toLocaleDateString()}{" "}
                            - {document.medicineListCreationUser}
                          </p>
                          <p>{document.documentName}</p>
                        </div>
                      </a>
                    ))}
                    {ROLE === "DOCTOR" &&
                      /*sub === doctorUserName */ (documentsList.filter(
                        (d) =>
                          d.documentName ===
                          "Листок лікарських призначень (стаціонар)",
                      ).length < 1 ||
                        sub == "v.yamnyi" || doctorUserName == sub) && (
                        <Link
                          to={`/newlist/${patient.id}/Листок лікарських призначень (стаціонар)`}
                        >
                          Створити Лист Призначень
                        </Link>
                      )}
                    <br />
                    {(ROLE === "NURSE" || sub === "v.yamnyi") && (
                        <Link
                          to={`/newlist/${patient.id}/Листок життєвих показників (стаціонар)`}
                        >
                          Створити Лист Показників
                        </Link>
                      )}
                  </div>
                  {documentsList.length > 0 && (
                    <div className="document-preview-container">
                      <div className="buttons">
                        <Link to={`/listdetails/${selectDocument}`}>
                          <button className="edit-button">Редагувати</button>
                        </Link>
                        {/*ROLE == "DOCTOR" && (
                          <Link to={`/listdetails/copy/${selectDocument}`}>
                            <button className="edit-button">Копіювати</button>
                          </Link>
                        )*/}
                        {/*sub === doctorUserName || */ sub === "v.yamnyi" && (
                          <div
                            className="trash-ico-container"
                            style={{ marginTop: "3px" }}
                          >
                            <FontAwesomeIcon
                              icon={faTrash}
                              onClick={() => {
                                setShowApproveModal(true);
                              }}
                            />
                          </div>
                        )}
                      </div>
                      <div style={{ scale: 0.7 }} className="scroll-both">
                        {
                          <ListDetails
                            key={selectDocument}
                            Id={selectDocument}
                          />
                        }
                      </div>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </>
  );
}
