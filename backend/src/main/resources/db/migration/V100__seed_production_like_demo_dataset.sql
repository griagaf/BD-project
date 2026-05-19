DELETE FROM refresh_tokens;
DELETE FROM user_roles;
DELETE FROM role_permissions;
DELETE FROM command_assignments;
DELETE FROM users;
DELETE FROM permissions;
DELETE FROM roles;
DELETE FROM audit_events;
UPDATE military_formations SET commander_id = NULL;
UPDATE military_units SET commander_id = NULL;
UPDATE subdivisions SET commander_id = NULL;
DELETE FROM personnel_specialties;
DELETE FROM personnel_ranks;
DELETE FROM subdivision_buildings;
DELETE FROM equipment_in_units;
DELETE FROM weapon_in_units;
DELETE FROM buildings;
DELETE FROM equipment_types;
DELETE FROM equipment_categories;
DELETE FROM weapon_types;
DELETE FROM weapon_categories;
DELETE FROM personnel;
DELETE FROM specialties;
DELETE FROM military_ranks;
DELETE FROM subdivisions;
DELETE FROM military_units;
DELETE FROM military_formations;
DELETE FROM locations;

INSERT INTO roles (code, name, description) VALUES
('ADMIN_DISTRICT', 'Администратор округа', 'Полное администрирование округа и режим имитации доступа'),
('STAFF_ANALYST', 'Аналитик штаба', 'Аналитический доступ на чтение по всему округу'),
('ARMY_COMMANDER', 'Командующий армией', 'Командный доступ в пределах назначенной армии'),
('FORMATION_COMMANDER', 'Командир соединения', 'Командный доступ в пределах назначенного соединения'),
('UNIT_COMMANDER', 'Командир военной части', 'Командный доступ в пределах назначенной военной части'),
('COMPANY_COMMANDER', 'Командир роты', 'Командный доступ в пределах назначенной роты'),
('PLATOON_COMMANDER', 'Командир взвода', 'Командный доступ в пределах назначенного взвода'),
('SQUAD_COMMANDER', 'Командир отделения', 'Командный доступ в пределах назначенного отделения'),
('SOLDIER', 'Военнослужащий', 'Доступ к личной карточке военнослужащего');

INSERT INTO permissions (code, name, description) VALUES
('dashboard:read', 'Просмотр панели управления', 'Доступ к тактической панели управления'),
('structure:read', 'Просмотр структуры', 'Просмотр иерархии округа и подчинённости'),
('personnel:read', 'Просмотр личного состава', 'Просмотр карточек военнослужащих'),
('personnel:create', 'Создание военнослужащих', 'Создание карточек военнослужащих'),
('personnel:update', 'Изменение военнослужащих', 'Изменение карточек военнослужащих'),
('personnel:delete', 'Удаление военнослужащих', 'Удаление карточек военнослужащих'),
('unit:read', 'Просмотр частей', 'Просмотр военных частей'),
('unit:create', 'Создание частей', 'Создание военных частей'),
('unit:update', 'Изменение частей', 'Изменение военных частей'),
('unit:delete', 'Удаление частей', 'Удаление военных частей'),
('equipment:read', 'Просмотр техники', 'Просмотр записей о технике'),
('equipment:update', 'Изменение техники', 'Изменение записей о технике'),
('weapon:read', 'Просмотр вооружения', 'Просмотр записей о вооружении'),
('weapon:update', 'Изменение вооружения', 'Изменение записей о вооружении'),
('building:read', 'Просмотр сооружений', 'Просмотр сооружений и размещения подразделений'),
('building:update', 'Изменение сооружений', 'Изменение сооружений и назначений'),
('specialty:read', 'Просмотр специальностей', 'Просмотр военных специальностей'),
('query:execute', 'Выполнение аналитических запросов', 'Выполнение шаблонов терминала разведки'),
('alert:read', 'Просмотр предупреждений', 'Просмотр центра предупреждений'),
('alert:update', 'Обработка предупреждений', 'Подтверждение и закрытие предупреждений'),
('report:read', 'Просмотр отчётов', 'Просмотр тактических отчётов'),
('report:generate', 'Формирование отчётов', 'Формирование тактических отчётов'),
('user:manage', 'Управление пользователями', 'Управление пользователями, ролями и назначениями'),
('audit:read', 'Просмотр аудита', 'Просмотр журнала действий'),
('commander:assign', 'Назначение командиров', 'Назначение командиров на объекты структуры');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN_DISTRICT';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:read', 'structure:read', 'personnel:read', 'unit:read',
    'equipment:read', 'weapon:read', 'building:read', 'specialty:read',
    'query:execute', 'alert:read', 'report:read', 'audit:read'
)
WHERE r.code = 'STAFF_ANALYST';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:read', 'structure:read', 'personnel:read', 'personnel:update',
    'unit:read', 'unit:update', 'equipment:read', 'equipment:update',
    'weapon:read', 'weapon:update', 'building:read', 'building:update',
    'specialty:read', 'query:execute', 'alert:read', 'alert:update',
    'report:read', 'report:generate', 'commander:assign'
)
WHERE r.code IN ('ARMY_COMMANDER', 'FORMATION_COMMANDER', 'UNIT_COMMANDER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:read', 'structure:read', 'personnel:read', 'personnel:update',
    'unit:read', 'equipment:read', 'weapon:read', 'building:read',
    'specialty:read', 'alert:read', 'report:read'
)
WHERE r.code IN ('COMPANY_COMMANDER', 'PLATOON_COMMANDER', 'SQUAD_COMMANDER');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.code IN ('personnel:read', 'specialty:read')
WHERE r.code = 'SOLDIER';

