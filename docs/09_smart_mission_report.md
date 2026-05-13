# TACTICAL DISTRICT COMMAND: Smart Mission Report

## Назначение

**Smart Mission Report** формирует тактический отчет по выбранному объекту структуры:

- армия;
- соединение;
- военная часть;
- рота;
- взвод.

Пользователь выбирает объект и нажимает **Generate Tactical Report**. Backend собирает связанные данные, рассчитывает readiness, добавляет alert'ы и формирует рекомендации на основе правил.

Настоящий AI не используется. Рекомендации создаются deterministic rule engine на основе метрик, alert'ов и readiness score.

## Report flow

```text
Frontend
  -> select object
  -> Generate Tactical Report
  -> POST /api/reports/smart-mission/generate

Backend
  -> check scope
  -> load object passport
  -> load commanders
  -> load personnel summary
  -> load equipment summary
  -> load weapons summary
  -> load buildings summary
  -> load specialties summary
  -> load alerts
  -> calculate readiness
  -> generate recommendations
  -> return report preview
```

## 1. Backend architecture

Package:

```text
report/
  controller/
    SmartMissionReportController.java
  dto/
    SmartMissionReportRequest.java
    SmartMissionReportDto.java
    ReportSectionDto.java
    ReportObjectDto.java
    ReportCommanderDto.java
    ReportPersonnelSummaryDto.java
    ReportEquipmentSummaryDto.java
    ReportWeaponSummaryDto.java
    ReportBuildingSummaryDto.java
    ReportSpecialtySummaryDto.java
    ReportAlertDto.java
    ReportReadinessDto.java
    ReportRecommendationDto.java
    ReportExportRequest.java
  model/
    ReportObjectType.java
    ReportRecommendationSeverity.java
    ReportExportFormat.java
  service/
    ReportService.java
    SmartMissionReportService.java
    ReportGenerator.java
    ReportRecommendationEngine.java
    ReportExportService.java
  repository/
    ReportDataRepository.java
    ReportQueryRepository.java
```

Dependencies:

```text
SmartMissionReportService
  -> AccessControlService
  -> FocusTreeService
  -> AlertService
  -> ReadinessService
  -> QueryExecutorService / ReportQueryRepository
  -> ReportGenerator
  -> ReportRecommendationEngine
  -> AuditService
```

Data sources:

| Report section | Source |
|---|---|
| object information | Focus Tree passport / `military_formations`, `military_units`, `subdivisions` |
| commanders | `commander_id`, `personnel`, `personnel_ranks` |
| personnel | `personnel`, `v_personnel_full` |
| equipment | `v_unit_equipment` |
| weapons | `v_unit_weapons` |
| buildings | `v_buildings_usage` |
| specialties | `v_personnel_specialties`, `specialties` |
| alerts | Alert Center generation |
| readiness | Readiness Radar service |
| recommendations | rule engine |

## 2. ReportService

`ReportService` is the public application service for report use cases.

```java
public interface ReportService {

    SmartMissionReportDto generateSmartMissionReport(SmartMissionReportRequest request);

    SmartMissionReportDto getReport(String reportId);

    byte[] exportCsv(String reportId);

    byte[] exportCsv(SmartMissionReportRequest request);

    byte[] exportPdf(String reportId);
}
```

Implementation:

```java
@Service
public class SmartMissionReportService implements ReportService {

    private final UserContextProvider userContextProvider;
    private final AccessControlService accessControlService;
    private final ReportGenerator reportGenerator;
    private final ReportExportService reportExportService;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public SmartMissionReportDto generateSmartMissionReport(SmartMissionReportRequest request) {
        UserContext user = userContextProvider.current();

        if (!accessControlService.isInScope(user, request.objectType(), request.objectId())) {
            throw new AccessDeniedException("Report object is outside command scope");
        }

        SmartMissionReportDto report = reportGenerator.generate(user, request);
        auditService.queryExecuted(user, "SMART_MISSION_REPORT:" + request.objectType() + ":" + request.objectId());

        return report;
    }

    @Override
    public byte[] exportCsv(SmartMissionReportRequest request) {
        SmartMissionReportDto report = generateSmartMissionReport(request);
        return reportExportService.toCsv(report);
    }
}
```

