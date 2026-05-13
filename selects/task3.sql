-- 3.1
SELECT
    vpf.personnel_id,
    vpf.last_name,
    vpf.first_name,
    vpf.middle_name,
    vpf.personal_number,
    vpf.rank_name,
    vpf.unit_name,
    vpf.subdivision_name
FROM v_personnel_full vpf
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vpf.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
  AND vpf.rank_category = 'Сержантский и Рядовой'
ORDER BY vpf.rank_name, vpf.last_name, vpf.first_name, vpf.middle_name;

-- 3.2
SELECT
    vpf.personnel_id,
    vpf.last_name,
    vpf.first_name,
    vpf.middle_name,
    vpf.personal_number,
    vpf.rank_name,
    vpf.unit_name,
    vpf.subdivision_name
FROM v_personnel_full vpf
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = vpf.formation_id
WHERE fc.root_formation_name = 'Западный военный округ'
  AND vpf.rank_category = 'Сержантский и Рядовой'
  AND vpf.rank_name = 'Сержант'
ORDER BY vpf.last_name, vpf.first_name, vpf.middle_name;

-- 3.3
SELECT
    vpf.personnel_id,
    vpf.last_name,
    vpf.first_name,
    vpf.middle_name,
    vpf.personal_number,
    vpf.rank_name,
    vpf.unit_name,
    vpf.subdivision_name
FROM v_personnel_full vpf
WHERE vpf.unit_name = '1-й танковый полк'
  AND vpf.rank_category = 'Сержантский и Рядовой'
ORDER BY vpf.rank_name, vpf.last_name, vpf.first_name, vpf.middle_name;