INSERT INTO locations (location_id, city, address)
SELECT gs,
       (ARRAY[
           'Новосибирск', 'Омск', 'Барнаул', 'Томск', 'Кемерово', 'Красноярск',
           'Иркутск', 'Абакан', 'Бийск', 'Рубцовск', 'Бердск', 'Ачинск',
           'Братск', 'Норильск', 'Улан-Удэ', 'Чита', 'Канск', 'Тайга',
           'Ишим', 'Тобольск', 'Северск', 'Ангарск', 'Минусинск', 'Юрга',
           'Лесосибирск', 'Новоалтайск', 'Куйбышев', 'Искитим', 'Кызыл', 'Белово'
       ])[gs],
       'Гарнизонный сектор ' || lpad(gs::text, 2, '0')
FROM generate_series(1, 30) gs;

INSERT INTO military_formations (formation_id, name, formation_type, parent_id, formation_date, status)
VALUES (1, 'Сибирский тактический военный округ', 'Округ', NULL, DATE '2018-01-10', 'Активна');

INSERT INTO military_formations (formation_id, name, formation_type, parent_id, formation_date, status)
SELECT 10 + a,
       a || '-я общевойсковая армия "' ||
       (ARRAY['Северный щит', 'Енисей', 'Барс', 'Тайга', 'Ангара', 'Стальной рубеж'])[a] || '"',
       'Армия',
       1,
       DATE '2018-02-01' + (a * 17),
       'Активна'
FROM generate_series(1, 6) a;

INSERT INTO military_formations (formation_id, name, formation_type, parent_id, formation_date, status)
SELECT 1000 + a * 100 + f,
       CASE f
           WHEN 1 THEN (20 + a) || '-й армейский корпус'
           WHEN 2 THEN (40 + a) || '-я мотострелковая дивизия'
           WHEN 3 THEN (70 + a) || '-я гвардейская бригада'
           ELSE (90 + a) || '-я артиллерийская бригада'
       END || ' / ' || (ARRAY['Авангард', 'Кедр', 'Вектор', 'Гранит'])[f],
       CASE f
           WHEN 1 THEN 'Корпус'
           WHEN 2 THEN 'Дивизия'
           ELSE 'Бригада'
       END,
       10 + a,
       DATE '2018-04-01' + ((a * 31 + f * 11)::INTEGER),
       'Активна'
FROM generate_series(1, 6) a
CROSS JOIN generate_series(1, 4) f;

WITH unit_seed AS (
    SELECT a, f, u,
           10000 + a * 1000 + f * 100 + u AS unit_id,
           1000 + a * 100 + f AS formation_id
    FROM generate_series(1, 6) a
    CROSS JOIN generate_series(1, 4) f
    CROSS JOIN generate_series(1, 8) u
    WHERE u <= 5 + ((a + f) % 4)
)
INSERT INTO military_units (unit_id, name, formation_id, location_id)
SELECT unit_id,
       'Военная часть №' || (42000 + unit_id) || ' "' ||
       (ARRAY[
           'Орион', 'Рубеж', 'Вулкан', 'Сигнал', 'Вымпел', 'Форпост', 'Бастион', 'Арктика'
       ])[u] || '"',
       formation_id,
       ((a * 7 + f * 3 + u) % 30) + 1
