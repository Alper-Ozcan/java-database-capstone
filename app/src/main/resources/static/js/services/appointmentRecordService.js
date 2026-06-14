// appointmentRecordService.js
import { API_BASE_URL } from "../config/config.js";
const APPOINTMENT_API = `${API_BASE_URL}/appointments`;

import { getPatientAppointments } from "/js/services/patientServices.js";

async function initializePage() {
  try {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("userRole") || "patient";

    // Hafızadan o anki hastanın ID bilgisini güvenle alıyoruz
    const patientId = localStorage.getItem("patientId") || 2;

    if (!token) {
      console.error("Token bulunamadı, giriş sayfasına yönlendiriliyor.");
      window.location.href = "/";
      return;
    }

    console.log("Servis çağrısı öncesi pürüzsüz parametre kontrolü:", { patientId, token, role });

    // 🔥 KESİN ÇÖZÜM: Parametre dizilimini harfi harfine sıraya koyuyoruz!
    // Sıralama tam olarak: patientId, sonra şifreli token, en son ise rol dizesi (doctor/patient) olmalıdır.
    const appointmentData = await getPatientAppointments(patientId, token, role);

    console.log("Backend katmanından pürüzsüzce dönen randevu arşivi:", appointmentData);

    // Dönen verileri arayüzdeki tablonuza basan fonksiyonunuzu tetikleyin:
    if (typeof renderAppointments === "function") {
      renderAppointments(appointmentData);
    }

  } catch (error) {
    console.error("Error loading appointments inside patientRecordServices:", error);
    alert("❌ Failed to load appointments.");
  }
}

//This is for the doctor to get all the patient Appointments
export async function getAllAppointments(date, patientName, token) {
  try {
    // Eğer date parametresi boş geldiyse URL kırılmasın diye bugünün tarihini basıyoruz
    const finalDate = !date || date === null || date.trim() === ""
      ? new Date().toISOString().split("T")[0]
      : date.trim();

    const finalName = !patientName || patientName === null || patientName.trim() === "" ? "all" : patientName.trim();

    // 🔥 KESİN ÇÖZÜM: doctorId değişkenini ve URL sonundaki '?doctorId=' ekini tamamen sildik!
    // URL artık tertemiz ve kurumsal standartta sadece korumalı rotayı barındırıyor:
    const url = `${APPOINTMENT_API}/${finalDate}/${encodeURIComponent(finalName)}/${token}`;

    console.log("Güvenli mimariyle istek atılan tam URL:", url);

    const response = await fetch(url);
    if (!response.ok) {
      throw new Error("Failed to fetch appointments");
    }

    return await response.json();
  } catch (error) {
    console.error("Error in getAllAppointments service:", error);
    return [];
  }
}


export async function bookAppointment(appointment, token) {
  try {
    const response = await fetch(`${APPOINTMENT_API}/${token}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(appointment)
    });

    const data = await response.json();
    return {
      success: response.ok,
      message: data.message || "Something went wrong"
    };
  } catch (error) {
    console.error("Error while booking appointment:", error);
    return {
      success: false,
      message: "Network error. Please try again later."
    };
  }
}

export async function updateAppointment(appointment, token) {
  try {
    const response = await fetch(`${APPOINTMENT_API}/${token}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify(appointment)
    });

    const data = await response.json();
    return {
      success: response.ok,
      message: data.message || "Something went wrong"
    };
  } catch (error) {
    console.error("Error while booking appointment:", error);
    return {
      success: false,
      message: "Network error. Please try again later."
    };
  }
}
