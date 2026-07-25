import React from "react";

const MedicineListPrint = React.forwardRef(({ preparedMedicine }, ref) => {
  const patientsByLastName = preparedMedicine.reduce((acc, patient) => {
    const lastName = patient.patientName || "Без прізвища";

    if (!acc[lastName]) acc[lastName] = [];
    acc[lastName].push(patient);

    return acc;
  }, {});
  return (
    <>
      <style>{`
        @media print {
          @page {
            size: A4;
            margin: 12mm 10mm;
          }

          html, body {
            margin: 0;
            padding: 0;
          }

          .page {
            padding: 0;
            font-family: system-ui, -apple-system, sans-serif;
            background: white;
            color: #000;
          }

          h1 {
            text-align: center;
            margin: 0 0 24px 0;
            font-size: 1.6rem;
            page-break-after: avoid;
          }

          h2 {
            margin: 1.4em 0 0.8em;
            font-size: 1.3rem;
            color: #2b6cb0;
            page-break-after: avoid;
          }

          /* ─── The most important rule ─── */
          .patient-card {
            background: #fff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            padding: 16px;
            margin-bottom: 24px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.06);

            /* Prevent splitting this block across pages */
            break-inside: avoid;
            page-break-inside: avoid;

            /* Try to keep header with content */
            break-before: avoid;
            break-after: avoid-column; /* helpful when printing in columns */
          }

          .patient-header {
            margin-bottom: 12px;
            padding-bottom: 8px;
            border-bottom: 1px solid #cbd5e1;
            break-inside: avoid;
          }

          table {
            width: 100%;
            border-collapse: collapse;
            font-size: 0.95rem;
          }

          th {
            text-align: left;
            color: #4a5568;
            padding: 8px 4px;
            border-bottom: 2px solid #e2e8f0;
            background: #f7fafc;
          }

          td {
            padding: 8px 4px;
            vertical-align: top;
            border-bottom: 1px solid #edf2f7;
          }

          tr {
            break-inside: avoid;
            page-break-inside: avoid;
          }

          /* Avoid orphan rows at the top of a new page */
          tbody tr:last-child {
            break-after: avoid;
          }

          /* Force page break before new room if needed */
          h2 {
            break-before: avoid-column;
          }
        }

        /* Screen styles (non-print) */
        .page {
          padding: 20px;
          font-family: system-ui;
          background: #f4f6f8;
          color: #2d3748;
        }

        h1 {
          text-align: center;
          margin-bottom: 30px;
        }

        .patient-card {
          background: #fff;
          border-radius: 10px;
          padding: 16px;
          box-shadow: 0 6px 20px rgba(0,0,0,0.08);
          margin-bottom: 20px;
        }

        .patient-header {
          border-bottom: 1px solid #e2e8f0;
          padding-bottom: 10px;
          margin-bottom: 10px;
          font-weight: 900;
        }

        .page .room,
        .page .bed {
          color: #2b6cb0;
          font-weight: 900;
        }

        .page .name {
          font-size: 1.1rem;
        }

        
        table {
          width: 100%;
          border-collapse: collapse;
        }

        .page th {
          text-align: center;
          color: #718096;
          padding: 6px 0;
          background: #fff;
          font-weight: 900;
        }

        .page td {
          text-align: center;
          padding: 6px 0;
          vertical-align: top;
          background: #fff;
          font-weight: 900;
        }

        tr:not(:last-child) td {
          border-bottom: 1px dashed #e2e8f0;
        }
      `}</style>

      <div className="page" ref={ref}>
        <h1>Лист призначень пацієнтів</h1>
        <h2 style={{ fontWeight: "900", textAlign: "center" }}>
          {preparedMedicine[0]?.date}
        </h2>
        {Object.entries(patientsByLastName).map(([lastName, patients]) => (
          <div key={lastName}>
            <div className="patient-card">
              <table>
                <thead>
                  <tr>
                    <th>
                      <div className="patient-header">
                        <div className="name" style={{ fontWeight: 900 }}>
                          {lastName}
                        </div>
                      </div>
                      Препарат
                    </th>
                    <th>Час</th>
                    <th>Примітка</th>
                  </tr>
                </thead>

                <tbody>
                  {patients.map((med, index) => (
                    <tr key={index}>
                      <td>{med.medicineName || "—"}</td>
                      <td>
                        {[
                          !med.morning?.isPlannedAndFinished && med.morning?.isPlanned && "Ранок",
                          !med.day?.isPlannedAndFinished && med.day?.isPlanned && "День",
                          !med.evening?.isPlannedAndFinished && med.evening?.isPlanned && "Вечір",
                          !med.night?.isPlannedAndFinished && med.night?.isPlanned && "Ніч",
                        ]
                          .filter(Boolean)
                          .join(", ") || "—"}
                      </td>
                      <td>{med.medicineMethod || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ))}
      </div>
    </>
  );
});

export default MedicineListPrint;