FROM unit_seed;

WITH unit_seed AS (
    SELECT unit_id
    FROM military_units
)
INSERT INTO subdivisions (subdivision_id, name, type, unit_id, parent_id)
SELECT unit_id * 100 + c,
       c || '-я рота',
       'Рота',
       unit_id,
       NULL
FROM unit_seed
CROSS JOIN generate_series(1, 2) c;

WITH unit_seed AS (
    SELECT unit_id
    FROM military_units
)
INSERT INTO subdivisions (subdivision_id, name, type, unit_id, parent_id)
SELECT unit_id * 1000 + c * 10 + p,
       c || '-' || p || ' взвод',
       'Взвод',
       unit_id,
       unit_id * 100 + c
FROM unit_seed
CROSS JOIN generate_series(1, 2) c
CROSS JOIN generate_series(1, 2) p;

WITH unit_seed AS (
    SELECT unit_id
    FROM military_units
)
INSERT INTO subdivisions (subdivision_id, name, type, unit_id, parent_id)
SELECT unit_id * 10000 + c * 100 + p * 10 + s,
       c || '-' || p || '-' || s || ' отделение',
       'Отделение',
       unit_id,
       unit_id * 1000 + c * 10 + p
FROM unit_seed
CROSS JOIN generate_series(1, 2) c
CROSS JOIN generate_series(1, 2) p
CROSS JOIN generate_series(1, 3) s;

INSERT INTO military_ranks (rank_id, name, category) VALUES
(1, 'Генерал-полковник', 'Офицерский'),
(2, 'Генерал-лейтенант', 'Офицерский'),
(3, 'Генерал-майор', 'Офицерский'),
(4, 'Полковник', 'Офицерский'),
(5, 'Подполковник', 'Офицерский'),
(6, 'Майор', 'Офицерский'),
(7, 'Капитан', 'Офицерский'),
(8, 'Старший лейтенант', 'Офицерский'),
(9, 'Лейтенант', 'Офицерский'),
(10, 'Старший прапорщик', 'Сержантский и Рядовой'),
(11, 'Прапорщик', 'Сержантский и Рядовой'),
(12, 'Старшина', 'Сержантский и Рядовой'),
(13, 'Старший сержант', 'Сержантский и Рядовой'),
(14, 'Сержант', 'Сержантский и Рядовой'),
(15, 'Младший сержант', 'Сержантский и Рядовой'),
(16, 'Ефрейтор', 'Сержантский и Рядовой'),
(17, 'Рядовой', 'Сержантский и Рядовой');

INSERT INTO specialties (specialty_id, name) VALUES
(1, 'оператор связи'),
(2, 'механик'),
(3, 'военный медик'),
(4, 'специалист материального обеспечения'),
(5, 'стрелок'),
(6, 'механик-водитель БМП'),
(7, 'артиллерист'),
(8, 'оператор БПЛА'),
(9, 'инженер-сапёр'),
(10, 'оператор ПВО'),
(11, 'аналитик разведки'),
(12, 'начальник связи'),
(13, 'оружейный техник'),
(14, 'специалист ГСМ'),
(15, 'повар полевой кухни'),
(16, 'специалист киберзащиты'),
(17, 'специалист ракетных комплексов'),
(18, 'специалист эвакуации техники'),
(19, 'топогеодезист'),
(20, 'оператор арктических БПЛА');