## 3. ReportGenerator

`ReportGenerator` aggregates all report sections.

```java
@Component
public class ReportGenerator {

    private final ReportDataRepository reportDataRepository;
    private final AlertService alertService;
    private final ReadinessService readinessService;
    private final ReportRecommendationEngine recommendationEngine;

    public SmartMissionReportDto generate(UserContext user, SmartMissionReportRequest request) {
        ReportObjectDto object = reportDataRepository.objectInfo(request.objectType(), request.objectId());
        List<ReportCommanderDto> commanders = reportDataRepository.commanders(request.objectType(), request.objectId());
        ReportPersonnelSummaryDto personnel = reportDataRepository.personnelSummary(request.objectType(), request.objectId());
        ReportEquipmentSummaryDto equipment = reportDataRepository.equipmentSummary(request.objectType(), request.objectId());
        ReportWeaponSummaryDto weapons = reportDataRepository.weaponSummary(request.objectType(), request.objectId());
        ReportBuildingSummaryDto buildings = reportDataRepository.buildingSummary(request.objectType(), request.objectId());
        ReportSpecialtySummaryDto specialties = reportDataRepository.specialtySummary(request.objectType(), request.objectId());
        List<ReportAlertDto> alerts = reportDataRepository.alerts(request.objectType(), request.objectId());
        ReportReadinessDto readiness = reportDataRepository.readiness(request.objectType(), request.objectId());

        List<ReportRecommendationDto> recommendations = recommendationEngine.generate(
                personnel,
                equipment,
                weapons,
                buildings,
                specialties,
                alerts,
                readiness
        );

        return new SmartMissionReportDto(
                UUID.randomUUID().toString(),
                object,
                Instant.now(),
                commanders,
                personnel,
                equipment,
                weapons,
                buildings,
                specialties,
                alerts,
                readiness,
                recommendations
        );
    }
}
```

## 4. DTO структуры отчета

### Request

```java
public record SmartMissionReportRequest(
        ReportObjectType objectType,
        Long objectId,
        boolean includePersonnel,
        boolean includeResources,
        boolean includeAlerts,
        boolean includeRecommendations
) {
}
```

```java
public enum ReportObjectType {
    ARMY,
    FORMATION,
    MILITARY_UNIT,
    COMPANY,
    PLATOON
}
```

### Main DTO

```java
public record SmartMissionReportDto(
        String reportId,
        ReportObjectDto object,
        Instant generatedAt,
        List<ReportCommanderDto> commanders,
        ReportPersonnelSummaryDto personnel,
        ReportEquipmentSummaryDto equipment,
        ReportWeaponSummaryDto weapons,
        ReportBuildingSummaryDto buildings,
        ReportSpecialtySummaryDto specialties,
        List<ReportAlertDto> alerts,
        ReportReadinessDto readiness,
        List<ReportRecommendationDto> recommendations
) {
}
```

### Object section

```java
public record ReportObjectDto(
        ReportObjectType type,
        Long id,
        String name,
        String parentName,
        String status,
        String location,
        List<String> path
) {
}
```

### Commanders section

```java
public record ReportCommanderDto(
        Long personnelId,
        String fullName,
        String rankName,
        String position,
        String objectName
) {
}
```

### Personnel section

```java
public record ReportPersonnelSummaryDto(
        long total,
        long officers,
        long enlisted,
        long commanders,
        Map<String, Long> byRank,
        Map<String, Long> bySubdivision
) {
}
```

### Equipment section

