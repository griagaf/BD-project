UPDATE equipment_types SET name = 'БМП-2' WHERE name IN ('BMP-2');
UPDATE equipment_types SET name = 'БТР-82А' WHERE name IN ('BTR-82A');
UPDATE equipment_types SET name = 'Т-72Б3' WHERE name IN ('T-72B3');

UPDATE weapon_types SET name = 'АК-74М' WHERE name IN ('AK-74M');
UPDATE weapon_types SET name = 'ПКМ' WHERE name IN ('PKM');
UPDATE weapon_types SET name = 'СВД' WHERE name IN ('SVD');
UPDATE weapon_types SET name = 'РПГ-7' WHERE name IN ('RPG-7');
UPDATE weapon_types SET name = 'АГС-17' WHERE name IN ('AGS-17');
UPDATE weapon_types SET name = '2Б14 Поднос' WHERE name IN ('2B14 Podnos');
UPDATE weapon_types SET name = '2А65 Мста-Б' WHERE name IN ('2A65 Msta-B');
UPDATE weapon_types SET name = '9К115 Метис' WHERE name IN ('9K115 Metis');

WITH ranked_assignments AS (
    SELECT subdivision_id,
           building_id,
           row_number() OVER (
               PARTITION BY subdivision_id
               ORDER BY (SELECT assignable FROM buildings WHERE buildings.building_id = subdivision_buildings.building_id) DESC,
                        building_id
           ) AS rn
    FROM subdivision_buildings
)
DELETE FROM subdivision_buildings sb
USING ranked_assignments ra
WHERE sb.subdivision_id = ra.subdivision_id
  AND sb.building_id = ra.building_id
  AND ra.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS ux_subdivision_single_building_assignment
    ON subdivision_buildings(subdivision_id);

CREATE OR REPLACE FUNCTION tdc_validate_subdivision_building_assignment()
RETURNS TRIGGER AS $$
DECLARE
    subdivision_unit BIGINT;
    building_unit BIGINT;
    allowed BOOLEAN;
BEGIN
    SELECT unit_id INTO subdivision_unit
    FROM subdivisions
    WHERE subdivision_id = NEW.subdivision_id;

    SELECT unit_id, assignable INTO building_unit, allowed
    FROM buildings
    WHERE building_id = NEW.building_id;

    IF subdivision_unit IS NULL THEN
        RAISE EXCEPTION 'SUBDIVISION_NOT_FOUND';
    END IF;

    IF building_unit IS NULL THEN
        RAISE EXCEPTION 'BUILDING_NOT_FOUND';
    END IF;

    IF subdivision_unit <> building_unit THEN
        RAISE EXCEPTION 'SUBDIVISION_BUILDING_UNIT_MISMATCH';
    END IF;

    IF allowed IS DISTINCT FROM TRUE THEN
        RAISE EXCEPTION 'BUILDING_NOT_ASSIGNABLE';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_subdivision_building_unit_guard ON subdivision_buildings;
DROP TRIGGER IF EXISTS trg_building_assignment_assignable_guard ON subdivision_buildings;
DROP TRIGGER IF EXISTS trg_subdivision_building_assignment_guard ON subdivision_buildings;
CREATE TRIGGER trg_subdivision_building_assignment_guard
BEFORE INSERT OR UPDATE ON subdivision_buildings
FOR EACH ROW
EXECUTE FUNCTION tdc_validate_subdivision_building_assignment();

INSERT INTO equipment_attribute_types (name, data_type) VALUES
('десант', 'number'),
('тип вооружения', 'text'),
('тяговое усилие, тс', 'number'),
('грузоподъёмность, т', 'number'),
('тип двигателя', 'text'),
('расход топлива, л/100 км', 'number'),
('тип кузова', 'text'),
('количество мест', 'number'),
('рабочее оборудование', 'text'),
('производительность', 'text'),
('радиус связи, км', 'number'),
('диапазон частот', 'text'),
('время развёртывания, мин', 'number'),
('радиус наблюдения, км', 'number'),
('продолжительность полёта, ч', 'number'),
('тип полезной нагрузки', 'text'),
('тип обеспечения', 'text'),
('суточная производительность', 'text')
ON CONFLICT (name) DO UPDATE SET data_type = EXCLUDED.data_type;

