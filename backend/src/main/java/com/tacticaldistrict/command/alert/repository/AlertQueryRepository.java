package com.tacticaldistrict.command.alert.repository;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AlertQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<UnitCandidate> unitsWithoutEquipment() {
        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name
                FROM military_units mu
                WHERE NOT EXISTS (
                    SELECT 1 FROM equipment_in_units eiu WHERE eiu.unit_id = mu.unit_id
                )
                ORDER BY mu.name
                """, Map.of(), (rs, rowNum) -> new UnitCandidate(rs.getLong("unit_id"), rs.getString("unit_name")));
    }

    public List<UnitCandidate> unitsWithoutWeapons() {
        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name
                FROM military_units mu
                WHERE NOT EXISTS (
                    SELECT 1 FROM weapon_in_units wiu WHERE wiu.unit_id = mu.unit_id
                )
                ORDER BY mu.name
                """, Map.of(), (rs, rowNum) -> new UnitCandidate(rs.getLong("unit_id"), rs.getString("unit_name")));
    }

    public List<BuildingCandidate> buildingsWithoutSubdivisions() {
        return jdbcTemplate.query("""
                SELECT b.building_id, b.name AS building_name, mu.name AS unit_name, COUNT(sb.subdivision_id) AS subdivisions_count
                FROM buildings b
                JOIN military_units mu ON mu.unit_id = b.unit_id
                LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
                WHERE b.assignable = TRUE
                GROUP BY b.building_id, b.name, mu.name
                HAVING COUNT(sb.subdivision_id) = 0
                ORDER BY mu.name, b.name
                """, Map.of(), (rs, rowNum) -> new BuildingCandidate(
                rs.getLong("building_id"),
                rs.getString("building_name"),
                rs.getString("unit_name"),
                rs.getLong("subdivisions_count")
        ));
    }

    public List<BuildingCandidate> overloadedBuildings(int threshold) {
        return jdbcTemplate.query("""
                SELECT b.building_id, b.name AS building_name, mu.name AS unit_name, COUNT(sb.subdivision_id) AS subdivisions_count
                FROM buildings b
                JOIN military_units mu ON mu.unit_id = b.unit_id
                LEFT JOIN subdivision_buildings sb ON sb.building_id = b.building_id
                WHERE b.assignable = TRUE
                GROUP BY b.building_id, b.name, mu.name
                HAVING COUNT(sb.subdivision_id) > :threshold
                ORDER BY subdivisions_count DESC, b.name
                """, Map.of("threshold", threshold), (rs, rowNum) -> new BuildingCandidate(
                rs.getLong("building_id"),
                rs.getString("building_name"),
                rs.getString("unit_name"),
                rs.getLong("subdivisions_count")
        ));
    }

    public List<SpecialtyCandidate> specialtiesWithoutSpecialists() {
        return jdbcTemplate.query("""
                SELECT s.specialty_id, s.name AS specialty_name
                FROM specialties s
                WHERE NOT EXISTS (
                    SELECT 1 FROM personnel_specialties ps WHERE ps.specialty_id = s.specialty_id
                )
                ORDER BY s.name
                """, Map.of(), (rs, rowNum) -> new SpecialtyCandidate(rs.getLong("specialty_id"), rs.getString("specialty_name")));
    }

    public List<InventoryCandidate> equipmentQuantityExceeded(int threshold) {
        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name, et.name AS resource_type, eiu.quantity
                FROM equipment_in_units eiu
                JOIN military_units mu ON mu.unit_id = eiu.unit_id
                JOIN equipment_types et ON et.type_id = eiu.type_id
                WHERE eiu.quantity > :threshold
                ORDER BY eiu.quantity DESC
                """, Map.of("threshold", threshold), (rs, rowNum) -> new InventoryCandidate(
                rs.getLong("unit_id"),
                rs.getString("unit_name"),
                rs.getString("resource_type"),
                rs.getInt("quantity")
        ));
    }

    public List<InventoryCandidate> weaponQuantityExceeded(int threshold) {
        return jdbcTemplate.query("""
                SELECT mu.unit_id, mu.name AS unit_name, wt.name AS resource_type, wiu.quantity
                FROM weapon_in_units wiu
                JOIN military_units mu ON mu.unit_id = wiu.unit_id
                JOIN weapon_types wt ON wt.type_id = wiu.type_id
                WHERE wiu.quantity > :threshold
                ORDER BY wiu.quantity DESC
                """, Map.of("threshold", threshold), (rs, rowNum) -> new InventoryCandidate(
                rs.getLong("unit_id"),
                rs.getString("unit_name"),
                rs.getString("resource_type"),
                rs.getInt("quantity")
        ));
    }

    public record UnitCandidate(Long unitId, String unitName) {
    }

    public record BuildingCandidate(Long buildingId, String buildingName, String unitName, Long subdivisionsCount) {
    }

    public record SpecialtyCandidate(Long specialtyId, String specialtyName) {
    }

    public record InventoryCandidate(Long unitId, String unitName, String resourceType, Integer quantity) {
    }
}
