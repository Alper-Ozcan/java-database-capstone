// patientServices
import { API_BASE_URL } from "../config/config.js";

const PATIENT_API = API_BASE_URL + '/patient/login'


//For creating a patient in db
export async function patientSignup(data) {
  try {
    const response = await fetch(`${PATIENT_API}`,
      {
        method: "POST",
        headers: {
          "Content-type": "application/json"
        },
        body: JSON.stringify(data)
      }
    );
    const result = await response.json();
    if (!response.ok) {
      throw new Error(result.message);
    }
    return { success: response.ok, message: result.message }
  }
  catch (error) {
    console.error("Error :: patientSignup :: ", error)
    return { success: false, message: error.message }
  }
}

//For logging in patient
export async function patientLogin(data) {
  console.log("patientLogin :: ", data)
  return await fetch(`${PATIENT_API}/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(data)
  });
}



// For getting patient data (name ,id , etc ). Used in booking appointments
// patientServices.js

export async function getPatientData(token) {
    try {
        // 🔥 KESİN ÇÖZÜM: Backend Controller 3. metoda (@GetMapping("/{token}")) tam uyum sağlandı.
        // Araya 'login' veya 'profile' koymadan doğrudan token'ı URL'e ekliyoruz.
        const url = `${API_BASE_URL}/patient/${token}`; 

        console.log("Profil verisi çekiliyor. Adres:", url);

        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Content-Type": "application/json"
            }
        });

        if (response.ok) {
            const data = await response.json();
            console.log("Backend'den dönen hasta veri paketi:", data);
            return data;
        } else {
            console.error("Hasta profili sunucudan çekilemedi. Durum Kodu:", response.status);
            return null;
        }
    } catch (error) {
        console.error("getPatientData servis hatası:", error);
        return null;
    }
}


// the Backend API for fetching the patient record(visible in Doctor Dashboard) and Appointments (visible in Patient Dashboard) are same based on user(patient/doctor).
// patientServices.js - getPatientAppointments

export async function getPatientAppointments(patientId, token, userRole) {
    try {
        // 🔥 KESİN KORUMA 1: Eğer rol boş geldiyse veya yanlışlıkla token ile yer değiştirdiyse düzeltiyoruz
        const finalRole = userRole || localStorage.getItem("userRole") || "patient";
        const finalToken = token || localStorage.getItem("token");

        if (!finalToken) {
            console.error("Token bulunamadı, istek engellendi.");
            return [];
        }

        // Backend Controller 6. maddedeki dizilim: /appointments/{patientId}/{user}/{token}
        const url = `${API_BASE_URL}/patient/appointments/${patientId}/${finalRole.toLowerCase()}/${finalToken}`;
        
        console.log("Randevu geçmişi detayları için istek atılan tam URL:", url);

        const response = await fetch(url, {
            method: "GET",
            headers: { "Content-Type": "application/json" }
        });

        if (response.ok) {
            return await response.json();
        } else {
            throw new Error(`Failed to fetch patient appointments. Status: ${response.status}`);
        }
    } catch (error) {
        console.error("Error in getPatientAppointments service:", error);
        throw error;
    }
}


export async function filterAppointments(condition, doctorName, token) {
    try {
        // Gelen null veya boş parametreleri backend'in esnek araması için "all" dizesine çeviriyoruz
        const finalCondition = (!condition || condition === null || condition === "null" || condition.trim() === "") ? "all" : condition.trim();
        const finalName = (!doctorName || doctorName === null || doctorName === "null" || doctorName.trim() === "") ? "all" : doctorName.trim();

        // 🔥 KESİN ÇÖZÜM: Hatalı '/login/' ekini silip backend Controller 7. maddedeki rota ile harfi harfine eşliyoruz
        // Rota: /patient/appointments/filter/ + condition + / + name + / + token
        const url = `${API_BASE_URL}/patient/appointments/filter/${encodeURIComponent(finalCondition)}/${encodeURIComponent(finalName)}/${token}`;

        console.log("Randevu filtreleme için istek atılan tam URL:", url);

        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Content-Type": "application/json"
            }
        });

        if (response.ok) {
            const data = await response.json();
            console.log("Filtrelenmiş randevu listesi (JSON):", data);
            return data;
        } else {
            console.error("Filtreleme başarısız oldu. Durum:", response.status);
            return [];
        }
    } catch (error) {
        console.error("Error in filterAppointments service:", error);
        return [];
    }
}