DELETE FROM equipment_category_attributes;

INSERT INTO equipment_category_attributes (category_id, attribute_id, is_required)
SELECT ec.category_id, eat.attribute_id, eat.name IN ('назначение', 'экипаж')
FROM equipment_categories ec
JOIN equipment_attribute_types eat ON
    (ec.name ILIKE '%брон%' AND eat.name IN ('назначение', 'экипаж', 'десант', 'масса, т', 'скорость, км/ч', 'запас хода, км', 'тип вооружения', 'производитель')) OR
    (ec.name ILIKE '%транспорт%' AND eat.name IN ('назначение', 'грузоподъёмность, т', 'тип кузова', 'количество мест', 'тип двигателя', 'расход топлива, л/100 км', 'запас хода, км', 'производитель')) OR
    (ec.name ILIKE '%связ%' AND eat.name IN ('назначение', 'экипаж', 'радиус связи, км', 'диапазон частот', 'время развёртывания, мин', 'производитель')) OR
    (ec.name ILIKE '%инженер%' AND eat.name IN ('назначение', 'рабочее оборудование', 'производительность', 'экипаж', 'масса, т', 'скорость, км/ч', 'производитель')) OR
    (ec.name ILIKE '%беспилот%' AND eat.name IN ('назначение', 'экипаж', 'радиус наблюдения, км', 'продолжительность полёта, ч', 'тип полезной нагрузки', 'производитель')) OR
    (ec.name ILIKE '%обеспеч%' AND eat.name IN ('назначение', 'тип обеспечения', 'суточная производительность', 'экипаж', 'производитель'))
