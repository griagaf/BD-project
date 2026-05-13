CREATE OR REPLACE VIEW v_personnel_specialties AS
SELECT
    vpf.personnel_id,
    vpf.last_name,
    vpf.first_name,
    vpf.middle_name,
    vpf.personal_number,
    vpf.rank_name,
    vpf.rank_category,
    vpf.subdivision_id,
    vpf.subdivision_name,
    vpf.subdivision_type,
    vpf.unit_id,
    vpf.unit_name,
    vpf.formation_id,
    vpf.formation_name,
    vpf.formation_type,
    sp.specialty_id,
    sp.name AS specialty_name
FROM v_personnel_full vpf
JOIN personnel_specialties ps ON ps.personnel_id = vpf.personnel_id
JOIN specialties sp ON sp.specialty_id = ps.specialty_id;
