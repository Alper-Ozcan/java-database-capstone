// patientRecordServices.js
import { getPatientAppointments } from "./services/patientServices.js";
import { createPatientRecordRow } from './components/patientRecordRow.js';

const tableBody = document.getElementById("patientTableBody");
const token = localStorage.getItem("token");

// URL parametrelerinden hasta ve doktor ID'lerini güvenle çekiyoruz
const urlParams = new URLSearchParams(window.location.search);
const patientId = urlParams.get("id");
const doctorId = urlParams.get("doctorId");

document.addEventListener("DOMContentLoaded", initializePage);

/*
 * Sayfa İlk Yüklenme Mantığı
 */
// patientRecordServices.js - Satır 31 Civarı

async function initializePage() {
  try {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("userRole") || "patient";
    const patientId = localStorage.getItem("patientId") || 2;

    if (!token) {
        window.location.href = "/";
        return;
    }

    // 🔥 KESİN ÇÖZÜM: Değişkenlerin sırasını tam olarak backend'in beklediği hizaya getiriyoruz!
    // Sıralama kuralı: 1. Kimlik (patientId), 2. Şifreli Anahtar (token), 3. Rol Kelimesi (role)
    const appointmentData = await getPatientAppointments(patientId, token, role);
    
    console.log("Sunucudan pürüzsüzce dönen randevu geçmişi:", appointmentData);

    if (typeof renderAppointments === "function") {
        renderAppointments(appointmentData);
    }

  } catch (error) {
    console.error("Error loading appointments inside patientRecordServices:", error);
    alert("❌ Failed to load appointments.");
  }
}


/*
 * Randevuları HTML Tablosuna Basma Mantığı
 */
function renderAppointments(appointments) {
  if (!tableBody) return;
  
  tableBody.innerHTML = "";

  // "Actions/İşlemler" tablosunun başlığını her zaman görünür kılıyoruz
  const actionTh = document.querySelector("#patientTable thead tr th:last-child");
  if (actionTh) {
    actionTh.style.display = "table-cell"; 
  }

  // Eğer filtreleme sonucunda hastaya ait hiçbir randevu kalmadıysa temiz mesaj basıyoruz
  if (!appointments || appointments.length === 0) {
    tableBody.innerHTML = `
        <tr>
            <td colspan="5" style="text-align:center; font-style: italic; color: #666; padding: 20px;">
                No Appointments Found for this doctor criteria.
            </td>
        </tr>
    `;
    return;
  }

  // Her bir randevu nesnesi için 'patientRecordRow.js' bileşeni çağrılıp tabloya ekleniyor
  appointments.forEach(appointment => {
    // Burada kart satır bileşenine normalize edilmiş randevunun kendisi enjekte ediliyor
    const row = createPatientRecordRow(appointment);
    tableBody.appendChild(row);
  });
}