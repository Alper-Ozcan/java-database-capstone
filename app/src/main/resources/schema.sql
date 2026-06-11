/* SQL script to create stored procedures */

DROP PROCEDURE IF EXISTS GetDailyAppointmentReportByDoctor$$
DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByMonth$$
DROP PROCEDURE IF EXISTS GetDoctorWithMostPatientsByYear$$
DROP PROCEDURE IF EXISTS sp_daily_appointment_report$$
DROP PROCEDURE IF EXISTS sp_patient_daily_report$$
DROP PROCEDURE IF EXISTS sp_dashboard_daily_summary$$
DROP PROCEDURE IF EXISTS sp_top_doctor_by_patient_count$$
DROP PROCEDURE IF EXISTS sp_top5_doctors_by_patient_count$$
DROP PROCEDURE IF EXISTS sp_best_doctor_monthly_report$$


CREATE PROCEDURE GetDailyAppointmentReportByDoctor(
    IN report_date DATE
)
BEGIN
    SELECT 
        d.name AS doctor_name,
        a.appointment_time,
        a.status,
        p.name AS patient_name,
        p.phone AS patient_phone
    FROM 
        appointment a
    JOIN 
        doctor d ON a.doctor_id = d.id
    JOIN 
        patient p ON a.patient_id = p.id
    WHERE 
        DATE(a.appointment_time) = report_date
    ORDER BY 
        d.name, a.appointment_time;
END$$

CREATE PROCEDURE GetDoctorWithMostPatientsByMonth(
    IN input_month INT, 
    IN input_year INT
)
BEGIN
    SELECT
        doctor_id, 
        COUNT(patient_id) AS patients_seen
    FROM
        appointment
    WHERE
        MONTH(appointment_time) = input_month 
        AND YEAR(appointment_time) = input_year
    GROUP BY
        doctor_id
    ORDER BY
        patients_seen DESC
    LIMIT 1;
END$$

CREATE PROCEDURE GetDoctorWithMostPatientsByYear(
    IN input_year INT
)
BEGIN
    SELECT
        doctor_id, 
        COUNT(patient_id) AS patients_seen
    FROM
        appointment
    WHERE
        YEAR(appointment_time) = input_year
    GROUP BY
        doctor_id
    ORDER BY
        patients_seen DESC
    LIMIT 1;
END$$

CREATE PROCEDURE sp_daily_appointment_report(
    IN report_date DATE
)
BEGIN

    SELECT
        d.id AS doctor_id,
        d.name AS doctor_name,

        COUNT(a.id) AS total_appointments,

        SUM(
            CASE
                WHEN a.status = 1 THEN 1
                ELSE 0
            END
        ) AS completed_appointments,

        SUM(
            CASE
                WHEN a.status = 0 THEN 1
                ELSE 0
            END
        ) AS scheduled_appointments

    FROM doctor d

    LEFT JOIN appointment a
        ON d.id = a.doctor_id
       AND DATE(a.appointment_time) = report_date

    GROUP BY
        d.id,
        d.name

    ORDER BY
        total_appointments DESC;

END$$


CREATE PROCEDURE sp_patient_daily_report(
    IN report_date DATE
)
BEGIN

    SELECT
        a.id,
        p.name AS patient_name,
        d.name AS doctor_name,
        a.appointment_time,

        CASE
            WHEN a.status = 0 THEN 'Scheduled'
            WHEN a.status = 1 THEN 'Completed'
            ELSE 'Unknown'
        END AS appointment_status

    FROM appointment a
    JOIN patient p
        ON p.id = a.patient_id
    JOIN doctor d
        ON d.id = a.doctor_id

    WHERE DATE(a.appointment_time) = report_date

    ORDER BY a.appointment_time;

END$$

CREATE PROCEDURE sp_dashboard_daily_summary(
    IN report_date DATE
)
BEGIN

    SELECT

        COUNT(*) AS total_appointments,

        SUM(
            CASE
                WHEN status = 1 THEN 1
                ELSE 0
            END
        ) AS completed,

        SUM(
            CASE
                WHEN status = 0 THEN 1
                ELSE 0
            END
        ) AS scheduled,

        COUNT(DISTINCT doctor_id) AS active_doctors,

        COUNT(DISTINCT patient_id) AS active_patients

    FROM appointment

    WHERE DATE(appointment_time) = report_date;

END$$


CREATE PROCEDURE sp_top_doctor_by_patient_count(
    IN p_month INT,
    IN p_year INT
)
BEGIN

    SELECT
        d.id AS doctor_id,
        d.name AS doctor_name,
        d.specialty,

        COUNT(DISTINCT a.patient_id) AS unique_patient_count,

        COUNT(a.id) AS total_appointments

    FROM doctor d
    JOIN appointment a
        ON d.id = a.doctor_id

    WHERE MONTH(a.appointment_time) = p_month
      AND YEAR(a.appointment_time) = p_year

    GROUP BY
        d.id,
        d.name,
        d.specialty

    ORDER BY unique_patient_count DESC

    LIMIT 1;

END$$

CREATE PROCEDURE sp_top5_doctors_by_patient_count(
    IN p_month INT,
    IN p_year INT
)
BEGIN

    SELECT
        d.id,
        d.name,
        d.specialty,
        COUNT(DISTINCT a.patient_id) AS unique_patient_count

    FROM doctor d
    JOIN appointment a
        ON d.id = a.doctor_id

    WHERE MONTH(a.appointment_time) = p_month
      AND YEAR(a.appointment_time) = p_year

    GROUP BY
        d.id,
        d.name,
        d.specialty

    ORDER BY unique_patient_count DESC

    LIMIT 5;

END$$

CREATE PROCEDURE sp_best_doctor_monthly_report(
    IN p_month INT,
    IN p_year INT
)
BEGIN

    SELECT
        d.id,
        d.name,
        d.specialty,

        COUNT(DISTINCT a.patient_id) AS unique_patients,

        COUNT(a.id) AS total_appointments,

        SUM(
            CASE
                WHEN a.status = 1 THEN 1
                ELSE 0
            END
        ) AS completed_appointments

    FROM doctor d
    JOIN appointment a
        ON d.id = a.doctor_id

    WHERE MONTH(a.appointment_time) = p_month
      AND YEAR(a.appointment_time) = p_year

    GROUP BY
        d.id,
        d.name,
        d.specialty

    ORDER BY unique_patients DESC

    LIMIT 1;

END$$