WITH name_source AS (
    SELECT ARRAY[
        'Орлов', 'Соколов', 'Мельников', 'Кузнецов', 'Волков', 'Лебедев',
        'Новиков', 'Морозов', 'Павлов', 'Егоров', 'Федоров', 'Алексеев',
        'Семенов', 'Белов', 'Комаров', 'Макаров', 'Никитин', 'Зайцев',
        'Громов', 'Титов', 'Карпов', 'Савельев', 'Данилов', 'Быков'
    ] AS last_names,
    ARRAY[
        'Александр', 'Дмитрий', 'Сергей', 'Андрей', 'Иван', 'Михаил',
        'Павел', 'Виктор', 'Роман', 'Никита', 'Егор', 'Олег',
        'Кирилл', 'Антон', 'Илья', 'Матвей', 'Георгий', 'Денис'
    ] AS first_names,
    ARRAY[
        'Александрович', 'Дмитриевич', 'Сергеевич', 'Андреевич', 'Иванович',
        'Михайлович', 'Павлович', 'Викторович', 'Романович', 'Олегович'
    ] AS middle_names
),
personnel_seed AS (
    SELECT 1::BIGINT AS personnel_id, 'Орлов' AS last_name, 'Александр' AS first_name, 'Викторович' AS middle_name,
           'СО-000001' AS personal_number, DATE '1974-02-14' AS birth_date, DATE '1995-08-01' AS service_start,
           1110101::BIGINT AS subdivision_id
    UNION ALL
    SELECT 2, 'Семенова', 'Мария', 'Игоревна', 'СО-000002', DATE '1986-06-04', DATE '2008-09-01', 1110101
    UNION ALL
    SELECT 100 + a,
           (last_names)[1 + (a % array_length(last_names, 1))],
           (first_names)[1 + ((a + 3) % array_length(first_names, 1))],
           (middle_names)[1 + ((a + 4) % array_length(middle_names, 1))],
           'СО-' || lpad((100 + a)::text, 6, '0'),
           DATE '1976-01-01' + (a * 97),
           DATE '1998-01-15' + (a * 41),
           (10000 + a * 1000 + 100 + 1) * 100 + 1
    FROM generate_series(1, 6) a, name_source
    UNION ALL
    SELECT 10000 + a * 100 + f,
           (last_names)[1 + ((a * 4 + f) % array_length(last_names, 1))],
           (first_names)[1 + ((a * 5 + f) % array_length(first_names, 1))],
           (middle_names)[1 + ((a + f) % array_length(middle_names, 1))],
           'СО-' || lpad((10000 + a * 100 + f)::text, 6, '0'),
           DATE '1978-01-01' + ((a * 83 + f * 19)::INTEGER),
           DATE '2000-01-15' + ((a * 37 + f * 23)::INTEGER),
           (10000 + a * 1000 + f * 100 + 1) * 100 + 1
    FROM generate_series(1, 6) a
    CROSS JOIN generate_series(1, 4) f
    CROSS JOIN name_source
    UNION ALL
    SELECT mu.unit_id,
           (last_names)[1 + ((mu.unit_id + 2) % array_length(last_names, 1))],
           (first_names)[1 + ((mu.unit_id + 5) % array_length(first_names, 1))],
           (middle_names)[1 + ((mu.unit_id + 7) % array_length(middle_names, 1))],
           'СО-' || lpad(mu.unit_id::text, 6, '0'),
           DATE '1981-01-01' + ((mu.unit_id % 4000)::INTEGER),
           DATE '2003-01-01' + ((mu.unit_id % 5000)::INTEGER),
           mu.unit_id * 100 + 1
    FROM military_units mu, name_source
    UNION ALL
    SELECT s.subdivision_id,
           (last_names)[1 + ((s.subdivision_id + 3) % array_length(last_names, 1))],
           (first_names)[1 + ((s.subdivision_id + 6) % array_length(first_names, 1))],
           (middle_names)[1 + ((s.subdivision_id + 9) % array_length(middle_names, 1))],
           'СО-' || s.subdivision_id::text,
           DATE '1988-01-01' + ((s.subdivision_id % 3000)::INTEGER),
           DATE '2010-01-01' + ((s.subdivision_id % 3500)::INTEGER),
           s.subdivision_id
    FROM subdivisions s, name_source
    UNION ALL
    SELECT s.subdivision_id * 10 + n,
           (last_names)[1 + ((s.subdivision_id + n) % array_length(last_names, 1))],
           (first_names)[1 + ((s.subdivision_id + n * 2) % array_length(first_names, 1))],
           (middle_names)[1 + ((s.subdivision_id + n * 3) % array_length(middle_names, 1))],
           'СО-' || (s.subdivision_id * 10 + n)::text,
           DATE '1995-01-01' + (((s.subdivision_id + n * 13) % 4200)::INTEGER),
           DATE '2018-01-01' + (((s.subdivision_id + n * 17) % 2500)::INTEGER),
           s.subdivision_id
    FROM subdivisions s
    CROSS JOIN generate_series(1, 4) n
    CROSS JOIN name_source
    WHERE s.type = 'Отделение'
)
INSERT INTO personnel (
    personnel_id, last_name, first_name, middle_name, personal_number,
    birth_date, service_start, subdivision_id
)
SELECT personnel_id, last_name, first_name, middle_name, personal_number,
       birth_date, service_start, subdivision_id
