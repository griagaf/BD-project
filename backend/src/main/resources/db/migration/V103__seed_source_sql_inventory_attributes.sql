UPDATE equipment_types et
SET purpose = COALESCE(et.purpose, CASE
        WHEN lower(et.name) LIKE '%т-72%' THEN 'основной боевой танк для усиления мотострелковых подразделений'
        WHEN lower(et.name) LIKE '%бмп%' OR lower(et.name) LIKE '%bmp%' THEN 'боевая машина пехоты для перевозки отделения и огневой поддержки'
        WHEN lower(et.name) LIKE '%бтр%' OR lower(et.name) LIKE '%btr%' THEN 'бронетранспортёр для перевозки личного состава'
        WHEN lower(et.name) LIKE '%радиостанц%' OR lower(et.name) LIKE '%radio%' THEN 'средство тактической радиосвязи'
        WHEN lower(et.name) LIKE '%бпла%' OR lower(et.name) LIKE '%орлан%' THEN 'беспилотный комплекс разведки и наблюдения'
        WHEN lower(et.name) LIKE '%кухн%' THEN 'полевая техника обеспечения питания'
        WHEN lower(et.name) LIKE '%топлив%' THEN 'техника подвоза и выдачи топлива'
        WHEN lower(et.name) LIKE '%имр%' OR lower(et.name) LIKE '%бат-2%' THEN 'инженерная техника обеспечения движения'
        ELSE 'техника обеспечения выполнения задач подразделения'
    END),
    crew_size = COALESCE(et.crew_size, CASE
        WHEN lower(et.name) LIKE '%радиостанц%' THEN 2
        WHEN lower(et.name) LIKE '%бпла%' OR lower(et.name) LIKE '%орлан%' THEN 2
        WHEN lower(et.name) LIKE '%кухн%' THEN 3
        ELSE 3
    END),
    weight_tons = COALESCE(et.weight_tons, CASE
        WHEN lower(et.name) LIKE '%т-72%' THEN 46.00
        WHEN lower(et.name) LIKE '%бмп%' OR lower(et.name) LIKE '%bmp%' THEN 14.30
        WHEN lower(et.name) LIKE '%бтр%' OR lower(et.name) LIKE '%btr%' THEN 15.40
        WHEN lower(et.name) LIKE '%имр%' THEN 44.50
        WHEN lower(et.name) LIKE '%бат-2%' THEN 39.70
        WHEN lower(et.name) LIKE '%урал%' THEN 8.40
        WHEN lower(et.name) LIKE '%камаз%' THEN 9.10
        ELSE 5.00
    END),
    max_speed_kmh = COALESCE(et.max_speed_kmh, CASE
        WHEN lower(et.name) LIKE '%бтр%' OR lower(et.name) LIKE '%btr%' THEN 80
        WHEN lower(et.name) LIKE '%урал%' OR lower(et.name) LIKE '%камаз%' THEN 85
        WHEN lower(et.name) LIKE '%т-72%' THEN 60
        ELSE 65
    END),
    operational_range_km = COALESCE(et.operational_range_km, CASE
        WHEN lower(et.name) LIKE '%урал%' OR lower(et.name) LIKE '%камаз%' THEN 1000
        WHEN lower(et.name) LIKE '%бтр%' OR lower(et.name) LIKE '%btr%' THEN 600
        WHEN lower(et.name) LIKE '%т-72%' THEN 500
        ELSE 550
    END),
    adoption_year = COALESCE(et.adoption_year, CASE
        WHEN lower(et.name) LIKE '%т-72%' THEN 1973
        WHEN lower(et.name) LIKE '%бмп%' OR lower(et.name) LIKE '%bmp%' THEN 1980
        WHEN lower(et.name) LIKE '%бтр%' OR lower(et.name) LIKE '%btr%' THEN 2013
        WHEN lower(et.name) LIKE '%орлан%' THEN 2010
        ELSE 1995
    END),
    manufacturer = COALESCE(et.manufacturer, CASE
        WHEN lower(et.name) LIKE '%т-72%' THEN 'Уралвагонзавод'
        WHEN lower(et.name) LIKE '%бмп%' OR lower(et.name) LIKE '%bmp%' THEN 'Курганмашзавод'
        WHEN lower(et.name) LIKE '%бтр%' OR lower(et.name) LIKE '%btr%' THEN 'Арзамасский машиностроительный завод'
        WHEN lower(et.name) LIKE '%урал%' THEN 'Уральский автомобильный завод'
        WHEN lower(et.name) LIKE '%камаз%' THEN 'Камский автомобильный завод'
        ELSE 'Предприятие оборонно-промышленного комплекса'
    END),
    description = COALESCE(et.description, 'Паспортные характеристики внесены согласно атрибутной модели БД.')
WHERE et.archived = FALSE;

