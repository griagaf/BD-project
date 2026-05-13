SELECT
    mu.unit_id,
    mu.name AS unit_name,
    mf.name AS parent_formation_name,
    mf.formation_type AS parent_formation_type,
    cp.last_name AS commander_last_name,
    cp.first_name AS commander_first_name,
    cp.middle_name AS commander_middle_name,
    mr.name AS commander_rank
FROM military_units mu
JOIN military_formations mf
    ON mf.formation_id = mu.formation_id
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = mu.formation_id
LEFT JOIN personnel cp
    ON cp.personnel_id = mu.commander_id
LEFT JOIN personnel_ranks pr
    ON pr.personnel_id = cp.personnel_id
LEFT JOIN military_ranks mr
    ON mr.rank_id = pr.rank_id
WHERE fc.root_formation_name = 'Западный военный округ'
ORDER BY mf.formation_type, mf.name, mu.name;
