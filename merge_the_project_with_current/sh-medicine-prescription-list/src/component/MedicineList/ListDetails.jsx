import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import DocumentHeader from "./DocumentHeader";
import List from "./List";
import VitalList from "./VitalList";
import SuccessModal from "./SuccessModal";

import {
  getMedicineListById,
  updateMedicineListById,
  addNewMedicineList,
  updateMedicineListStatusByListId,
  isDocumentEditing,
  searchMedicine,
} from "../../utils/ApiFunctions";

import {
  //handleItemChange,
  handleDetailChange,
  handleAddNewMedicineItem,
  handleRemoveMedicineItem,
  handleAddNewDayDetails,
  handleDelNewDayDetails,
  handleMedicineMethodChange,
  isEmpty,
  handleSubmit,
  getWeekDates,
  formatDate,
  handleSearchedMedicineClick,
  handleCurrentRowClick,
  handleMedicineRegimeChange,
  handleVitalChange,
} from "../../utils/Functions";

export default function ListDetails(props) {
  const { Id } = props;
  const id = Id ? Id : useParams().id;
  const isScaled = Id ? true : false;
  const [isNew, setIsNew] = useState(false);

  const [ROLE, setROLE] = useState(localStorage.getItem("businessRole"));
  const sub = localStorage.getItem("sub");
  const [errorMessage, setErrorMessage] = useState("");

  const [medicineList, setMedicineList] = useState({
    medicineListID: null,
    patientRef: null,
    documentName: "",
    medicineListCreationUser: sub,
    medicineListCreationDate: null,
    medicineDetails: [],
    approvedRowIndexes: [],
  });
  const [vitalList, setVitalList] = useState();

  const [medicineDetails, setMedicineListItem] = useState([]);
  const [triggerSubmit, setTriggerSubmit] = useState(false);
  const [dates, setDates] = useState([]);

  const [showSuggestions, setShowSuggestions] = useState(false);
  const [currentRowSuggestionIndex, setCurrentRowSuggestionIndex] = useState();
  const [medicineName, setMedicineName] = useState("");
  const [triggerSearchedMedicine, setTriggerSearchedMedicine] = useState(false);
  const [searchedMedicine, setSearchedMedicine] = useState([
    { id: "", name: "", categoryRef: "", PTG: "" },
  ]);

  const [showSuccessModal, setShowSuccessModal] = useState(false);

  useEffect(() => {
    getMedicineListById(id.split("|")[0]).then((list) => {
      setMedicineList(list);
      setMedicineListItem(medicineList.medicineDetails);
      setVitalList(medicineList.vitalList);
      setDates(getWeekDates(formatDate(list)));
    });
  }, [medicineList.medicineListID]);

  useEffect(() => {
    if (triggerSubmit) {
      (async () => {
        let response;
        if (props.isCopy) {
          medicineList.medicineListCreationUser = sub;

          medicineList.medicineListCreationDate = new Date();
          medicineList.medicineDetails.map((md1) => {
            md1.medicineDetails.map((md2) => {
              md2.morning.isCompleted = false;
              //md2.morning.pain = "";
              md2.morning.doctorName = "";
              md2.morning.nurseName = "";
              md2.day.isCompleted = false;
              //md2.day.pain = "";
              md2.day.doctorName = "";
              md2.day.nurseName = "";
              md2.evening.isCompleted = false;
              // md2.evening.pain = "";
              md2.evening.doctorName = "";
              md2.evening.nurseName = "";
              md2.night.isCompleted = false;
              // md2.night.pain = "";
              md2.night.doctorName = "";
              md2.night.nurseName = "";
            });
          });

          response = await addNewMedicineList(
            medicineList,
            JSON.parse(localStorage.getItem("patient")),
          );
        } else {
          response = await updateMedicineListById(
            medicineList,
            JSON.parse(localStorage.getItem("patient")),
          );
        }

        if (response.status === 200 || response.status === 201) {
          setShowSuccessModal(true);
        } else {
          alert(response);
        }
        setTriggerSubmit(false);
      })();
    }
  }, [medicineList, triggerSubmit]);

  useEffect(() => {
    if (triggerSearchedMedicine) {
      (async () => {
        if (!isEmpty(medicineName)) {
          searchMedicine(medicineName.trim()).then((searchedMedicine) => {
            setSearchedMedicine(() => {
              return [...searchedMedicine];
            });
          });
        } else {
          setSearchedMedicine([{ id: "", name: "", categoryRef: "", PTG: "" }]);
        }
      })();
    }
  }, [medicineName]);

  const [renderCount, setRenderCount] = useState(0);
  useEffect(() => {
    if (renderCount < 1) {
      setRenderCount((prevCount) => prevCount + 1);
    }
  }, []);

  useEffect(() => {
    !isScaled && updateMedicineListStatusByListId(id.split("|")[0], sub);

    if (!isScaled) {
      (async () => {
        const result = await isDocumentEditing(id.split("|")[0]);
        if (result.status === 409) {
          setErrorMessage(result.response.data.message);
          if (ROLE === "DOCTOR") {
            setROLE("BLOCKED_DOCTOR");
          } else if (ROLE === "NURSE") {
            setROLE("BLOCKED_NURSE");
          }
        } else {
          if (ROLE === "BLOCKED_DOCTOR") {
            setROLE("DOCTOR");
          } else if (ROLE === "BLOCKED_NURSE") {
            setROLE("NURSE");
          }
        }
      })();
    }

    return () => {
      updateMedicineListStatusByListId(id.split("|")[0], "Saved");
    };
  }, [renderCount]);

  useEffect(() => {
    const handlePopState = () => {
      updateMedicineListStatusByListId(id.split("|")[0], "Saved");
    };

    window.addEventListener("popstate", handlePopState);

    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, [id.split("|")[0]]);

  useEffect(() => {
    const handleBeforeUnload = () => {
      updateMedicineListStatusByListId(id.split("|")[0], "Saved");
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [id.split("|")[0]]);

  return (
    <>
      {showSuccessModal && (
        <SuccessModal
          setShowSuccessModal={setShowSuccessModal}
          isSaved={true}
        />
      )}
      {errorMessage && <p style={{ color: "red" }}>{errorMessage}</p>}
      {!isScaled && <DocumentHeader id={id.split("|")[1]} />}
      {medicineList.documentName ===
      "Листок лікарських призначень (стаціонар)" ? (
        <List
          isNew={isNew}
        patientId={id.split("|")[1]}
        documentId={id.split("|")[0]}
        isCopy={props.isCopy}
        setIsNew={setIsNew}
        //handleItemChange={handleItemChange}
        handleDetailChange={handleDetailChange}
        handleAddNewMedicineItem={handleAddNewMedicineItem}
        handleRemoveMedicineItem={handleRemoveMedicineItem}
        handleAddNewDayDetails={handleAddNewDayDetails}
        handleDelNewDayDetails={handleDelNewDayDetails}
        handleMedicineMethodChange={handleMedicineMethodChange}
        handleSubmit={handleSubmit}
        handleSearchedMedicineClick={handleSearchedMedicineClick}
        handleCurrentRowClick={handleCurrentRowClick}
        dates={dates}
        medicineList={medicineList}
        medicineDetails={medicineDetails}
        currentRowSuggestionIndex={currentRowSuggestionIndex}
        ROLE={ROLE}
        setMedicineListItem={setMedicineListItem}
        setSearchedMedicine={setSearchedMedicine}
        isEmpty={isEmpty}
        setMedicineList={setMedicineList}
        setTriggerSubmit={setTriggerSubmit}
        setShowSuggestions={setShowSuggestions}
        setCurrentRowSuggestionIndex={setCurrentRowSuggestionIndex}
        setMedicineName={setMedicineName}
        searchMedicine={searchMedicine}
        setTriggerSearchedMedicine={setTriggerSearchedMedicine}
        showSuggestions={showSuggestions}
        triggerSearchedMedicine={triggerSearchedMedicine}
        searchedMedicine={searchedMedicine}
        isScaled={isScaled}
        handleMedicineRegimeChange={handleMedicineRegimeChange}
        handleVitalChange={handleVitalChange}
        vitalList={vitalList}
        setVitalList={setVitalList}
        />
      ) : (
        <VitalList
        isNew={isNew}
        patientId={id.split("|")[1]}
        isCopy={props.isCopy}
        setIsNew={setIsNew}
        //handleItemChange={handleItemChange}
        handleDetailChange={handleDetailChange}
        handleAddNewMedicineItem={handleAddNewMedicineItem}
        handleRemoveMedicineItem={handleRemoveMedicineItem}
        handleAddNewDayDetails={handleAddNewDayDetails}
        handleDelNewDayDetails={handleDelNewDayDetails}
        handleMedicineMethodChange={handleMedicineMethodChange}
        handleSubmit={handleSubmit}
        handleSearchedMedicineClick={handleSearchedMedicineClick}
        handleCurrentRowClick={handleCurrentRowClick}
        dates={dates}
        medicineList={medicineList}
        medicineDetails={medicineDetails}
        currentRowSuggestionIndex={currentRowSuggestionIndex}
        ROLE={ROLE}
        setMedicineListItem={setMedicineListItem}
        setSearchedMedicine={setSearchedMedicine}
        isEmpty={isEmpty}
        setMedicineList={setMedicineList}
        setTriggerSubmit={setTriggerSubmit}
        setShowSuggestions={setShowSuggestions}
        setCurrentRowSuggestionIndex={setCurrentRowSuggestionIndex}
        setMedicineName={setMedicineName}
        searchMedicine={searchMedicine}
        setTriggerSearchedMedicine={setTriggerSearchedMedicine}
        showSuggestions={showSuggestions}
        triggerSearchedMedicine={triggerSearchedMedicine}
        searchedMedicine={searchedMedicine}
        isScaled={isScaled}
        handleMedicineRegimeChange={handleMedicineRegimeChange}
        handleVitalChange={handleVitalChange}
        vitalList={vitalList}
        setVitalList={setVitalList}
        />
      )}
    </>
  );
}