```java
public record ReportEquipmentSummaryDto(
        long totalQuantity,
        long typesCount,
        long unitsWithoutEquipment,
        List<ResourceQuantityDto> topEquipment,
        List<String> missingEquipmentWarnings
) {
}
```

### Weapons section

```java
public record ReportWeaponSummaryDto(
        long totalQuantity,
        long typesCount,
        long unitsWithoutWeapons,
        List<ResourceQuantityDto> topWeapons,
        List<String> missingWeaponWarnings
) {
}
```

```java
public record ResourceQuantityDto(
        String typeName,
        String categoryName,
        long quantity
) {
}
```

### Buildings section

```java
public record ReportBuildingSummaryDto(
        long total,
        long unused,
        long overloaded,
        List<BuildingUsageDto> problemBuildings
) {
}
```

```java
public record BuildingUsageDto(
        Long buildingId,
        String buildingName,
        String unitName,
        long subdivisionsCount
) {
}
```

### Specialties section

```java
public record ReportSpecialtySummaryDto(
        long totalSpecialties,
        long coveredSpecialties,
        long missingSpecialties,
        List<String> missingSpecialtyNames,
        Map<String, Long> topSpecialties
) {
}
```

### Alerts section

```java
public record ReportAlertDto(
        String alertId,
        String type,
        String severity,
        String title,
        String message,
        String objectType,
        Long objectId
) {
}
```

### Readiness section

```java
public record ReportReadinessDto(
        int overall,
        int personnel,
        int equipment,
        int weapons,
        int specialists,
        int infrastructure,
        String status
) {
}
```

### Recommendations

```java
public record ReportRecommendationDto(
        String code,
        ReportRecommendationSeverity severity,
        String title,
        String description,
        String actionLabel,
        String actionRoute
) {
}
```

```java
public enum ReportRecommendationSeverity {
    INFO,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

## Recommendation engine

Recommendations are generated with simple rules.

```java
@Component
public class ReportRecommendationEngine {

