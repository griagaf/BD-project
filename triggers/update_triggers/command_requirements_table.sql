CREATE TABLE command_requirements (
    entity_scope TEXT NOT NULL,
    type_name TEXT NOT NULL,
    min_rank_level INT NOT NULL,
    max_rank_level INT,
    PRIMARY KEY (entity_scope, type_name)
);

INSERT INTO command_requirements VALUES
-- Формирования
('formation', 'Округ', 95, NULL),
('formation', 'Армия', 90, NULL),
('formation', 'Корпус', 80, NULL),
('formation', 'Дивизия', 70, NULL),
('formation', 'Бригада', 60, NULL),

('unit', 'Воинская часть', 50, NULL),

-- Подразделения
('subdivision', 'Батальон', 60, NULL),
('subdivision', 'Рота', 50, NULL),
('subdivision', 'Взвод', 10, 40),    
('subdivision', 'Отделение', 10, 40);
