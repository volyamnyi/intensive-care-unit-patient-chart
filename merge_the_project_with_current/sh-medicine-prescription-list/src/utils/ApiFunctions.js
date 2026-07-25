import axios from "axios";
import { isTokenExpired } from "./Functions";

export const api = axios.create({
  //baseURL: "http://localhost:8080",
  baseURL: "http://192.168.24.32:8080",
});

api.interceptors.request.use(
  (config) => {

    const token = localStorage.getItem("accessToken");

    if (config.url === "/api/auth/login") {
      return config;
    }

    if (!token) {
      window.location.href = "/login";
      return Promise.reject(new Error("No token found"));
    }

    if (isTokenExpired(token)) {
      localStorage.removeItem("accessToken");
      window.location.href = "/login";
      return Promise.reject(new Error("Token expired"));
    }

    //config.headers.Authorization = `Bearer ${token}`;
    return config;
  },

  (error) => {
    console.error("Request interceptor error:", error);
    return Promise.reject(error);
  },
);

export const getHeader = () => {

  /**
   * Отримання access token зі сховища браузера.
   */
  const token = localStorage.getItem("accessToken");

  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
};

export async function getAllMedicineLists() {
  try {
    const response = await api.get(`/api/medicinelist`, {
      headers: getHeader(),
    });
    return response.data;
  } catch (error) {

    throw new Error("Error fetching medicine lists");
  }
}

export async function getAllDocumentsByPatientId(patientId) {
  try {
    const response = await api.get(`/api/medicinelist/bypatient/${patientId}`, {
      headers: getHeader(),
    });
    return response.data;
  } catch (error) {
    throw new Error(`Error fetching medicine lists ${error.message}`);
  }
}

export async function getAllAllergiesByPatientId(patientId) {
  try {
    const response = await api.get(
      `/api/medicinelist/allergies/bypatient/${patientId}`,
      {
        headers: getHeader(),
      },
    );
    return response.data;
  } catch (error) {
    throw new Error(`Error fetching allergies ${error.message}`);
  }
}

export async function getMedicineListById(listId) {
  try {
    const response = await api.get(`/api/medicinelist/bylist/${listId}`, {
      headers: getHeader(),
    });
    return response.data;
  } catch (error) {
    throw new Error(`Error fetching medicine list ${error.message}`);
  }
}

export async function deleteDocumentById(documentId) {
  try {
    const response = await api.delete(`/api/medicinelist/${documentId}`, {
      headers: getHeader(),
    });
    return response.data;
  } catch (error) {
    return error.message;
  }
}

export async function addNewMedicineList(medicineList, patient) {

  const payload = {
    medicineList,
    patient: patient,
  };
  
  try {
    const response = await api.post("/api/medicinelist", payload, {
      headers: getHeader(),
    });
    return response;
  } catch (error) {
    throw new Error(`Error creating medicine list ${error.message}`);
  }
}

export async function updateMedicineListById(medicineList, patient) {
  const payload = {
    medicineList,
    patient: patient,

    medicineListPage: window.location.href,
  };

  try {
    const response = await api.put("/api/medicinelist", payload, {
      headers: getHeader(),
    });
    return response;
  } catch (error) {
    throw new Error(`Error updating medicine list ${error.message}`);
  }
}

export async function isDocumentEditing(listId) {
  try {
    const response = await api.get(
      `/api/medicinelist/isDocumentEditing/${listId}`,
      {
        headers: getHeader(),
      },
    );
    return response.data;
  } catch (error) {
    return error;
  }
}

export async function updateMedicineListStatusByListId(id, status) {
  try {
    const response = await api.put(
      `/api/medicinelist/${id}?status=${status}`,
      {},
      {
        headers: getHeader(),
      },
    );
    return response;
  } catch (error) {
    throw new Error(`Error updating medicine list status ${error.message}`);
  }
}