    public List<ReportRecommendationDto> generate(
            ReportPersonnelSummaryDto personnel,
            ReportEquipmentSummaryDto equipment,
            ReportWeaponSummaryDto weapons,
            ReportBuildingSummaryDto buildings,
            ReportSpecialtySummaryDto specialties,
            List<ReportAlertDto> alerts,
            ReportReadinessDto readiness
    ) {
        List<ReportRecommendationDto> result = new ArrayList<>();

        if (readiness.overall() < 60) {
            result.add(recommendation(
                    "LOW_OVERALL_READINESS",
                    ReportRecommendationSeverity.CRITICAL,
                    "Overall readiness is below operational threshold",
                    "Review resource gaps, alerts and personnel coverage before mission assignment.",
                    "Open alerts",
                    "/alerts"
            ));
        }

        if (equipment.unitsWithoutEquipment() > 0) {
            result.add(recommendation(
                    "UNITS_WITHOUT_EQUIPMENT",
                    ReportRecommendationSeverity.HIGH,
                    "Units without equipment detected",
                    "Assign equipment or verify equipment registry completeness.",
                    "Open equipment query",
                    "/intelligence?preset=FIND_EQUIPMENT_AVAILABILITY"
            ));
        }

        if (weapons.unitsWithoutWeapons() > 0) {
            result.add(recommendation(
                    "UNITS_WITHOUT_WEAPONS",
                    ReportRecommendationSeverity.HIGH,
                    "Units without weapons detected",
                    "Review weapon distribution for affected units.",
                    "Open weapons query",
                    "/intelligence?preset=FIND_WEAPON_AVAILABILITY"
            ));
        }

        if (specialties.missingSpecialties() > 0) {
            result.add(recommendation(
                    "MISSING_SPECIALISTS",
                    ReportRecommendationSeverity.MEDIUM,
                    "Specialty coverage is incomplete",
                    "Check missing specialties and personnel assignment.",
                    "Open specialty report",
                    "/reports/personnel"
            ));
        }

        if (buildings.overloaded() > 0) {
            result.add(recommendation(
                    "OVERLOADED_BUILDINGS",
                    ReportRecommendationSeverity.MEDIUM,
                    "Overloaded buildings detected",
                    "Review building assignment and redistribute subdivisions.",
                    "Open infrastructure report",
                    "/reports/buildings"
            ));
        }

        long criticalAlerts = alerts.stream()
                .filter(alert -> "CRITICAL".equals(alert.severity()))
                .count();

        if (criticalAlerts > 0) {
            result.add(recommendation(
                    "CRITICAL_ALERTS_PRESENT",
                    ReportRecommendationSeverity.CRITICAL,
                    "Critical alerts require attention",
                    "Resolve critical alerts before confirming operational readiness.",
                    "Open critical alerts",
                    "/alerts?severity=CRITICAL"
            ));
        }

        return result;
    }
}
```

## 5. REST endpoints

### Generate preview

```http
POST /api/reports/smart-mission/generate
```

Request:

```json
{
  "objectType": "MILITARY_UNIT",
  "objectId": 12,
  "includePersonnel": true,
  "includeResources": true,
  "includeAlerts": true,
  "includeRecommendations": true
}
```

Response:

```json
{
  "reportId": "4d4a9d87-9d55-43dd-b96e-7695526f0d19",
  "object": {
    "type": "MILITARY_UNIT",
    "id": 12,
    "name": "1-й танковый полк",
    "parentName": "7-я бригада",
    "status": "Активна",
    "location": "Москва, Военный городок 1",
    "path": [
      "Западный военный округ",
      "1-я армия",
      "7-я бригада",
      "1-й танковый полк"
    ]
  },
  "generatedAt": "2026-05-13T11:30:00Z",
  "readiness": {
    "overall": 78,
    "personnel": 90,
    "equipment": 72,
    "weapons": 80,
    "specialists": 65,
    "infrastructure": 84,
    "status": "OPERATIONAL_WITH_WARNINGS"
  },
  "recommendations": [
    {
      "code": "MISSING_SPECIALISTS",
      "severity": "MEDIUM",
      "title": "Specialty coverage is incomplete",
      "description": "Check missing specialties and personnel assignment.",
      "actionLabel": "Open specialty report",
      "actionRoute": "/reports/personnel"
    }
  ]
}
```

### Get generated report

```http
GET /api/reports/smart-mission/{reportId}
```

### Export CSV by report id

```http
GET /api/reports/smart-mission/{reportId}/export.csv
```

### Export CSV directly from request

```http
POST /api/reports/smart-mission/export.csv
```

### Export PDF

```http
GET /api/reports/smart-mission/{reportId}/export.pdf
POST /api/reports/smart-mission/export.pdf
```

PDF export uses the same report DTO and renders printable sections.

## 6. Export CSV

CSV export flattens the report into sections.

Example output:

```csv
Section,Key,Value
Object,Name,1-й танковый полк
Object,Type,MILITARY_UNIT
Readiness,Overall,78
Readiness,Personnel,90
Readiness,Equipment,72
Personnel,Total,240
Equipment,Total Quantity,54
Weapons,Total Quantity,380
Alerts,High,2
Recommendation,MISSING_SPECIALISTS,Specialty coverage is incomplete
```

Implementation:

```java
@Service
public class ReportExportService {

