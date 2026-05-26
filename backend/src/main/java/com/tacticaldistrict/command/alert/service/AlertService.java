package com.tacticaldistrict.command.alert.service;

import com.tacticaldistrict.command.alert.application.port.AlertRepositoryPort;
import com.tacticaldistrict.command.alert.dto.AlertActionDto;
import com.tacticaldistrict.command.alert.dto.TacticalAlertDto;
import com.tacticaldistrict.command.security.access.PermissionService;
import com.tacticaldistrict.command.security.model.ObjectType;
import com.tacticaldistrict.command.user.service.UserContext;
import com.tacticaldistrict.command.user.service.UserContextProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int BUILDING_OVERLOAD_THRESHOLD = 3;
    private static final int EQUIPMENT_EXCEEDED_THRESHOLD = 300;
    private static final int WEAPON_EXCEEDED_THRESHOLD = 1000;

    private final AlertRepositoryPort alertRepository;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<TacticalAlertDto> alerts() {
        UserContext user = userContextProvider.current();
        if (!user.hasPermission("alert:read")) {
            throw new AccessDeniedException("Access denied");
        }

        List<TacticalAlertDto> result = new ArrayList<>();
        result.addAll(unitsWithoutEquipment(user));
        result.addAll(unitsWithoutWeapons(user));
        result.addAll(buildingsWithoutSubdivisions(user));
        result.addAll(overloadedBuildings(user));
        result.addAll(specialtiesWithoutSpecialists(user));
        result.addAll(equipmentQuantityExceeded(user));
        result.addAll(weaponQuantityExceeded(user));
        return result;
    }

    private List<TacticalAlertDto> unitsWithoutEquipment(UserContext user) {
        return alertRepository.unitsWithoutEquipment()
                .stream()
                .map(row -> alert(
                "UNIT_WITHOUT_EQUIPMENT",
                "HIGH",
                "В части отсутствует техника",
                row.unitName() + ": техника не зарегистрирована в инвентаре.",
                ObjectType.MILITARY_UNIT,
                row.unitId(),
                Map.of("unitName", row.unitName()),
                List.of(
                        new AlertActionDto("Открыть часть", "/units/" + row.unitId(), null),
                        new AlertActionDto("Добавить технику", "/equipment?unitId=" + row.unitId() + "&action=add", null),
                        new AlertActionDto("Показать технику", "/equipment?unitId=" + row.unitId(), "FIND_UNIT_EQUIPMENT"),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_EQUIPMENT_AVAILABILITY")
                )
        ))
                .filter(alert -> canRead(user, alert))
                .toList();
    }

    private List<TacticalAlertDto> unitsWithoutWeapons(UserContext user) {
        return alertRepository.unitsWithoutWeapons()
                .stream()
                .map(row -> alert(
                "UNIT_WITHOUT_WEAPONS",
                "HIGH",
                "В части отсутствует вооружение",
                row.unitName() + ": вооружение не зарегистрировано в инвентаре.",
                ObjectType.MILITARY_UNIT,
                row.unitId(),
                Map.of("unitName", row.unitName()),
                List.of(
                        new AlertActionDto("Открыть часть", "/units/" + row.unitId(), null),
                        new AlertActionDto("Добавить вооружение", "/weapons?unitId=" + row.unitId() + "&action=add", null),
                        new AlertActionDto("Показать вооружение", "/weapons?unitId=" + row.unitId(), "FIND_UNIT_WEAPONS"),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_WEAPON_AVAILABILITY")
                )
        ))
                .filter(alert -> canRead(user, alert))
                .toList();
    }

    private List<TacticalAlertDto> buildingsWithoutSubdivisions(UserContext user) {
        return alertRepository.buildingsWithoutSubdivisions()
                .stream()
                .map(row -> alert(
                "BUILDING_WITHOUT_SUBDIVISIONS",
                "MEDIUM",
                "Сооружение не используется",
                row.buildingName() + " не закреплено ни за одним подразделением.",
                ObjectType.BUILDING,
                row.buildingId(),
                Map.of("buildingName", row.buildingName(), "unitName", row.unitName()),
                List.of(
                        new AlertActionDto("Открыть сооружения", "/buildings", null),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_BUILDING_USAGE")
                )
        ))
                .filter(alert -> canRead(user, alert))
                .toList();
    }

    private List<TacticalAlertDto> overloadedBuildings(UserContext user) {
        return alertRepository.overloadedBuildings(BUILDING_OVERLOAD_THRESHOLD)
                .stream()
                .map(row -> alert(
                "BUILDING_OVERLOADED",
                "MEDIUM",
                "Перегрузка сооружения",
                row.buildingName() + " размещает подразделений: " + row.subdivisionsCount() + ".",
                ObjectType.BUILDING,
                row.buildingId(),
                Map.of(
                        "buildingName", row.buildingName(),
                        "unitName", row.unitName(),
                        "subdivisionsCount", row.subdivisionsCount()
                ),
                List.of(
                        new AlertActionDto("Открыть сооружения", "/buildings", null),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_BUILDING_USAGE")
                )
        ))
                .filter(alert -> canRead(user, alert))
                .toList();
    }

    private List<TacticalAlertDto> specialtiesWithoutSpecialists(UserContext user) {
        return alertRepository.specialtiesWithoutSpecialists()
                .stream()
                .map(row -> alert(
                "SPECIALTY_WITHOUT_SPECIALISTS",
                "MEDIUM",
                "Недостаток специалистов",
                "По специальности \"" + row.specialtyName() + "\" нет назначенных военнослужащих.",
                ObjectType.SPECIALTY,
                row.specialtyId(),
                Map.of("specialtyName", row.specialtyName()),
                List.of(new AlertActionDto("Открыть терминал", "/intelligence", "FIND_SPECIALTY_COVERAGE"))
        ))
                .filter(alert -> permissionService.canRead(user, ObjectType.SPECIALTY, alert.objectId()))
                .toList();
    }

    private List<TacticalAlertDto> equipmentQuantityExceeded(UserContext user) {
        return alertRepository.equipmentQuantityExceeded(EQUIPMENT_EXCEEDED_THRESHOLD)
                .stream()
                .map(row -> alert(
                "EQUIPMENT_QUANTITY_EXCEEDED",
                "CRITICAL",
                "Превышение количества техники",
                row.unitName() + ": зафиксировано повышенное количество техники \"" + row.resourceType() + "\".",
                ObjectType.MILITARY_UNIT,
                row.unitId(),
                Map.of("unitName", row.unitName(), "equipmentType", row.resourceType(), "quantity", row.quantity()),
                List.of(
                        new AlertActionDto("Показать технику", "/equipment?unitId=" + row.unitId(), "FIND_UNIT_EQUIPMENT"),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_EQUIPMENT_AVAILABILITY")
                )
        ))
                .filter(alert -> canRead(user, alert))
                .toList();
    }

    private List<TacticalAlertDto> weaponQuantityExceeded(UserContext user) {
        return alertRepository.weaponQuantityExceeded(WEAPON_EXCEEDED_THRESHOLD)
                .stream()
                .map(row -> alert(
                "WEAPON_QUANTITY_EXCEEDED",
                "CRITICAL",
                "Превышение количества вооружения",
                row.unitName() + ": зафиксировано повышенное количество вооружения \"" + row.resourceType() + "\".",
                ObjectType.MILITARY_UNIT,
                row.unitId(),
                Map.of("unitName", row.unitName(), "weaponType", row.resourceType(), "quantity", row.quantity()),
                List.of(
                        new AlertActionDto("Показать вооружение", "/weapons?unitId=" + row.unitId(), "FIND_UNIT_WEAPONS"),
                        new AlertActionDto("Открыть терминал", "/intelligence", "FIND_WEAPON_AVAILABILITY")
                )
        ))
                .filter(alert -> canRead(user, alert))
                .toList();
    }

    private TacticalAlertDto alert(
            String type,
            String severity,
            String title,
            String message,
            ObjectType objectType,
            Long objectId,
            Map<String, Object> details,
            List<AlertActionDto> actions
    ) {
        return new TacticalAlertDto(
                type + ":" + objectType.name() + ":" + objectId,
                type,
                severity,
                title,
                message,
                objectType.name(),
                objectId,
                new LinkedHashMap<>(details),
                actions
        );
    }

    private boolean canRead(UserContext user, TacticalAlertDto alert) {
        return permissionService.canRead(user, ObjectType.from(alert.objectType()), alert.objectId());
    }
}