UPDATE weapon_types wt
SET purpose = COALESCE(wt.purpose, CASE
        WHEN lower(wt.name) LIKE '%ак%' OR lower(wt.name) LIKE '%ak%' THEN 'индивидуальное стрелковое вооружение'
        WHEN lower(wt.name) LIKE '%пкм%' OR lower(wt.name) LIKE '%pkm%' THEN 'единый пулемёт огневой поддержки'
        WHEN lower(wt.name) LIKE '%свд%' THEN 'снайперская винтовка отделения'
        WHEN lower(wt.name) LIKE '%рпг%' OR lower(wt.name) LIKE '%rpg%' THEN 'ручной противотанковый гранатомёт'
        WHEN lower(wt.name) LIKE '%корнет%' OR lower(wt.name) LIKE '%метис%' THEN 'противотанковый ракетный комплекс'
        WHEN lower(wt.name) LIKE '%игла%' THEN 'переносной зенитный ракетный комплекс'
        WHEN lower(wt.name) LIKE '%2б14%' OR lower(wt.name) LIKE '%podnos%' THEN 'миномёт ротного звена'
        WHEN lower(wt.name) LIKE '%мста%' THEN 'буксируемая артиллерийская система'
        ELSE 'вооружение подразделения'
    END),
    caliber = COALESCE(wt.caliber, CASE
        WHEN lower(wt.name) LIKE '%ак%' OR lower(wt.name) LIKE '%ak%' THEN '5,45 мм'
        WHEN lower(wt.name) LIKE '%пкм%' OR lower(wt.name) LIKE '%pkm%' THEN '7,62 мм'
        WHEN lower(wt.name) LIKE '%свд%' THEN '7,62 мм'
        WHEN lower(wt.name) LIKE '%рпг%' OR lower(wt.name) LIKE '%rpg%' THEN '40 мм'
        WHEN lower(wt.name) LIKE '%2б14%' OR lower(wt.name) LIKE '%podnos%' THEN '82 мм'
        WHEN lower(wt.name) LIKE '%мста%' THEN '152 мм'
        ELSE 'специальный'
    END),
    effective_range_m = COALESCE(wt.effective_range_m, CASE
        WHEN lower(wt.name) LIKE '%ак%' OR lower(wt.name) LIKE '%ak%' THEN 500
        WHEN lower(wt.name) LIKE '%пкм%' OR lower(wt.name) LIKE '%pkm%' THEN 1000
        WHEN lower(wt.name) LIKE '%свд%' THEN 1200
        WHEN lower(wt.name) LIKE '%рпг%' OR lower(wt.name) LIKE '%rpg%' THEN 500
        WHEN lower(wt.name) LIKE '%корнет%' THEN 5500
        WHEN lower(wt.name) LIKE '%метис%' THEN 2000
        WHEN lower(wt.name) LIKE '%игла%' THEN 5200
        WHEN lower(wt.name) LIKE '%мста%' THEN 24700
        ELSE 1500
    END),
    adoption_year = COALESCE(wt.adoption_year, CASE
        WHEN lower(wt.name) LIKE '%ак%' OR lower(wt.name) LIKE '%ak%' THEN 1991
        WHEN lower(wt.name) LIKE '%пкм%' OR lower(wt.name) LIKE '%pkm%' THEN 1969
        WHEN lower(wt.name) LIKE '%корнет%' THEN 1998
        WHEN lower(wt.name) LIKE '%игла%' THEN 1983
        ELSE 1985
    END),
    manufacturer = COALESCE(wt.manufacturer, CASE
        WHEN lower(wt.name) LIKE '%ак%' OR lower(wt.name) LIKE '%пкм%' OR lower(wt.name) LIKE '%ak%' OR lower(wt.name) LIKE '%pkm%' THEN 'Концерн Калашников'
        WHEN lower(wt.name) LIKE '%корнет%' OR lower(wt.name) LIKE '%метис%' THEN 'Конструкторское бюро приборостроения'
        ELSE 'Предприятие оборонно-промышленного комплекса'
    END),
    description = COALESCE(wt.description, 'Паспортные характеристики внесены согласно атрибутной модели БД.')
WHERE wt.archived = FALSE;

INSERT INTO equipment_type_attribute_values (type_id, attribute_id, value_text, value_number)
SELECT et.type_id,
       eat.attribute_id,
       CASE eat.name
           WHEN 'назначение' THEN et.purpose
           WHEN 'производитель' THEN et.manufacturer
           WHEN 'описание' THEN et.description
           ELSE NULL
       END,
       CASE eat.name
           WHEN 'экипаж' THEN et.crew_size::NUMERIC
           WHEN 'масса, т' THEN et.weight_tons
           WHEN 'скорость, км/ч' THEN et.max_speed_kmh::NUMERIC
           WHEN 'запас хода, км' THEN et.operational_range_km::NUMERIC
           WHEN 'год принятия' THEN et.adoption_year::NUMERIC
           ELSE NULL
       END
FROM equipment_types et
JOIN equipment_attribute_types eat ON eat.name IN (
    'назначение', 'экипаж', 'масса, т', 'скорость, км/ч', 'запас хода, км', 'год принятия', 'производитель', 'описание'
)
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
           WHEN 'производитель' THEN wt.manufacturer
           WHEN 'описание' THEN wt.description
           ELSE NULL
       END,
       CASE wat.name
           WHEN 'дальность, м' THEN wt.effective_range_m::NUMERIC
           WHEN 'год принятия' THEN wt.adoption_year::NUMERIC
           ELSE NULL
       END
FROM weapon_types wt
JOIN weapon_attribute_types wat ON wat.name IN (
    'назначение', 'калибр', 'дальность, м', 'год принятия', 'производитель', 'описание'
)
WHERE wt.archived = FALSE
ON CONFLICT (type_id, attribute_id) DO UPDATE SET
    value_text = EXCLUDED.value_text,
    value_number = EXCLUDED.value_number,
    value_date = NULL,
    value_boolean = NULL;
