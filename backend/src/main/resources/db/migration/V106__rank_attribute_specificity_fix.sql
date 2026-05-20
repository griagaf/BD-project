DELETE FROM rank_type_attributes;

INSERT INTO rank_type_attributes (rank_id, attribute_id, is_required)
SELECT mr.rank_id, rat.attribute_id, TRUE
FROM military_ranks mr
JOIN rank_attribute_types rat ON
    (mr.name ILIKE '%генерал%' AND rat.name IN ('уровень командования', 'дата окончания академии', 'дата присвоения генеральского звания', 'стратегическая специализация')) OR
    (mr.name = 'Полковник' AND rat.name IN ('уровень командования', 'опыт командования частью', 'количество лет в офицерском составе', 'профиль командования')) OR
    (mr.name ILIKE '%подполковник%' AND rat.name IN ('уровень командования', 'опыт штабной работы', 'профиль подготовки', 'количество подчинённых подразделений')) OR
    (mr.name NOT ILIKE '%генерал%' AND mr.name ILIKE '%майор%' AND rat.name IN ('уровень командования', 'направление службы', 'опыт оперативного планирования', 'уровень допуска')) OR
    (mr.name ILIKE '%капитан%' AND rat.name IN ('уровень командования', 'командирский стаж', 'профиль роты', 'уровень подготовки')) OR
    (mr.name NOT ILIKE '%генерал%' AND mr.name ILIKE '%лейтенант%' AND mr.name NOT ILIKE '%подполковник%' AND rat.name IN ('уровень командования', 'военное училище', 'год выпуска', 'первичная специальность')) OR
    (mr.name ILIKE '%прапорщик%' AND rat.name IN ('техническая специализация', 'категория допуска', 'стаж службы')) OR
    (mr.name ILIKE '%старшина%' AND rat.name IN ('опыт работы с личным составом', 'направление подготовки', 'уровень дисциплинарной ответственности')) OR
    (mr.name ILIKE '%сержант%' AND rat.name IN ('командирская подготовка', 'специализация отделения', 'категория инструктора')) OR
    (mr.name ILIKE '%ефрейтор%' AND rat.name IN ('основная специальность', 'уровень подготовки', 'допуск к технике')) OR
    (mr.name ILIKE '%рядовой%' AND rat.name IN ('базовая специальность', 'уровень подготовки', 'дата зачисления в подразделение'))
ON CONFLICT (rank_id, attribute_id) DO UPDATE SET is_required = EXCLUDED.is_required;
