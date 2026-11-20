import React from "react";

const ListPrint = React.forwardRef(
  ({ allPages, medicineListCreationUser }, ref) => {
    const { weekChunk, pageMedicines } = allPages;
    const patient = JSON.parse(localStorage.getItem("patient") || "{}");

    const dayIndices = [0, 1, 2, 3, 4, 5, 6];
    const dayParts = ["Ранок", "День", "Вечір", "Ніч"];
    const dayPartsEnglish = ["morning", "day", "evening", "night"];
    const safeWeekChunk = weekChunk || { medicineDetails: [] };
    const details = safeWeekChunk.medicineDetails?.[0]?.medicineDetails || [];
    const allDetails = (() => {
      const minLength = 7;
      const extraNeeded = Math.max(0, minLength - details.length);
      const lastDate = details.length
        ? new Date(details[details.length - 1].date)
        : new Date();

      const generated = Array.from({ length: extraNeeded }, (_, idx) => {
        const d = new Date(lastDate);
        d.setDate(d.getDate() + idx + 1);
        return { date: d.toISOString().split("T")[0] };
      });

      return [...details, ...generated];
    })();

    if (!allPages || allPages.length === 0) {
      return (
        <div
          style={{ padding: "100px", textAlign: "center", fontSize: "20px" }}
        >
          Немає даних для друку
        </div>
      );
    }

    return (
      <>
        <style>
          {`
          .flex { display: flex; }
          .flex-column { flex-direction: column; }
          .bold { font-weight: bold; }

          .table { width: 1620px; font-size: 14px; }
          .table-cell {
            border: 1px solid black;
            padding: 5px;
            display: flex;
            justify-content: center;
            align-items: center;
            text-align: center;
          }
          .num-cell { border: 1px solid black; width: 30px; height: 20px; }

          .table-body .table-cell { font-size: 13px; }
          .table-body .date-cell { font-size: 11px; }

          @media print {
            @page { margin: 20px; size: A4 landscape; }
            .print-container { padding: 0; }
          }
        `}
        </style>

        <div className="print-container">
          <div className="table" ref={ref}>
            {allPages.map((pageData, pageIndex) => {
              if (!pageData) {
                console.error("Invalid pageData at index", pageIndex);
                return null;
              }

              const weekChunk = pageData.weekChunk || { medicineDetails: [] };
              const pageMedicines = pageData.pageMedicines || [];

              const details =
                weekChunk.medicineDetails?.[0]?.medicineDetails || [];

              const allDetails = (() => {
                const minLength = 7;
                const extraNeeded = Math.max(0, minLength - details.length);
                const lastDate =
                  details.length > 0
                    ? new Date(details[details.length - 1].date)
                    : new Date();

                const generated = Array.from(
                  { length: extraNeeded },
                  (_, idx) => {
                    const d = new Date(lastDate);
                    d.setDate(d.getDate() + idx + 1);
                    return { date: d.toISOString().split("T")[0] };
                  }
                );

                return [...details, ...generated];
              })();

              return (
                <div
                  key={pageIndex}
                  style={{
                    pageBreakBefore: pageIndex === 0 ? "auto" : "always",
                    breakInside: "avoid",
                  }}
                >
                  {/* HEADER */}
                  <div className="table-header">
                    <div className="table-row flex">
                      <div className="left-header flex flex-column">
                        <div
                          className="table-cell bold"
                          style={{ height: 60, width: 730 }}
                        >
                          МОЗ України <br /> Департамент охорони здоровʼя
                        </div>
                        <div
                          className="table-cell bold"
                          style={{ height: 65, width: 730 }}
                        >
                          Superhumans Львів
                        </div>
                        <div
                          className="table-cell"
                          style={{
                            height: 40,
                            width: 730,
                            justifyContent: "space-between",
                            padding: 5,
                          }}
                        >
                          Ідентифікаційний код ЄДРПОУ
                          <div className="num-cell-container flex">
                            {[4, 4, 8, 0, 3, 5, 9, 7].map((n, i) => (
                              <div key={i} className="num-cell bold">
                                {n}
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                      <div
                        className="table-cell"
                        style={{ height: 165, width: 350 }}
                      >
                        <img
                          src="/Superhumans_logo_black.png"
                          width={300}
                          alt="Logo"
                        />
                      </div>
                      <div className="right-header flex flex-column">
                        <div
                          className="table-cell bold"
                          style={{ height: 40, width: 540 }}
                        >
                          МЕДИЧНА ДОКУМЕНТАЦІЯ
                        </div>
                        <div
                          className="table-cell bold"
                          style={{ height: 50, width: 540 }}
                        >
                          Форма первинної облікової документації <br />№ 003-4/о
                        </div>
                        <div
                          className="table-cell bold"
                          style={{ height: 75, width: 540 }}
                        >
                          ЗАТВЕРДЖЕНО
                          <br />
                          Наказ МОЗ України
                          <br />
                          29.05.2013 № 435
                        </div>
                      </div>
                    </div>

                    <div className="table-row flex">
                      <div
                        className="table-cell flex-column"
                        style={{ width: 390, height: 60 }}
                      >
                        Номер медичної картки
                        <br />
                        <span className="flex">
                          стаціонарного хворого &nbsp;
                          <span className="bold">{weekChunk?.patientRef}</span>
                        </span>
                      </div>
                      <div
                        className="table-cell flex-column"
                        style={{ width: 880 }}
                      >
                        <span>ЛИСТОК ЛІКАРСЬКИХ ПРИЗНАЧЕНЬ</span>
                        <br />
                        <span className="flex">
                          <span>Прізвище, ім’я, по батькові хворого&nbsp;</span>
                          <span className="bold">{patient?.name}</span>
                        </span>
                      </div>
                      <div
                        className="table-cell"
                        style={{ width: 350, justifyContent: "left" }}
                      >
                        Номер палати&nbsp;
                        <span className="bold">{patient?.roomNumber}</span>
                      </div>
                    </div>

                    <div className="table-row flex">
                      <div
                        className="table-cell"
                        style={{ width: 1620, height: 30 }}
                      >
                        Відмітка про призначення та виконання
                      </div>
                    </div>
                  </div>

                  {/* BODY */}
                  <div className="table-body flex flex-column">
                    <div className="table-row flex">
                      <div
                        className="table-cell"
                        style={{ width: 250, height: 45 }}
                      >
                        Призначення
                      </div>
                      <div
                        className="table-cell"
                        style={{ width: 65, height: 45 }}
                      >
                        Виконання
                      </div>
                      <div
                        className="table-cell"
                        style={{ width: 45, height: 45 }}
                      >
                        Дата
                      </div>
                      {allDetails.map((d, i) => {
                        const formatted = d.date
                          .split("-")
                          .reverse()
                          .map((p, idx) => (idx === 2 ? p.slice(2) : p))
                          .join(".");
                        return (
                          <React.Fragment key={i}>
                            {dayParts.map((part, pIdx) => (
                              <div className="date-cell-container" key={pIdx}>
                                <div
                                  className="table-cell date-cell"
                                  style={{ width: 45, height: 20 }}
                                >
                                  {formatted}
                                </div>
                                <div
                                  className="table-cell date-cell"
                                  style={{ width: 45, height: 25 }}
                                >
                                  {part}
                                </div>
                              </div>
                            ))}
                          </React.Fragment>
                        );
                      })}
                    </div>

                    {/* Regime row */}
                    <div className="table-row flex">
                      <div
                        className="table-cell"
                        style={{
                          width: 250,
                          height: 20,
                          justifyContent: "left",
                        }}
                      >
                        Режим
                      </div>
                      <div
                        className="table-cell"
                        style={{ width: 65, height: 20 }}
                      />
                      <div
                        className="table-cell"
                        style={{ width: 45, height: 20 }}
                      />
                      {Array.from({ length: 28 }, (_, i) => (
                        <div
                          key={i}
                          className="table-cell"
                          style={{ width: 45, height: 20 }}
                        />
                      ))}
                    </div>

                    {/* 12 Medicine rows */}
                    {Array.from({ length: 12 }, (_, rowIndex) => {
                      const medicine = pageMedicines[rowIndex];
                      const isPlaceholder = !medicine;

                      return (
                        <div className="table-row flex" key={rowIndex}>
                          <div
                            className="table-cell"
                            style={{
                              width: 250,
                              height: 60,
                              justifyContent: "left",
                              paddingLeft: 10,
                            }}
                          >
                            {isPlaceholder ? "" : medicine?.medicineName || ""}
                          </div>
                          <div className="doctor-nurse-container">
                            <div
                              className="table-cell"
                              style={{ width: 65, height: 30 }}
                            >
                              {"Лікар"}
                            </div>
                            <div
                              className="table-cell"
                              style={{ width: 65, height: 30 }}
                            >
                              {"Сестра"}
                            </div>
                          </div>
                          <div className="doctor-nurse-container">
                            <div
                              className="table-cell"
                              style={{ width: 45, height: 30 }}
                            />
                            <div
                              className="table-cell"
                              style={{ width: 45, height: 30 }}
                            />
                          </div>
                          {dayIndices.map((dayIndex) => (
                            <React.Fragment key={dayIndex}>
                              {dayPartsEnglish.map((part) => {
                                const cell = isPlaceholder
                                  ? null
                                  : medicine?.medicineDetails?.[dayIndex]?.[
                                      part
                                    ];
                                return (
                                  <div
                                    className="doctor-nurse-container"
                                    key={part}
                                  >
                                    <div
                                      className="table-cell"
                                      style={{ width: 45, height: 30 }}
                                    >
                                      {cell?.isPlanned ? "+" : ""}
                                    </div>
                                    <div
                                      className="table-cell"
                                      style={{ width: 45, height: 30 }}
                                    >
                                      {cell?.isCompleted ? "+" : ""}
                                    </div>
                                  </div>
                                );
                              })}
                            </React.Fragment>
                          ))}
                        </div>
                      );
                    })}
                    <div className="table-row flex">
                      <div
                        className="table-cell"
                        style={{
                          width: 250,
                          height: 40,
                          justifyContent: "left",
                          paddingLeft: 10,
                        }}
                      >
                        Підписи
                      </div>
                      <div className="doctor-nurse-container">
                        <div
                          className="table-cell"
                          style={{ width: 65, height: 20 }}
                        >
                          Лікар
                        </div>
                        <div
                          className="table-cell"
                          style={{ width: 65, height: 20 }}
                        >
                          Сестра
                        </div>
                      </div>
                      <div className="doctor-nurse-container">
                        <div
                          className="table-cell"
                          style={{ width: 45, height: 20 }}
                        />
                        <div
                          className="table-cell"
                          style={{ width: 45, height: 20 }}
                        />
                      </div>

                      {(() => {
                        const cells = [];
                        const medicines = pageMedicines || [];
                        const doctorName = medicineListCreationUser || "";

                        for (let day = 0; day < 7; day++) {
                          for (const part of dayPartsEnglish) {
                            const hasCompletion = medicines.some(
                              (med) =>
                                med?.medicineDetails?.[day]?.[part]
                                  ?.isCompleted === true
                            );

                            let nurseName = "";
                            if (hasCompletion) {
                              for (const med of medicines) {
                                const name =
                                  med?.medicineDetails?.[day]?.[part]
                                    ?.nurseName;
                                if (
                                  typeof name === "string" &&
                                  name.trim() !== ""
                                ) {
                                  nurseName = name.trim();
                                  break;
                                }
                              }
                            }

                            cells.push(
                              <div
                                className="doctor-nurse-container"
                                key={`sig-${day}-${part}`}
                              >
                                <div
                                  className="table-cell"
                                  style={{ width: 45, height: 20, fontSize: 7 }}
                                >
                                  {doctorName}
                                </div>
                                <div
                                  className="table-cell"
                                  style={{ width: 45, height: 20, fontSize: 7 }}
                                >
                                  {nurseName}
                                </div>
                              </div>
                            );
                          }
                        }

                        return cells;
                      })()}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </>
    );
  }
);
export default ListPrint;