export async function searchPatients(keyword) {
  try {
    const response = await api.get(
      `/api/medicinelist/searchpatients?keyword=${keyword}`,
      {
        headers: getHeader(),
      },
    );
    return response.data;
  } catch (error) {
    throw new 
  }
}

export async function getAllInpatients(order, residence) {
  console.log(order);
  try {
    const response = await api.get(
      `/api/medicinelist/patient/sort?order=${order}&residence=${residence}`,
      {
        headers: getHeader(),
      },
    );
    return response.data;
  } catch (error) {
    throw new Error("Error fetching patients");
  }
}

export async function updateUserById(user) {
  try {
    const response = await api.put(`/api/auth/admin`, user, {
      headers: getHeader(),
    });
    return response;
  } catch (error) {
    throw new Error(`Error updating user${error.message}`);
  }
}

export async function deleteUserById(userId) {
  try {
    const response = await api.delete(`/api/auth/admin/${userId}`, {
      headers: getHeader(),
    });
    return response.data;
  } catch (error) {
    return error.message;
  }
}

export async function getAllUsers() {
  try {
    const response = await api.get("/api/auth/admin", {
      headers: getHeader(),
    });

    return response.data;
  } catch (error) {
    throw new Error(`Error fetching users ${error.message}`);
  }
}

export async function searchMedicine(keyword) {
  const encodedKeyword = encodeURIComponent(keyword);
  try {
    const response = await api.get(
      `/api/medicinelist/searchmedicine?keyword=${encodedKeyword}`,
      {
        headers: getHeader(),
      },
    );
    return response.data;
  } catch (error) {
    throw new Error("Error fetching medicine");
  }
}

export async function getHighRiskMedicineByName(highRiskMedicineName) {
  const encodedHighRiskMedicineName = encodeURIComponent(highRiskMedicineName);
  try {
    const response = await api.get(
      `/api/medicinelist/medicine/getHighRiskMedicineByName?highRiskMedicineName=${encodedHighRiskMedicineName}`,
      {
        headers: getHeader(),
      },
    );
    return response.data;
  } catch (error) {
    throw new Error("Error fetching medicine");
  }
}

export async function getConflictMedicineByName(conflictMedicineName) {
  const encodedConflictMedicineName = encodeURIComponent(conflictMedicineName);
  try {
    const response = await api.get(
      `/api/medicinelist/medicine/getConflictMedicineByName?conflictMedicineName=${encodedConflictMedicineName}`,
      {
        headers: getHeader(),
      },
    );
    return response.data;
  } catch (error) {
    throw new Error("Error fetching medicine");
  }
}

export async function getPatientById(patientId) {
  try {
    const response = await api.get(`/api/medicinelist/patient/${patientId}`, {
      headers: getHeader(),
    });
    return response.data;
  } catch (error) {
    throw new Error(`Error fetching patients ${error.message}`);
  }
}

export async function userLogin(login) {
  try {
    const response = await api.post("/api/auth/login", login, {});
    return response;
  } catch (error) {
    return error.response.data;
  }
}

export async function userRegister(registration) {
  try {
    const response = await api.post("/api/auth/admin/register", registration, {
      headers: getHeader(),
    });

    return response.data;
  } catch (error) {
    throw new Error(`User registration error : ${error.message}`);
  }
}

export async function generateDEDoc(medicineListID, documentDateTime) {
  try {
    const response = await api.get(
      `/api/medicinelist/generatedoc?medicineListID=${medicineListID}&documentDateTime=${encodeURIComponent(
        documentDateTime,
      )}`,
      {
        headers: getHeader(),
      },
    );

    return response.data;
  } catch (error) {
    throw new Error(`Document generating error : ${error.message}`);
  }
}

export async function closeMedicineListByListId(id) {
  try {
    const response = await api.put(
      `/api/medicinelist/closelist/${id}`,
      {},
      {
        headers: getHeader(),
      },
    );

    return response.data;
  } catch (error) {
    throw new Error(`Document generating error : ${error.message}`);
  }
}