FROM personnel_seed;

INSERT INTO personnel_ranks (personnel_id, rank_id, assignment_date)
SELECT p.personnel_id,
       CASE
           WHEN p.personnel_id = 1 THEN 1
           WHEN p.personnel_id = 2 THEN 5
           WHEN p.personnel_id BETWEEN 101 AND 106 THEN 2
           WHEN p.personnel_id BETWEEN 10101 AND 10604 THEN CASE WHEN p.personnel_id % 2 = 0 THEN 4 ELSE 3 END
           WHEN p.personnel_id IN (SELECT unit_id FROM military_units) THEN CASE WHEN p.personnel_id % 3 = 0 THEN 5 ELSE 6 END
           WHEN p.personnel_id IN (SELECT subdivision_id FROM subdivisions WHERE type = 'Рота') THEN 7
           WHEN p.personnel_id IN (SELECT subdivision_id FROM subdivisions WHERE type = 'Взвод') THEN CASE WHEN p.personnel_id % 2 = 0 THEN 8 ELSE 9 END
           WHEN p.personnel_id IN (SELECT subdivision_id FROM subdivisions WHERE type = 'Отделение') THEN CASE WHEN p.personnel_id % 2 = 0 THEN 13 ELSE 14 END
           WHEN p.personnel_id % 10 = 4 THEN 15
           WHEN p.personnel_id % 10 = 3 THEN 16
           ELSE 17
       END,
       GREATEST(p.service_start, DATE '2019-01-01')
FROM personnel p;

INSERT INTO personnel_specialties (personnel_id, specialty_id)
SELECT p.personnel_id,
       CASE
           WHEN p.personnel_id = 1 THEN 12
           WHEN p.personnel_id = 2 THEN 11
           WHEN p.personnel_id BETWEEN 101 AND 106 THEN 11
           WHEN p.personnel_id BETWEEN 10101 AND 10604 THEN 12
           WHEN p.personnel_id IN (SELECT unit_id FROM military_units) THEN 4
           WHEN p.personnel_id IN (SELECT subdivision_id FROM subdivisions WHERE type = 'Рота') THEN 4
           WHEN p.personnel_id IN (SELECT subdivision_id FROM subdivisions WHERE type = 'Взвод') THEN 1
           WHEN p.personnel_id IN (SELECT subdivision_id FROM subdivisions WHERE type = 'Отделение') THEN 5
           ELSE 1 + ((p.personnel_id % 18)::INTEGER)
       END
FROM personnel p
WHERE CASE
    WHEN p.personnel_id % 97 = 0 THEN FALSE
    ELSE TRUE
END;

INSERT INTO personnel_specialties (personnel_id, specialty_id)
SELECT p.personnel_id, 3
FROM personnel p
WHERE p.personnel_id % 29 = 0
ON CONFLICT DO NOTHING;

INSERT INTO personnel_specialties (personnel_id, specialty_id)
SELECT p.personnel_id, 17
FROM personnel p
WHERE p.personnel_id % 41 = 0
ON CONFLICT DO NOTHING;

UPDATE military_formations SET commander_id = 1 WHERE formation_id = 1;

UPDATE military_formations
SET commander_id = formation_id + 90
WHERE formation_type = 'Армия';

UPDATE military_formations
SET commander_id = 10000 + ((formation_id - 1000) / 100) * 100 + ((formation_id - 1000) % 100)
WHERE formation_type IN ('Корпус', 'Дивизия', 'Бригада');

UPDATE military_units
SET commander_id = unit_id;

UPDATE subdivisions
SET commander_id = subdivision_id;

INSERT INTO equipment_categories (category_id, name) VALUES
(1, 'бронетехника'),
(2, 'транспорт'),
(3, 'средства связи'),
(4, 'инженерная техника'),
(5, 'беспилотные комплексы'),
(6, 'обеспечение');

INSERT INTO equipment_types (type_id, name, category_id) VALUES
(1, 'BMP-2', 1),
(2, 'BTR-82A', 1),
(3, 'T-72B3', 1),
(4, 'Урал-4320', 2),
(5, 'КамАЗ-5350', 2),
(6, 'радиостанция Р-168', 3),
(7, 'инженерная машина разграждения ИМР-2', 4),
(8, 'путепрокладчик БАТ-2', 4),
(9, 'БПЛА Орлан-10', 5),
(10, 'полевая кухня КП-130', 6),
(11, 'автотопливозаправщик АТЗ-5', 6);