    public byte[] toCsv(SmartMissionReportDto report) {
        StringBuilder csv = new StringBuilder();
        csv.append("Section,Key,Value\n");

        append(csv, "Object", "Name", report.object().name());
        append(csv, "Object", "Type", report.object().type().name());
        append(csv, "Readiness", "Overall", report.readiness().overall());
        append(csv, "Readiness", "Personnel", report.readiness().personnel());
        append(csv, "Readiness", "Equipment", report.readiness().equipment());
        append(csv, "Personnel", "Total", report.personnel().total());
        append(csv, "Equipment", "Total Quantity", report.equipment().totalQuantity());
        append(csv, "Weapons", "Total Quantity", report.weapons().totalQuantity());

        for (ReportRecommendationDto recommendation : report.recommendations()) {
            append(csv, "Recommendation", recommendation.code(), recommendation.title());
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void append(StringBuilder csv, String section, String key, Object value) {
        csv.append(escape(section)).append(",")
                .append(escape(key)).append(",")
                .append(escape(value)).append("\n");
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }
}
```

## 7. Export PDF

PDF export uses the same DTO and does not change report logic.

Options:

```text
HTML template -> render to PDF
or
Java PDF library -> direct PDF generation
```

PDF structure:

```text
Cover
  report title
  object name
  generated at

Executive Summary
  readiness score
  alert count
  top recommendations

Sections
  commanders
  personnel
  equipment
  weapons
  buildings
  specialties
  alerts
  recommendations
```

Endpoint response:

```http
Content-Type: application/pdf
Content-Disposition: attachment; filename="smart-mission-report-12.pdf"
```

## 8. Frontend report page

Route:

```text
/reports/smart-mission
```

Page composition:

```text
SmartMissionReportPage
  ReportObjectSelector
  ReportOptionsPanel
  GenerateReportButton
  ReportPreview
  ReportExportActions
```

Object selector:

- type select: Army, Formation, Unit, Company, Platoon;
- scoped combobox for objects;
- search by name;
- current scope badge.

Report options:

```text
[x] Include personnel
[x] Include resources
[x] Include alerts
[x] Include recommendations
```

Generate action:

```tsx
export function GenerateReportButton() {
  const generateReport = useGenerateSmartMissionReportMutation()
  const request = useSmartReportStore((state) => state.request)

  return (
    <Button
      onClick={() => generateReport.mutate(request)}
      disabled={generateReport.isPending}
    >
      <FileText className="size-4" />
      Generate Tactical Report
    </Button>
  )
}
```

## 9. Report preview component

`ReportPreview` renders the report as a clean readable document inside the app.

```tsx
export function ReportPreview({ report }: { report: SmartMissionReport }) {
  return (
    <article className="space-y-6">
      <ReportHeader object={report.object} generatedAt={report.generatedAt} />
      <ReportReadinessSummary readiness={report.readiness} />
      <ReportSection title="Commanders">
        <CommandersList commanders={report.commanders} />
      </ReportSection>
      <ReportSection title="Personnel">
        <PersonnelSummary data={report.personnel} />
      </ReportSection>
      <ReportSection title="Resources">
        <ResourceSummary equipment={report.equipment} weapons={report.weapons} />
      </ReportSection>
      <ReportSection title="Alerts">
        <ReportAlerts alerts={report.alerts} />
      </ReportSection>
      <ReportSection title="Recommendations">
        <RecommendationList recommendations={report.recommendations} />
      </ReportSection>
    </article>
  )
}
```

Visual style:

- central document panel;
- compact tactical header;
- readiness score badge;
- section cards;
- severity-colored recommendation blocks;
- export buttons in sticky top actions.

## 10. Использование Query Terminal и Alerts

Smart Mission Report reuses Query Terminal and Alert Center data.

### Query Terminal integration

Report sections map to known query templates:

| Report section | Query template |
|---|---|
| units in formation | `FIND_UNITS_IN_FORMATION` |
| equipment | `FIND_UNIT_EQUIPMENT` |
| weapons | `FIND_UNIT_WEAPONS` |
| buildings | `FIND_BUILDING_USAGE` |
| specialty coverage | `FIND_SPECIALTY_COVERAGE` |
| specialists | `FIND_SPECIALISTS` |
| command chain | `FIND_PERSONNEL_COMMAND_CHAIN` |

Backend can use either:

```text
QueryExecutorService for reusable query execution
or
ReportDataRepository with dedicated SQL tuned for report aggregation
```

The report should store links to query presets:

```json
{
  "label": "Open equipment query",
  "route": "/intelligence",
  "queryPreset": {
    "templateCode": "FIND_UNIT_EQUIPMENT",
    "scope": {
      "type": "MILITARY_UNIT",
      "id": 12
    }
  }
}
```

### Alert Center integration

Report alerts use the same alert generation rules as Alert Center.

```java
AlertFilter filter = new AlertFilter(
        null,
        null,
        "OPEN",
        request.objectType().name(),
        request.objectId()
);

List<AlertDto> alerts = alertService.search(filter, Pageable.unpaged()).getContent();
```

Recommendations inspect alert types:

```text
UNIT_WITHOUT_EQUIPMENT -> recommend equipment registry review
UNIT_WITHOUT_WEAPONS -> recommend weapon distribution review
BUILDING_OVERLOADED -> recommend infrastructure redistribution
SPECIALTY_WITHOUT_SPECIALISTS -> recommend personnel specialization review
```

## 11. Roles and scope

Report generation follows the same authorization model:

```text
role permits report generation
scope permits selected object
sections return only scoped data
exports use the same checks as preview
```

Required permission:

```text
report:generate
```

Role access:

| Role | Report scope |
|---|---|
| `ADMIN_DISTRICT` | any object |
| `STAFF_ANALYST` | any object, read-only |
| `ARMY_COMMANDER` | assigned army |
| `FORMATION_COMMANDER` | assigned formation |
| `UNIT_COMMANDER` | assigned unit |
| `COMPANY_COMMANDER` | assigned company |
| `PLATOON_COMMANDER` | assigned platoon |
| `SQUAD_COMMANDER` | no mission report generation |
| `SOLDIER` | no mission report generation |

Backend checks:

```java
if (!permissionService.hasPermission(user, PermissionAction.GENERATE_REPORT, ObjectType.REPORT, request.objectId())) {
    throw new AccessDeniedException("Report generation is not allowed");
}

if (!accessControlService.isInScope(user, request.objectType(), request.objectId())) {
    throw new AccessDeniedException("Report object is outside command scope");
}
```

Frontend behavior:

- hides Generate button without `report:generate`;
- object selector only shows scoped objects;
- export buttons use the same permission guard;
- backend remains the authority for access checks.

## Frontend types

```ts
export type SmartMissionReportRequest = {
  objectType: "ARMY" | "FORMATION" | "MILITARY_UNIT" | "COMPANY" | "PLATOON"
  objectId: number
  includePersonnel: boolean
  includeResources: boolean
  includeAlerts: boolean
  includeRecommendations: boolean
}
```

```ts
export type SmartMissionReport = {
  reportId: string
  object: ReportObject
  generatedAt: string
  commanders: ReportCommander[]
  personnel: ReportPersonnelSummary
  equipment: ReportEquipmentSummary
  weapons: ReportWeaponSummary
  buildings: ReportBuildingSummary
  specialties: ReportSpecialtySummary
  alerts: ReportAlert[]
  readiness: ReportReadiness
  recommendations: ReportRecommendation[]
}
```

TanStack Query hooks:

```ts
export function useGenerateSmartMissionReportMutation() {
  return useMutation({
    mutationFn: reportApi.generateSmartMissionReport,
  })
}
```

```ts
export function useExportSmartMissionReportCsvMutation() {
  return useMutation({
    mutationFn: reportApi.exportSmartMissionReportCsv,
  })
}
```

## Summary

Smart Mission Report combines existing modules:

```text
Focus Tree object
  -> Query Terminal data
  -> Alert Center findings
  -> Readiness Radar score
  -> Rule-based recommendations
  -> Preview / CSV / PDF
```

The feature is visually strong and technically straightforward because all report sections are deterministic aggregations over existing database data.

