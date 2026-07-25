import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getPatientById } from "../../utils/ApiFunctions";

export default function DocumentHeader(props) {
  const [patient, setPatient] = useState({
    id: 0,
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
  });

  useEffect(() => {
    getPatientById(props.id).then((patient) => {
      setPatient(patient);
      localStorage.setItem("patient", JSON.stringify(patient));
    });
  }, []);

  return (
    <div className="medicine-header flex">
      <div className="medicine-header-container flex">
        {
          <Link
            style={{ color: "#007bff", fontWeight: "bold" }}
            to={`/patientdetails/${props?.id}`}
          >
            Назад
          </Link>
        }
        <div className="flex">
          <div className="patinet-details-container">
            <ul>
              <li>
                ПІБ:
                <span className="patient-full-name">{patient.name}</span>
              </li>
              <li>
                Стать: <span className="address-heading">{patient.gender}</span>
              </li>
              <li>
                Вік: <span className="patient-age-row">{patient.age}</span>
              </li>
              <li>
                Дата народження:
                <span className="patient-birthdate-row">
                  {patient.birthDate}
                </span>
              </li>
            </ul>
          </div>
        </div>
        <div className="patinet-details-container">
          <ul>
            <li>
              Відділення:
              <span className="medical-unit">{patient.department}</span>
            </li>
            <li>
              Номер палати:
              <span className="room-number">{patient.roomNumber}</span>
            </li>
            <li>
              Номер ліжка:
              <span className="bed-number">{patient.bedNumber}</span>
            </li>
            <li>
              Лікар: <span className="doctor-name">{patient.doctor}</span>
            </li>
          </ul>
        </div>
      </div>
      <div className="flex flex-column">
        <div className="marks">
          Позначення: &nbsp;
          <div className="day-cell">
            <label
              className="day-cell-input-label"
              style={{ backgroundColor: "lightblue" }}
            ></label>{" "}
            &nbsp;-{" "}
            <span>
              {" "}
              &nbsp;<strong>Заплановано</strong>
            </span>
          </div>
          &nbsp;
          {
            <div className="day-cell">
              <label
                className="day-cell-input-label"
                style={{ backgroundColor: "purple" }}
              ></label>{" "}
              &nbsp;-{" "}
              <span>
                {" "}
                &nbsp;<strong>Відмінено</strong>
                <br />
                <strong>
                  (Середня кнопка мишки <br />
                  по голубому)
                </strong>
              </span>
            </div>
          }
          &nbsp;
          <div className="day-cell">
            <label
              className="day-cell-input-label"
              style={{ backgroundColor: "lightgreen" }}
            ></label>{" "}
            &nbsp;- <span> &nbsp;Виконано&nbsp;</span>
          </div>
          {/*<div className="day-cell">
          <label
            className="day-cell-input-label"
            style={{ backgroundColor: "darkgreen" }}
          ></label>{" "}
          &nbsp;-{" "}
          <span>
            {" "}
            &nbsp;<strong>Виконано і завершено</strong>
            <br />
            <strong>
              (Середня кнопка мишки <br />
              по фіолетовому)
            </strong>
          </span>
        </div>*/}
          {/*&nbsp;
        <div className="day-cell">
          <label
            className="day-cell-input-label"
            style={{ backgroundColor: "#fff" }}
          ></label>{" "}
          &nbsp;- <span> &nbsp;Не заплановано</span>
        </div>
        &nbsp;*/}
          <div className="day-cell">
            <label
              className="day-cell-input-label"
              style={{ backgroundColor: "rgb(255, 204, 203)" }}
            ></label>{" "}
            &nbsp;<span>-&nbsp;Препарат високого ризику (фон)</span>
          </div>
          &nbsp;
          <div className="day-cell">
            <label
              className="day-cell-input-label"
              style={{ backgroundColor: "rgb(255, 230, 128)" }}
            ></label>{" "}
            &nbsp;<span>-&nbsp;Небезпечна взаємодія (мигання фон)</span>
          </div>
          &nbsp;
          {/*<div className="day-cell">
          <span>шб. - Шкала болю</span>
        </div>*/}
        </div>
        <div className="marks flex flex-column" style={{ marginTop: "50px" }}>
          Підказки: 
          <a href="https://bz.superhumans.com/pages/viewpage.action?pageId=126158854" target="_blank">АМП [Формуляр]</a>
          <a href="https://bz.superhumans.com/pages/viewpage.action?pageId=88506877" target="_blank">Адміністрування АМП [СОП]</a>
          <a href="https://bz.superhumans.com/pages/viewpage.action?pageId=126157554" target="_blank">Адміністрування АМП [Політика]</a>
          <a href="https://bz.superhumans.com/pages/viewpage.action?pageId=126157964" target="_blank">Периопераційна профілактика [СОП]</a>
        </div>
      </div>
    </div>
  );
}