INSERT INTO weapon_categories (category_id, name) VALUES
(1, 'стрелковое оружие'),
(2, 'противотанковые средства'),
(3, 'миномёты'),
(4, 'артиллерия'),
(5, 'ракетные комплексы'),
(6, 'противовоздушная оборона');

INSERT INTO weapon_types (type_id, name, category_id) VALUES
(1, 'AK-74M', 1),
(2, 'PKM', 1),
(3, 'SVD', 1),
(4, 'RPG-7', 2),
(5, 'AGS-17', 2),
(6, '2B14 Podnos', 3),
(7, '2A65 Msta-B', 4),
(8, '9K115 Metis', 5),
(9, 'ПТРК Корнет', 5),
(10, 'ПЗРК Игла', 6);

INSERT INTO equipment_in_units (unit_id, type_id, quantity)
SELECT unit_id, type_id,
       CASE
           WHEN type_id = 1 AND (unit_id = 11101 OR unit_id % 29 = 0) THEN 320
           WHEN type_id = 1 THEN 18 + (unit_id % 12)
           WHEN type_id = 2 THEN 8 + (unit_id % 10)
           WHEN type_id = 3 THEN 4 + (unit_id % 8)
           WHEN type_id = 4 THEN 40 + (unit_id % 35)
           WHEN type_id = 5 THEN 22 + (unit_id % 20)
           WHEN type_id = 6 THEN 12 + (unit_id % 18)
           WHEN type_id = 7 THEN 2 + (unit_id % 4)
           WHEN type_id = 8 THEN 1 + (unit_id % 3)
           WHEN type_id = 9 THEN 3 + (unit_id % 6)
           WHEN type_id = 10 THEN 2
           ELSE 4 + (unit_id % 7)
       END
FROM military_units
CROSS JOIN LATERAL (
    VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11)
) t(type_id)
WHERE unit_id % 31 <> 0
  AND NOT (type_id = 1 AND unit_id % 7 = 0)
  AND NOT (type_id IN (3, 7, 8, 9) AND unit_id % 4 = 0)
  AND NOT (type_id IN (2, 10, 11) AND unit_id % 6 = 0);

INSERT INTO weapon_in_units (unit_id, type_id, quantity)
SELECT unit_id, type_id,
       CASE
           WHEN type_id = 1 AND (unit_id = 11101 OR unit_id % 41 = 0) THEN 1250
           WHEN type_id = 1 THEN 230 + (unit_id % 180)
           WHEN type_id = 2 THEN 32 + (unit_id % 40)
           WHEN type_id = 3 THEN 18 + (unit_id % 16)
           WHEN type_id = 4 THEN 24 + (unit_id % 18)
           WHEN type_id = 5 THEN 12 + (unit_id % 10)
           WHEN type_id = 6 THEN 8 + (unit_id % 8)
           WHEN type_id = 7 THEN 4 + (unit_id % 6)
           WHEN type_id = 8 THEN 5 + (unit_id % 7)
           WHEN type_id = 9 THEN 3 + (unit_id % 5)
           ELSE 6 + (unit_id % 8)
       END
FROM military_units
CROSS JOIN LATERAL (
    VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10)
) t(type_id)
WHERE unit_id % 37 <> 0
  AND NOT (type_id IN (8, 9) AND unit_id % 5 = 0)
  AND NOT (type_id IN (7, 10) AND unit_id % 6 = 0);

INSERT INTO buildings (building_id, name, unit_id)
SELECT unit_id * 10 + b,
       (ARRAY[
           'Командный пункт', 'Казарма', 'Парк техники', 'Учебный комплекс', 'Склад материального обеспечения'
       ])[b] || ' / ' || unit_id,
       unit_id
FROM military_units
CROSS JOIN generate_series(1, 5) b;

INSERT INTO subdivision_buildings (subdivision_id, building_id)
SELECT s.subdivision_id, s.unit_id * 10 + 1
FROM subdivisions s
WHERE (s.type = 'Рота' AND s.name LIKE '1-%')
   OR (s.unit_id % 11 = 0 AND s.type IN ('Рота', 'Взвод'));

INSERT INTO subdivision_buildings (subdivision_id, building_id)
SELECT s.subdivision_id, s.unit_id * 10 + 2
FROM subdivisions s
WHERE s.type = 'Отделение'
  AND s.subdivision_id % 4 = 0;

