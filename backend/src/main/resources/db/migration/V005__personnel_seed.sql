INSERT INTO locations (location_id, city, address) VALUES
(1, 'Novosibirsk', 'District Command Center'),
(2, 'Omsk', 'Forward Garrison')
ON CONFLICT (location_id) DO NOTHING;

INSERT INTO military_formations (formation_id, name, formation_type, parent_id, formation_date, status) VALUES
(1, 'Siberian Tactical District', 'Округ', NULL, DATE '2020-01-10', 'Активна'),
(2, '2nd Combined Army', 'Армия', 1, DATE '2020-02-01', 'Активна'),
(5, '5th Guards Brigade', 'Бригада', 2, DATE '2020-03-15', 'Активна')
ON CONFLICT (formation_id) DO NOTHING;

INSERT INTO military_units (unit_id, name, formation_id, location_id) VALUES
(1, 'Unit 417 Signal Battalion', 5, 1),
(2, 'Unit 902 Logistics Detachment', 5, 2)
ON CONFLICT (unit_id) DO NOTHING;

INSERT INTO subdivisions (subdivision_id, name, type, unit_id, parent_id) VALUES
(10, 'Alpha Company', 'Рота', 1, NULL),
(11, 'Alpha-1 Platoon', 'Взвод', 1, 10),
(12, 'Alpha-1-1 Squad', 'Отделение', 1, 11),
(20, 'Bravo Company', 'Рота', 2, NULL)
ON CONFLICT (subdivision_id) DO NOTHING;

INSERT INTO personnel (
    personnel_id, last_name, first_name, middle_name, personal_number,
    birth_date, service_start, subdivision_id
) VALUES
(1, 'District', 'Admin', NULL, 'TD-0001', DATE '1982-01-20', DATE '2004-06-01', 10),
(2, 'Staff', 'Analyst', NULL, 'TD-0002', DATE '1987-05-14', DATE '2009-09-01', 10),
(3, 'Army', 'Commander', NULL, 'TD-0003', DATE '1978-03-11', DATE '2000-08-15', 10),
(4, 'Brigade', 'Commander', NULL, 'TD-0004', DATE '1980-07-21', DATE '2002-08-15', 10),
(5, 'Unit', 'Commander', NULL, 'TD-0005', DATE '1984-11-02', DATE '2006-09-01', 10),
(6, 'Company', 'Commander', NULL, 'TD-0006', DATE '1989-12-19', DATE '2011-10-01', 10),
(7, 'Platoon', 'Commander', NULL, 'TD-0007', DATE '1992-04-08', DATE '2014-07-01', 11),
(8, 'Squad', 'Commander', NULL, 'TD-0008', DATE '1994-08-25', DATE '2016-09-01', 12),
(9, 'Demo', 'Soldier', NULL, 'TD-0009', DATE '1999-02-17', DATE '2020-11-01', 12),
(10, 'Petrov', 'Ivan', 'Sergeevich', 'TD-0010', DATE '1998-06-12', DATE '2019-09-01', 12),
(11, 'Sidorov', 'Pavel', 'Andreevich', 'TD-0011', DATE '1997-10-02', DATE '2018-05-15', 20)
ON CONFLICT (personnel_id) DO NOTHING;

INSERT INTO military_ranks (rank_id, name, category) VALUES
(1, 'Полковник', 'Офицерский'),
(2, 'Майор', 'Офицерский'),
(3, 'Капитан', 'Офицерский'),
(4, 'Лейтенант', 'Офицерский'),
(5, 'Сержант', 'Сержантский и Рядовой'),
(6, 'Рядовой', 'Сержантский и Рядовой')
ON CONFLICT (rank_id) DO NOTHING;

INSERT INTO personnel_ranks (personnel_id, rank_id, assignment_date) VALUES
(1, 1, DATE '2018-01-01'),
(2, 2, DATE '2019-01-01'),
(3, 1, DATE '2017-01-01'),
(4, 1, DATE '2018-06-01'),
(5, 2, DATE '2020-01-01'),
(6, 3, DATE '2021-01-01'),
(7, 4, DATE '2022-01-01'),
(8, 5, DATE '2022-06-01'),
(9, 6, DATE '2023-01-01'),
(10, 6, DATE '2022-05-01'),
(11, 5, DATE '2021-05-01')
ON CONFLICT (personnel_id) DO NOTHING;

INSERT INTO specialties (specialty_id, name) VALUES
(1, 'Signals Operator'),
(2, 'Mechanic'),
(3, 'Medic'),
(4, 'Logistics Specialist')
ON CONFLICT (specialty_id) DO NOTHING;

INSERT INTO personnel_specialties (personnel_id, specialty_id) VALUES
(1, 1), (2, 4), (3, 4), (4, 1), (5, 1), (6, 1),
(7, 1), (8, 1), (9, 1), (10, 3), (11, 4)
ON CONFLICT DO NOTHING;

UPDATE military_formations SET commander_id = 1 WHERE formation_id = 1;
UPDATE military_formations SET commander_id = 3 WHERE formation_id = 2;
UPDATE military_formations SET commander_id = 4 WHERE formation_id = 5;
UPDATE military_units SET commander_id = 5 WHERE unit_id = 1;
UPDATE subdivisions SET commander_id = 6 WHERE subdivision_id = 10;
UPDATE subdivisions SET commander_id = 7 WHERE subdivision_id = 11;
UPDATE subdivisions SET commander_id = 8 WHERE subdivision_id = 12;

SELECT setval('locations_location_id_seq', (SELECT MAX(location_id) FROM locations));
SELECT setval('military_formations_formation_id_seq', (SELECT MAX(formation_id) FROM military_formations));
SELECT setval('military_units_unit_id_seq', (SELECT MAX(unit_id) FROM military_units));
SELECT setval('subdivisions_subdivision_id_seq', (SELECT MAX(subdivision_id) FROM subdivisions));
SELECT setval('personnel_personnel_id_seq', (SELECT MAX(personnel_id) FROM personnel));
SELECT setval('military_ranks_rank_id_seq', (SELECT MAX(rank_id) FROM military_ranks));
SELECT setval('specialties_specialty_id_seq', (SELECT MAX(specialty_id) FROM specialties));
