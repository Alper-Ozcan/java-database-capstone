# MySQL Veritabanı Tasarımı
## Table: admins
-	id: INT, Primary Key, Auto Increment
-	username: VARCHAR(50), Not Null, Unique
-	password: VARCHAR(255), Not Null
-	full_name: VARCHAR(100), Not Null
-	email: VARCHAR(100), Unique
-	created_at: DATETIME, Not Null
Constraints
-	PRIMARY KEY (id)
-	UNIQUE (username)
-	UNIQUE (email)
________________________________________
## Table: doctors
-	id: INT, Primary Key, Auto Increment
-	first_name: VARCHAR(50), Not Null
-	last_name: VARCHAR(50), Not Null
-	specialization: VARCHAR(100), Not Null
-	email: VARCHAR(100), Not Null, Unique
-	phone_number: VARCHAR(20), Not Null
-	availability_status: BOOLEAN, Not Null
-	created_at: DATETIME, Not Null
Constraints
-	PRIMARY KEY (id)
-	UNIQUE (email)
________________________________________
## Table: patients
-	id: INT, Primary Key, Auto Increment
-	first_name: VARCHAR(50), Not Null
-	last_name: VARCHAR(50), Not Null
-	email: VARCHAR(100), Not Null, Unique
-	password: VARCHAR(255), Not Null
-	phone_number: VARCHAR(20), Not Null
-	date_of_birth: DATE
-	created_at: DATETIME, Not Null
Constraints
-	PRIMARY KEY (id)
-	UNIQUE (email)
________________________________________
## Table: appointments
-	id: INT, Primary Key, Auto Increment
-	doctor_id: INT, Foreign Key → doctors(id)
-	patient_id: INT, Foreign Key → patients(id)
-	appointment_time: DATETIME, Not Null
-	duration_minutes: INT, Not Null
-	status: INT
Status Values
-	0 = Scheduled
-	1 = Completed
-	2 = Cancelled
Constraints
-	PRIMARY KEY (id)
-	FOREIGN KEY (doctor_id) REFERENCES doctors(id)
-	FOREIGN KEY (patient_id) REFERENCES patients(id)
________________________________________## Table:
## Table: clinic_locations
-	id: INT, Primary Key, Auto Increment
-	clinic_name: VARCHAR(100), Not Null
-	address: VARCHAR(255), Not Null
-	city: VARCHAR(50), Not Null
-	phone_number: VARCHAR(20)
Constraints
-	PRIMARY KEY (id)
________________________________________
## Table: payments
-	id: INT, Primary Key, Auto Increment
-	appointment_id: INT, Foreign Key → appointments(id)
-	amount: DECIMAL(10,2), Not Null
-	payment_date: DATETIME, Not Null
-	payment_status: VARCHAR(20), Not Null
Constraints
-	PRIMARY KEY (id)
-	FOREIGN KEY (appointment_id) REFERENCES appointments(id)
________________________________________
Validation Considerations
NOT NULL Fields
The following fields should not be empty:
-	username
-	password
-	first_name
-	last_name
-	email
-	specialization
-	appointment_time
-	amount
UNIQUE Fields
The following fields should be unique:
-	admins.username
-	admins.email
-	doctors.email
-	patients.email
AUTO_INCREMENT Fields
The primary key id column in all tables should use AUTO_INCREMENT.
Application-Level Validation
The following validations should be implemented in the application layer:
-	Email format validation
-	Phone number format validation
-	Password strength validation
-	Appointment date and time validation
-	Prevention of overlapping appointments
________________________________________

# MongoDB Koleksiyon Tasarımı
Collection: prescriptions
{
  "_id": "ObjectId('64abc123456')",
  "appointmentId": 51,
  "patientId": 12,
  "doctorId": 5,
  "diagnosis": "Upper Respiratory Infection",
  "medications": [
    {
      "name": "Paracetamol",
      "dosage": "500mg",
      "frequency": "Every 6 hours",
      "duration": "5 days"
    }
  ],
  "doctorNotes": "Drink plenty of fluids and rest.",
  "metadata": {
    "createdAt": "2025-06-15T10:30:00Z",
    "updatedAt": "2025-06-15T10:30:00Z",
    "status": "ACTIVE"
  }
}
## Collection: Prescriptions
-	_id: ObjectId, Primary Identifier
-	appointmentId: Integer, Related Appointment ID
-	patientId: Integer, Related Patient ID
-	doctorId: Integer, Related Doctor ID
-	diagnosis: String, Patient diagnosis information
-	medications: Array of Medication objects
-	doctorNotes: String, Additional notes from the doctor
-	metadata: Embedded document containing audit information
Embedded Document: Medication
{
  "name": "Paracetamol",
  "dosage": "500mg",
  "frequency": "Every 6 hours",
  "duration": "5 days"
}
Embedded Document: Metadata
{
  "createdAt": "2025-06-15T10:30:00Z",
  "updatedAt": "2025-06-15T10:30:00Z",
  "status": "ACTIVE"
}