INSERT INTO subdivision_buildings (subdivision_id, building_id)
SELECT s.subdivision_id, s.unit_id * 10 + 3
FROM subdivisions s
WHERE s.type = 'Рота';

INSERT INTO subdivision_buildings (subdivision_id, building_id)
SELECT s.subdivision_id, s.unit_id * 10 + 4
FROM subdivisions s
WHERE s.type = 'Взвод'
  AND s.subdivision_id % 2 = 0;

INSERT INTO subdivision_buildings (subdivision_id, building_id)
SELECT s.subdivision_id, s.unit_id * 10 + 5
FROM subdivisions s
WHERE s.type = 'Рота'
  AND s.name LIKE '2-%'
  AND s.unit_id % 5 <> 0;

INSERT INTO users (username, password_hash, display_name, personnel_id, is_active) VALUES
('admin.district', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Администратор округа', 1, TRUE),
('analyst.staff', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Аналитик штаба', 2, TRUE),
('army.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Командующий 1-й армией', 101, TRUE),
('formation.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Командир 21-го корпуса', 10101, TRUE),
('unit.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Командир военной части №53101', 11101, TRUE),
('company.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Командир 1-й роты', 1110101, TRUE),
('platoon.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Командир 1-1 взвода', 11101011, TRUE),
('squad.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Командир 1-1-1 отделения', 111010111, TRUE),
('soldier.demo', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Военнослужащий Дмитрий Крылов', 1110101111, TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON (
    (u.username = 'admin.district' AND r.code = 'ADMIN_DISTRICT')
    OR (u.username = 'analyst.staff' AND r.code = 'STAFF_ANALYST')
    OR (u.username = 'army.cmd.1' AND r.code = 'ARMY_COMMANDER')
    OR (u.username = 'formation.cmd.1' AND r.code = 'FORMATION_COMMANDER')
    OR (u.username = 'unit.cmd.1' AND r.code = 'UNIT_COMMANDER')
    OR (u.username = 'company.cmd.1' AND r.code = 'COMPANY_COMMANDER')
    OR (u.username = 'platoon.cmd.1' AND r.code = 'PLATOON_COMMANDER')
    OR (u.username = 'squad.cmd.1' AND r.code = 'SQUAD_COMMANDER')
    OR (u.username = 'soldier.demo' AND r.code = 'SOLDIER')
);

INSERT INTO command_assignments (soldier_id, object_type, object_id, starts_at, is_primary)
VALUES
(1, 'DISTRICT', 1, DATE '2018-01-10', TRUE),
(2, 'DISTRICT', 1, DATE '2021-04-01', TRUE),
(1110101111, 'SELF', 1110101111, DATE '2022-11-01', TRUE);

INSERT INTO command_assignments (soldier_id, object_type, object_id, starts_at, is_primary)
SELECT 100 + a, 'ARMY', 10 + a, DATE '2020-01-01' + (a * 19), TRUE
FROM generate_series(1, 6) a;

INSERT INTO command_assignments (soldier_id, object_type, object_id, starts_at, is_primary)
SELECT commander_id,
       CASE formation_type
           WHEN 'Корпус' THEN 'CORPS'
           WHEN 'Дивизия' THEN 'DIVISION'
           WHEN 'Бригада' THEN 'BRIGADE'
           ELSE 'FORMATION'
       END,
       formation_id,
       DATE '2020-03-01' + ((formation_id % 365)::INTEGER),
       TRUE
FROM military_formations
WHERE formation_type IN ('Корпус', 'Дивизия', 'Бригада');

INSERT INTO command_assignments (soldier_id, object_type, object_id, starts_at, is_primary)
SELECT commander_id, 'MILITARY_UNIT', unit_id, DATE '2021-01-01' + ((unit_id % 500)::INTEGER), TRUE
FROM military_units;

INSERT INTO command_assignments (soldier_id, object_type, object_id, starts_at, is_primary)
SELECT commander_id,
       CASE type
           WHEN 'Рота' THEN 'COMPANY'
           WHEN 'Взвод' THEN 'PLATOON'
           WHEN 'Отделение' THEN 'SQUAD'
           ELSE 'BATTALION'
       END,
       subdivision_id,
       DATE '2021-06-01' + ((subdivision_id % 600)::INTEGER),
       TRUE
FROM subdivisions;

WITH unit_sample AS (
    SELECT unit_id, row_number() OVER (ORDER BY unit_id) AS rn
    FROM military_units
),
personnel_sample AS (
    SELECT personnel_id, row_number() OVER (ORDER BY personnel_id) AS rn
    FROM personnel
    WHERE personnel_id > 1000000000
    LIMIT 240
),
building_sample AS (
    SELECT building_id, row_number() OVER (ORDER BY building_id) AS rn
    FROM buildings
),
actors AS (
    SELECT username, row_number() OVER (ORDER BY username) AS rn
    FROM users
),
events AS (
    SELECT gs,
           (ARRAY['СОЗДАНИЕ', 'ИЗМЕНЕНИЕ', 'НАЗНАЧЕНИЕ_КОМАНДИРА', 'ПЕРЕСЧЁТ_ИНВЕНТАРЯ', 'АНАЛИТИЧЕСКИЙ_ЗАПРОС', 'ФОРМИРОВАНИЕ_ОТЧЁТА'])[1 + (gs % 6)] AS action,
           (ARRAY['MILITARY_UNIT', 'PERSONNEL', 'BUILDING', 'EQUIPMENT', 'WEAPON'])[1 + (gs % 5)] AS object_type,
           (ARRAY[
               'Инвентаризация в области доступа синхронизирована',
               'Назначение командира проверено',
               'Показатели готовности пересчитаны',
               'Сценарий терминала разведки выполнен',
               'Предпросмотр тактического отчёта сформирован',
               'Данные личного состава обновлены'
           ])[1 + (gs % 6)] AS details
    FROM generate_series(1, 220) gs
)
INSERT INTO audit_events (actor_user_id, actor_username, action, object_type, object_id, details, created_at)
SELECT u.user_id,
       u.username,
       e.action,
       e.object_type,
       CASE e.object_type
           WHEN 'MILITARY_UNIT' THEN us.unit_id
           WHEN 'PERSONNEL' THEN ps.personnel_id
           WHEN 'BUILDING' THEN bs.building_id
           WHEN 'EQUIPMENT' THEN us.unit_id
           ELSE us.unit_id
       END,
       e.details,
       now() - (e.gs || ' hours')::INTERVAL
FROM events e
JOIN actors a ON a.rn = 1 + (e.gs % (SELECT COUNT(*) FROM actors))
JOIN users u ON u.username = a.username
JOIN unit_sample us ON us.rn = 1 + (e.gs % (SELECT COUNT(*) FROM unit_sample))
JOIN personnel_sample ps ON ps.rn = 1 + (e.gs % (SELECT COUNT(*) FROM personnel_sample))
JOIN building_sample bs ON bs.rn = 1 + (e.gs % (SELECT COUNT(*) FROM building_sample));

SELECT setval('locations_location_id_seq', (SELECT MAX(location_id) FROM locations));
SELECT setval('military_formations_formation_id_seq', (SELECT MAX(formation_id) FROM military_formations));
SELECT setval('military_units_unit_id_seq', (SELECT MAX(unit_id) FROM military_units));
SELECT setval('subdivisions_subdivision_id_seq', (SELECT MAX(subdivision_id) FROM subdivisions));
SELECT setval('personnel_personnel_id_seq', (SELECT MAX(personnel_id) FROM personnel));
SELECT setval('military_ranks_rank_id_seq', (SELECT MAX(rank_id) FROM military_ranks));
SELECT setval('specialties_specialty_id_seq', (SELECT MAX(specialty_id) FROM specialties));
SELECT setval('users_user_id_seq', (SELECT MAX(user_id) FROM users));
SELECT setval('roles_role_id_seq', (SELECT MAX(role_id) FROM roles));
SELECT setval('permissions_permission_id_seq', (SELECT MAX(permission_id) FROM permissions));
SELECT setval('command_assignments_assignment_id_seq', (SELECT MAX(assignment_id) FROM command_assignments));
SELECT setval('equipment_categories_category_id_seq', (SELECT MAX(category_id) FROM equipment_categories));
SELECT setval('equipment_types_type_id_seq', (SELECT MAX(type_id) FROM equipment_types));
SELECT setval('weapon_categories_category_id_seq', (SELECT MAX(category_id) FROM weapon_categories));
SELECT setval('weapon_types_type_id_seq', (SELECT MAX(type_id) FROM weapon_types));
SELECT setval('buildings_building_id_seq', (SELECT MAX(building_id) FROM buildings));
SELECT setval('audit_events_audit_event_id_seq', (SELECT MAX(audit_event_id) FROM audit_events));
