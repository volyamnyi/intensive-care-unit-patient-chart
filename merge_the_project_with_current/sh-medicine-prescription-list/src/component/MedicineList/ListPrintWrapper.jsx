import React from "react";
import ListPrint from "./ListPrint";

function chunkMedicineDetails(medicineList) {
  if (!medicineList?.medicineDetails) return [];

  let maxIndex = medicineList.medicineDetails[0]?.medicineDetails?.length;
  for(let i = 0; i< medicineList.medicineDetails.length;i++) {
    if(medicineList.medicineDetails[i]?.medicineDetails?.length > maxIndex) {
      maxIndex = medicineList.medicineDetails[i]?.medicineDetails?.length;
    }
  }

  const days = maxIndex;
  const chunks = [];

  for (let start = 0; start < days; start += 7) {
    const end = start + 7;

    const chunkedList = {
      ...medicineList,
      medicineDetails: medicineList.medicineDetails
        .map((medicine) => {
          const sliced = (medicine.medicineDetails || []).slice(start, end);

          const hasData = sliced.some((day) =>
            ["morning", "day", "evening", "night"].some(
              (part) => day[part]?.isPlanned || day[part]?.isCompleted
            )
          );

          if (!hasData) return null;

          return {
            ...medicine,
            medicineDetails: sliced,
          };
        })
        .filter(Boolean),
    };

    if (chunkedList.medicineDetails.length > 0) {
      chunks.push(chunkedList);
    }
  }

  return chunks;
}

const ListPrintWrapper = React.forwardRef(
  ({ medicineList }, ref) => {
    const weekChunks = chunkMedicineDetails(medicineList);

    const allPages = [];

    weekChunks.forEach((weekChunk, weekIndex) => {
      const medicines = weekChunk.medicineDetails || [];
      const rowsPerPage = 12;

      for (let i = 0; i < medicines.length; i += rowsPerPage) {
        const pageMedicines = medicines.slice(i, i + rowsPerPage);

        allPages.push({
          weekChunk: weekChunk || { medicineDetails: [] },
          weekIndex,
          pageMedicines,
          pageIndexWithinWeek: Math.floor(i / rowsPerPage),
        });
      }
    });
     const doctorUserName = JSON.parse(localStorage.getItem("patient"))?.doctorUserName?.split("\\")[1];

    return (
      <div ref={ref} style={{ paddingTop: "30px" }}>
        {allPages.length > 0 ? (
          <ListPrint
            allPages={allPages}
            medicineListCreationUser={doctorUserName}
          />
        ) : (
          <div
            style={{ padding: "50px", textAlign: "center", fontSize: "18px" }}
          >
            Немає даних для друку
          </div>
        )}
      </div>
    );
  }
);

export default ListPrintWrapper;