ON CONFLICT (category_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required;

INSERT INTO weapon_attribute_types (name, data_type) VALUES
('ёмкость магазина', 'number'),
('режимы стрельбы', 'text'),
('эффективная дальность, м', 'number'),
('калибр орудия', 'text'),
('максимальная дальность, м', 'number'),
('тип боеприпаса', 'text'),
('расчёт', 'number'),
('дальность пуска, м', 'number'),
('тип наведения', 'text'),
('масса боевой части, кг', 'number'),
('тип платформы', 'text'),
('тип выстрела', 'text'),
('прицельная дальность, м', 'number'),
('масса, кг', 'number'),
('зона поражения', 'text')
ON CONFLICT (name) DO UPDATE SET data_type = EXCLUDED.data_type;

DELETE FROM weapon_category_attributes;

INSERT INTO weapon_category_attributes (category_id, attribute_id, is_required)
SELECT wc.category_id, wat.attribute_id, wat.name IN ('назначение', 'калибр')
FROM weapon_categories wc
JOIN weapon_attribute_types wat ON
    (wc.name ILIKE '%стрел%' AND wat.name IN ('назначение', 'калибр', 'ёмкость магазина', 'режимы стрельбы', 'эффективная дальность, м', 'производитель')) OR
    (wc.name ILIKE '%противотанк%' AND wat.name IN ('назначение', 'калибр', 'тип выстрела', 'прицельная дальность, м', 'масса, кг', 'производитель')) OR
    (wc.name ILIKE '%мином%' AND wat.name IN ('назначение', 'калибр орудия', 'максимальная дальность, м', 'тип боеприпаса', 'расчёт', 'производитель')) OR
    (wc.name ILIKE '%артил%' AND wat.name IN ('назначение', 'калибр орудия', 'максимальная дальность, м', 'тип боеприпаса', 'расчёт', 'производитель')) OR
    (wc.name ILIKE '%ракет%' AND wat.name IN ('назначение', 'дальность пуска, м', 'тип наведения', 'масса боевой части, кг', 'тип платформы', 'производитель')) OR
    (wc.name ILIKE '%воздуш%' AND wat.name IN ('назначение', 'дальность пуска, м', 'тип наведения', 'зона поражения', 'тип платформы', 'производитель'))
ON CONFLICT (category_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required;

INSERT INTO equipment_type_attribute_values (type_id, attribute_id, value_text, value_number)
SELECT et.type_id,
       eat.attribute_id,
       CASE eat.name
           WHEN 'назначение' THEN et.purpose
           WHEN 'тип вооружения' THEN CASE WHEN et.name ILIKE '%Т-72%' THEN '125-мм пушка и спаренный пулемёт' WHEN et.name ILIKE '%БМП%' THEN '30-мм пушка и ПТРК' WHEN et.name ILIKE '%БТР%' THEN '30-мм автоматическая пушка' ELSE 'штатное вооружение машины' END
           WHEN 'тип двигателя' THEN 'дизельный'
           WHEN 'тип кузова' THEN CASE WHEN et.name ILIKE '%Урал%' THEN 'бортовой повышенной проходимости' WHEN et.name ILIKE '%КамАЗ%' THEN 'многоцелевое шасси' ELSE 'специальное шасси' END
           WHEN 'рабочее оборудование' THEN CASE WHEN et.name ILIKE '%ИМР%' THEN 'кран-манипулятор, бульдозерный отвал, трал' WHEN et.name ILIKE '%БАТ%' THEN 'бульдозерное оборудование и рыхлитель' ELSE 'штатное рабочее оборудование' END
           WHEN 'производительность' THEN CASE WHEN et.name ILIKE '%ИМР%' THEN 'расчистка завалов и проходов' WHEN et.name ILIKE '%БАТ%' THEN 'прокладка колонных путей' ELSE 'инженерное обеспечение подразделений' END
           WHEN 'диапазон частот' THEN 'КВ/УКВ тактический диапазон'
           WHEN 'тип полезной нагрузки' THEN 'оптико-электронная разведка'
           WHEN 'тип обеспечения' THEN 'материально-техническое обеспечение'
           WHEN 'суточная производительность' THEN CASE WHEN et.name ILIKE '%кухн%' THEN 'до 130 человек в смену' WHEN et.name ILIKE '%топлив%' THEN 'выдача топлива подразделениям части' ELSE 'обеспечение подразделений части' END
           WHEN 'производитель' THEN et.manufacturer
           ELSE NULL
       END,
       CASE eat.name
           WHEN 'экипаж' THEN et.crew_size::NUMERIC
           WHEN 'десант' THEN CASE WHEN et.name ILIKE '%БМП%' THEN 7 WHEN et.name ILIKE '%БТР%' THEN 8 ELSE 0 END
           WHEN 'масса, т' THEN et.weight_tons
           WHEN 'скорость, км/ч' THEN et.max_speed_kmh::NUMERIC
           WHEN 'запас хода, км' THEN et.operational_range_km::NUMERIC
           WHEN 'тяговое усилие, тс' THEN CASE WHEN et.name ILIKE '%БАТ%' THEN 25 ELSE 12 END
           WHEN 'грузоподъёмность, т' THEN CASE WHEN et.name ILIKE '%Урал%' THEN 5 WHEN et.name ILIKE '%КамАЗ%' THEN 6 WHEN et.name ILIKE '%топлив%' THEN 5 ELSE 2 END
           WHEN 'расход топлива, л/100 км' THEN CASE WHEN ec.name ILIKE '%транспорт%' THEN 35 WHEN ec.name ILIKE '%обеспеч%' THEN 38 ELSE 30 END
           WHEN 'количество мест' THEN CASE WHEN ec.name ILIKE '%транспорт%' THEN 3 ELSE 2 END
           WHEN 'радиус связи, км' THEN 40
           WHEN 'время развёртывания, мин' THEN 12
           WHEN 'радиус наблюдения, км' THEN 120
           WHEN 'продолжительность полёта, ч' THEN 10
           ELSE NULL
       END
FROM equipment_types et
JOIN equipment_categories ec ON ec.category_id = et.category_id
JOIN equipment_category_attributes eca ON eca.category_id = ec.category_id
JOIN equipment_attribute_types eat ON eat.attribute_id = eca.attribute_id
WHERE et.archived = FALSE
ON CONFLICT (type_id, attribute_id) DO UPDATE SET
    value_text = EXCLUDED.value_text,
    value_number = EXCLUDED.value_number,
    value_date = NULL,
    value_boolean = NULL;

INSERT INTO weapon_type_attribute_values (type_id, attribute_id, value_text, value_number)
SELECT wt.type_id,
       wat.attribute_id,
       CASE wat.name
           WHEN 'назначение' THEN wt.purpose
           WHEN 'калибр' THEN wt.caliber
           WHEN 'калибр орудия' THEN wt.caliber
           WHEN 'режимы стрельбы' THEN 'одиночный и автоматический'
           WHEN 'тип боеприпаса' THEN CASE WHEN wc.name ILIKE '%мином%' THEN 'осколочно-фугасная мина' WHEN wc.name ILIKE '%артил%' THEN 'осколочно-фугасный снаряд' ELSE 'штатный боеприпас' END
           WHEN 'тип наведения' THEN CASE WHEN wc.name ILIKE '%ракет%' THEN 'полуавтоматическое по лучу' WHEN wc.name ILIKE '%воздуш%' THEN 'инфракрасное самонаведение' ELSE 'механическое наведение' END
           WHEN 'тип платформы' THEN CASE WHEN wc.name ILIKE '%ракет%' THEN 'переносной/возимый комплекс' WHEN wc.name ILIKE '%воздуш%' THEN 'переносной комплекс' ELSE 'расчётная позиция' END
           WHEN 'тип выстрела' THEN CASE WHEN wt.name ILIKE '%РПГ%' THEN 'кумулятивный выстрел' WHEN wt.name ILIKE '%АГС%' THEN 'осколочная граната' ELSE 'штатный выстрел' END
           WHEN 'зона поражения' THEN 'низковысотные воздушные цели'
           WHEN 'производитель' THEN wt.manufacturer
           ELSE NULL
       END,
       CASE wat.name
           WHEN 'дальность, м' THEN wt.effective_range_m::NUMERIC
           WHEN 'эффективная дальность, м' THEN wt.effective_range_m::NUMERIC
           WHEN 'максимальная дальность, м' THEN wt.effective_range_m::NUMERIC
           WHEN 'дальность пуска, м' THEN wt.effective_range_m::NUMERIC
           WHEN 'прицельная дальность, м' THEN wt.effective_range_m::NUMERIC
           WHEN 'ёмкость магазина' THEN CASE WHEN wt.name ILIKE '%АК%' THEN 30 WHEN wt.name ILIKE '%ПКМ%' THEN 100 WHEN wt.name ILIKE '%СВД%' THEN 10 ELSE NULL END
           WHEN 'расчёт' THEN CASE WHEN wc.name ILIKE '%мином%' THEN 4 WHEN wc.name ILIKE '%артил%' THEN 8 ELSE NULL END
           WHEN 'масса боевой части, кг' THEN CASE WHEN wc.name ILIKE '%ракет%' THEN 7 WHEN wc.name ILIKE '%воздуш%' THEN 1.2 ELSE 1 END
           WHEN 'масса, кг' THEN CASE WHEN wt.name ILIKE '%РПГ%' THEN 7 WHEN wt.name ILIKE '%АГС%' THEN 31 ELSE 5 END
           ELSE NULL
       END
FROM weapon_types wt
JOIN weapon_categories wc ON wc.category_id = wt.category_id
JOIN weapon_category_attributes wca ON wca.category_id = wc.category_id
JOIN weapon_attribute_types wat ON wat.attribute_id = wca.attribute_id
WHERE wt.archived = FALSE
ON CONFLICT (type_id, attribute_id) DO UPDATE SET
    value_text = EXCLUDED.value_text,
    value_number = EXCLUDED.value_number,
    value_date = NULL,
    value_boolean = NULL;

INSERT INTO rank_attribute_types (name, data_type) VALUES
('дата окончания академии', 'date'),
('дата присвоения генеральского звания', 'date'),
('стратегическая специализация', 'text'),
('опыт командования частью', 'text'),
('количество лет в офицерском составе', 'number'),
('профиль командования', 'text'),
('опыт штабной работы', 'text'),
('профиль подготовки', 'text'),
('количество подчинённых подразделений', 'number'),
('направление службы', 'text'),
('опыт оперативного планирования', 'text'),
('уровень допуска', 'text'),
('командирский стаж', 'number'),
('профиль роты', 'text'),
('уровень подготовки', 'text'),
('военное училище', 'text'),
('год выпуска', 'number'),
('первичная специальность', 'text'),
('техническая специализация', 'text'),
('стаж службы', 'number'),
('опыт работы с личным составом', 'text'),
('направление подготовки', 'text'),
('уровень дисциплинарной ответственности', 'text'),
('командирская подготовка', 'text'),
('специализация отделения', 'text'),
('категория инструктора', 'text'),
('основная специальность', 'text'),
('допуск к технике', 'text'),
('базовая специальность', 'text'),
('дата зачисления в подразделение', 'date')
ON CONFLICT (name) DO UPDATE SET data_type = EXCLUDED.data_type;

DELETE FROM rank_type_attributes;

INSERT INTO rank_type_attributes (rank_id, attribute_id, is_required)
SELECT mr.rank_id, rat.attribute_id, TRUE
FROM military_ranks mr
JOIN rank_attribute_types rat ON
    (mr.name ILIKE '%генерал%' AND rat.name IN ('уровень командования', 'дата окончания академии', 'дата присвоения генеральского звания', 'стратегическая специализация')) OR
    (mr.name = 'Полковник' AND rat.name IN ('уровень командования', 'опыт командования частью', 'количество лет в офицерском составе', 'профиль командования')) OR
    (mr.name ILIKE '%подполковник%' AND rat.name IN ('уровень командования', 'опыт штабной работы', 'профиль подготовки', 'количество подчинённых подразделений')) OR
    (mr.name ILIKE '%майор%' AND rat.name IN ('уровень командования', 'направление службы', 'опыт оперативного планирования', 'уровень допуска')) OR
    (mr.name ILIKE '%капитан%' AND rat.name IN ('уровень командования', 'командирский стаж', 'профиль роты', 'уровень подготовки')) OR
    (mr.name ILIKE '%лейтенант%' AND rat.name IN ('уровень командования', 'военное училище', 'год выпуска', 'первичная специальность')) OR
    (mr.name ILIKE '%прапорщик%' AND rat.name IN ('техническая специализация', 'категория допуска', 'стаж службы')) OR
    (mr.name ILIKE '%старшина%' AND rat.name IN ('опыт работы с личным составом', 'направление подготовки', 'уровень дисциплинарной ответственности')) OR
    (mr.name ILIKE '%сержант%' AND rat.name IN ('командирская подготовка', 'специализация отделения', 'категория инструктора')) OR
    (mr.name ILIKE '%ефрейтор%' AND rat.name IN ('основная специальность', 'уровень подготовки', 'допуск к технике')) OR
    (mr.name ILIKE '%рядовой%' AND rat.name IN ('базовая специальность', 'уровень подготовки', 'дата зачисления в подразделение'))
ON CONFLICT (rank_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required;

INSERT INTO rank_attribute_values (personnel_id, attribute_id, value_text, value_number, value_date, value_boolean)
SELECT p.personnel_id,
       rat.attribute_id,
       CASE rat.name
           WHEN 'уровень командования' THEN NULL
           WHEN 'количество лет в офицерском составе' THEN NULL
           WHEN 'количество подчинённых подразделений' THEN NULL
           WHEN 'командирский стаж' THEN NULL
           WHEN 'год выпуска' THEN NULL
           WHEN 'стаж службы' THEN NULL
           WHEN 'дата окончания академии' THEN NULL
           WHEN 'дата присвоения генеральского звания' THEN NULL
           WHEN 'дата зачисления в подразделение' THEN NULL
           WHEN 'стратегическая специализация' THEN 'оперативно-стратегическое управление войсками'
           WHEN 'опыт командования частью' THEN 'командование военной частью постоянной готовности'
           WHEN 'профиль командования' THEN 'мотострелковые и общевойсковые подразделения'
           WHEN 'опыт штабной работы' THEN 'планирование боевого применения подразделений'
           WHEN 'профиль подготовки' THEN 'оперативная и мобилизационная подготовка'
           WHEN 'направление службы' THEN 'оперативное управление'
           WHEN 'опыт оперативного планирования' THEN 'планирование учений и развертывания'
           WHEN 'уровень допуска' THEN 'служебный'
           WHEN 'профиль роты' THEN 'мотострелковый'
           WHEN 'уровень подготовки' THEN 'подготовлен'
           WHEN 'военное училище' THEN 'Новосибирское высшее военное командное училище'
           WHEN 'первичная специальность' THEN 'командир мотострелкового взвода'
           WHEN 'техническая специализация' THEN 'эксплуатация вооружения и техники'
           WHEN 'категория допуска' THEN mr.category
           WHEN 'опыт работы с личным составом' THEN 'организация службы подразделения'
           WHEN 'направление подготовки' THEN 'строевая и тактическая подготовка'
           WHEN 'уровень дисциплинарной ответственности' THEN 'старший подразделения'
           WHEN 'командирская подготовка' THEN 'командир отделения'
           WHEN 'специализация отделения' THEN 'мотострелковое отделение'
           WHEN 'категория инструктора' THEN 'инструктор начального уровня'
           WHEN 'основная специальность' THEN 'стрелок'
           WHEN 'допуск к технике' THEN 'эксплуатация штатной техники'
           WHEN 'базовая специальность' THEN 'стрелок мотострелкового отделения'
           ELSE NULL
       END,
       CASE rat.name
           WHEN 'уровень командования' THEN tdc_rank_level(mr.name, mr.category)::NUMERIC
           WHEN 'количество лет в офицерском составе' THEN 12 + (p.personnel_id % 8)
           WHEN 'количество подчинённых подразделений' THEN 3 + (p.personnel_id % 5)
           WHEN 'командирский стаж' THEN 2 + (p.personnel_id % 6)
           WHEN 'год выпуска' THEN 2014 + (p.personnel_id % 8)
           WHEN 'стаж службы' THEN 5 + (p.personnel_id % 12)
           ELSE NULL
       END,
       CASE rat.name
           WHEN 'дата окончания академии' THEN DATE '2010-06-20' + ((p.personnel_id % 1800)::INT)
           WHEN 'дата присвоения генеральского звания' THEN DATE '2016-02-15' + ((p.personnel_id % 1200)::INT)
           WHEN 'дата зачисления в подразделение' THEN p.service_start
           ELSE NULL
       END,
       NULL
FROM personnel p
JOIN personnel_ranks pr ON pr.personnel_id = p.personnel_id
JOIN military_ranks mr ON mr.rank_id = pr.rank_id
JOIN rank_type_attributes rta ON rta.rank_id = mr.rank_id
JOIN rank_attribute_types rat ON rat.attribute_id = rta.attribute_id
ON CONFLICT (personnel_id, attribute_id) DO UPDATE SET
    value_text = EXCLUDED.value_text,
    value_number = EXCLUDED.value_number,
    value_date = EXCLUDED.value_date,
    value_boolean = EXCLUDED.value_boolean;
