// addPrescription.js - Güncellenmiş Kararlı Sürüm

import { savePrescription, getPrescription } from "./services/prescriptionServices.js";

document.addEventListener('DOMContentLoaded', async () => {
  const savePrescriptionBtn = document.getElementById("savePrescription");
  const patientNameInput = document.getElementById("patientName");
  const medicinesInput = document.getElementById("medicines");
  const dosageInput = document.getElementById("dosage");
  const notesInput = document.getElementById("notes");
  const heading = document.getElementById("heading");

  const urlParams = new URLSearchParams(window.location.search);
  const appointmentId = urlParams.get("appointmentId");
  let mode = urlParams.get("mode"); // const yerine let yaptık, dinamik değişebilsin
  const token = localStorage.getItem("token");
  const patientName = urlParams.get("patientName");

  // Pre-fill patient name (Her durumda hastanın adı formda yazmalı)
  if (patientNameInput && patientName) {
    patientNameInput.value = patientName;
  }

  // Fetch and pre-fill existing prescription if it exists
  if (appointmentId && token) {
    try {
      const response = await getPrescription(appointmentId, token);
      const prescriptionData = response.prescription || response;

      console.log("response data:", response);

      // Reçete verisi MongoDB'de bulunduysa alanları doldur
      if (prescriptionData && prescriptionData.medication) {
        console.log("Prescription found! Filling data...");
        patientNameInput.value = prescriptionData.patientName || "";
        medicinesInput.value = prescriptionData.medication || "";
        dosageInput.value = prescriptionData.dosage || "";
        notesInput.value = prescriptionData.doctorNotes || "";

        // Veri başarıyla doldurulduğu için modu zorunlu olarak "view" yapıyoruz
        mode = "view";
      }

    } catch (error) {
      // 🔥 MongoDB'de reçete bulunamadı (404 düştü)
      console.log("Bu randevuya ait eski bir reçete yok. Yeni reçete yazma modu aktif ediliyor.");

      // Reçete bulunamadığı için modu zorunlu olarak "add" (yazma) moduna çekiyoruz!
      mode = "add";
    }
  }

  // 🌟 MOD KONTROLÜ (Veri çekme işleminden sonra çalışır)
  if (mode === 'view') {
    if (heading) heading.innerHTML = `View <span>Prescription</span>`;

    // Alanları kilitle (Read-only)
    if (patientNameInput) patientNameInput.disabled = true;
    if (medicinesInput) medicinesInput.disabled = true;
    if (dosageInput) dosageInput.disabled = true;
    if (notesInput) notesInput.disabled = true;

    // Kaydet butonunu gizle
    if (savePrescriptionBtn) savePrescriptionBtn.style.display = "none";
  } else {
    // Mode 'add' ise veya 404'e düştüyse burası çalışır
    if (heading) heading.innerHTML = `Add <span>Prescription</span>`;

    // Alanların kilidini aç (Doktor yazabilsin)
    if (patientNameInput) patientNameInput.disabled = false;
    if (medicinesInput) medicinesInput.disabled = false;
    if (dosageInput) dosageInput.disabled = false;
    if (notesInput) notesInput.disabled = false;

    // Kaydet butonunu MUTLAKA göster
    if (savePrescriptionBtn) savePrescriptionBtn.style.display = "block";
  }

  // Save prescription on button click
  if (savePrescriptionBtn) {
    savePrescriptionBtn.addEventListener('click', async (e) => {
      e.preventDefault();

      // 🔥 ÇİFT TIKLAMA KORUMASI: Sunucuya mikro saniyeler içinde mükerrer istek gitmesini engeller
      if (savePrescriptionBtn.disabled) return;
      savePrescriptionBtn.disabled = true;
      savePrescriptionBtn.innerText = "Saving...";

      const prescription = {
        patientName: patientNameInput.value,
        medication: medicinesInput.value,
        dosage: dosageInput.value,
        doctorNotes: notesInput.value,
        appointmentId: parseInt(appointmentId)
      };

      try {
        const { success, message } = await savePrescription(prescription, token);

        if (success) {
          alert("✅ Prescription saved successfully.");
          
          // 🔥 DÜZELTME 1: selectRole fonksiyonunun varlığı kontrol edilerek çökme önlendi
          if (typeof selectRole === "function") {
              selectRole('doctor');
          }
          
          // 🔥 DÜZELTME 2: Tarayıcıyı '/' ile kök dizinden başlatarak Thymeleaf 
          // doktor paneli REST rotasına hatasız uçuruyoruz.
          window.location.href = `/doctorDashboard/${token}`;
          
        } else {
          alert("❌ Failed to save prescription. " + message);
          // Hata durumunda butonu tekrar aktif ediyoruz
          savePrescriptionBtn.disabled = false;
          savePrescriptionBtn.innerText = "Save";
        }
      } catch (err) {
        console.error("Network error during prescription save:", err);
        savePrescriptionBtn.disabled = false;
        savePrescriptionBtn.innerText = "Save";
      }
    });
  }
});




// addPrescription.js - Sayfa İlk Açıldığında Eski Veriyi Doldurma Katmanı
/*
import { getPrescription } from "./services/prescriptionServices.js";

document.addEventListener("DOMContentLoaded", async () => {
  const urlParams = new URLSearchParams(window.location.search);
  const appointmentId = urlParams.get("appointmentId");
  const token = localStorage.getItem("token");

  if (!appointmentId || !token) return;

  try {
    // MongoDB'den bu randevuya ait reçete bilgisini çekiyoruz
    const result = await getPrescription(appointmentId, token);

    // Backend veriyi map içinde "prescription" anahtarıyla döndüğü için çözümlüyoruz
    const prescription = result.prescription || result;

    // 🔥 KESİN ÇÖZÜM NOKTASI: 
    // Mükerrer bloklar temizlendi. Reçete verisi varsa doldurma ve kilitleme mekanizması başlar.
    if (prescription && prescription.medication) {
      console.log("Eski reçete başarıyla yüklendi:", prescription);

      // 1. Sayfa Başlığını Güncelleme (Görüntüleme Modu)
      const pageTitle = document.getElementById("pageTitle") || document.querySelector("h2");
      if (pageTitle) pageTitle.textContent = "View Prescription";

      // 2. Sayfadaki tüm girdileri (input ve textarea) sırayla hafızaya alıyoruz (Garantili Yöntem)
      const allInputs = document.querySelectorAll("input:not([type='button']):not([type='submit'])");
      const allTextAreas = document.querySelectorAll("textarea");

      // --- İlaç Adı Alanı ---
      const medicationInput = document.getElementById("medicationInput") ||
                              document.getElementById("medication") ||
                              document.getElementById("medName") ||
                              allInputs[0]; // ID bulunamazsa formdaki 1. sıradaki input kutusu
      if (medicationInput) {
        medicationInput.value = prescription.medication;
        medicationInput.disabled = true; // Düzenlemeyi kapatır
      }

      // --- Dozaj Alanı ---
      const dosageInput = document.getElementById("dosageInput") ||
                          document.getElementById("dosage") ||
                          document.getElementById("dose") ||
                          allInputs[1]; // ID bulunamazsa formdaki 2. sıradaki input kutusu
      if (dosageInput) {
        dosageInput.value = prescription.dosage;
        dosageInput.disabled = true;
      }

      // --- Doktor Notu Alanı ---
      const notesInput = document.getElementById("notesInput") ||
                         document.getElementById("doctorNotes") ||
                         document.getElementById("notes") ||
                         document.getElementById("description") ||
                         allTextAreas[0] || // ID bulunamazsa formdaki 1. büyük metin alanı
                         allInputs[2]; // O da yoksa formdaki 3. input kutusu
      if (notesInput) {
        notesInput.value = prescription.doctorNotes || "";
        notesInput.disabled = true;
      }

      // 3. Eski Reçete Değiştirilemeyeceği İçin Kaydet Butonunu Gizleme
      const saveBtn = document.getElementById("savePrescriptionBtn") ||
                      document.getElementById("saveBtn") ||
                      document.querySelector("button[type='submit']") ||
                      document.querySelector("button");
      if (saveBtn) {
        saveBtn.style.display = "none";
      }
    }
  } catch (error) {
    // Eğer 404 (Not Found) döndüyse veya hata alındıysa buraya düşer; form boş kalır.
    console.log("Bu randevuya ait eski bir reçete yok veya yüklenemedi. Yeni reçete yazma modu aktif.", error);
  }
});

*/

