# TACTICAL DISTRICT COMMAND: Tactical Dashboard, Alert Center, Readiness Radar

## Назначение

Документ описывает модули:

- **Tactical Dashboard**;
- **Alert Center**;
- **Readiness Radar**.

Эти модули используют существующие данные БД, представления из `selects/` и scope текущего пользователя. Backend возвращает только агрегаты и alert'ы, доступные пользователю по роли и `command_assignments`.

## Общая схема данных

```text
Dashboard
  -> summary counters
  -> readiness summary
  -> problem zones
  -> latest events

Alert Center
  -> generated alerts
  -> alert actions
  -> linked query templates

Readiness Radar
  -> personnel score
  -> equipment score
  -> weapons score
  -> specialists score
  -> infrastructure score
```

Источники:

| Data | Source |
|---|---|
| структура | `military_formations`, `military_units`, `subdivisions`, `v_formation_closure` |
| личный состав | `personnel`, `v_personnel_full` |
| техника | `equipment_in_units`, `v_unit_equipment` |
| вооружение | `weapon_in_units`, `v_unit_weapons` |
| сооружения | `buildings`, `subdivision_buildings`, `v_buildings_usage` |
| специальности | `specialties`, `personnel_specialties`, `v_personnel_specialties` |
| аудит | `audit_events` |

## 1. Tactical Dashboard

Tactical Dashboard является главной operational summary страницей.

### Dashboard blocks

```text
Command Summary
  total formations
  total units
  total subdivisions
  total personnel

Resource Summary
  equipment types
  equipment quantity
  weapon types
  weapon quantity
  buildings count

Problem Zones
  units without equipment
  units without weapons
  buildings without subdivisions
  overloaded buildings
  specialties without specialists

Readiness Summary
  overall score
  personnel score
  equipment score
  weapons score
  specialists score
  infrastructure score

Latest Events
  CRUD audit events
  query executions
  alert acknowledgements
```

### Counters

Counters are scoped.

Example:

```json
{
  "formations": 8,
  "units": 14,
  "subdivisions": 92,
  "personnel": 1280,
  "equipmentQuantity": 420,
  "weaponQuantity": 3100,
  "buildings": 64,
  "openAlerts": 9
}
```

### Problem zones

Problem zones group active alerts by category:

```json
[
  {
    "type": "UNIT_WITHOUT_EQUIPMENT",
    "label": "Units without equipment",
    "severity": "HIGH",
    "count": 3
  },
  {
    "type": "SPECIALTY_WITHOUT_SPECIALISTS",
    "label": "Specialties without specialists",
    "severity": "MEDIUM",
    "count": 5
  }
]
```

### Latest events

Events use `audit_events`.

```json
[
  {
    "id": 1001,
    "eventType": "UPDATE",
    "objectType": "PERSONNEL",
    "objectId": 44,
    "actor": "unit.cmd.1",
    "message": "Personnel record updated",
    "createdAt": "2026-05-13T11:00:00Z"
  }
]
```

## 2. Alert Center

Alert Center displays generated data-quality and readiness alerts.

Alerts can be persisted in `alerts` or generated dynamically. The API shape is the same in both cases.

### Alert types

```java
public enum AlertType {
    UNIT_WITHOUT_EQUIPMENT,
    UNIT_WITHOUT_WEAPONS,
    BUILDING_WITHOUT_SUBDIVISIONS,
    BUILDING_OVERLOADED,
    SPECIALTY_WITHOUT_SPECIALISTS,
    EQUIPMENT_QUANTITY_EXCEEDED,
    WEAPON_QUANTITY_EXCEEDED
}
```

### Alert severity

```java
public enum AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

Severity mapping:

| Alert type | Severity |
|---|---|
| `UNIT_WITHOUT_EQUIPMENT` | `HIGH` |
| `UNIT_WITHOUT_WEAPONS` | `HIGH` |
| `BUILDING_WITHOUT_SUBDIVISIONS` | `MEDIUM` |
| `BUILDING_OVERLOADED` | `MEDIUM` |
| `SPECIALTY_WITHOUT_SPECIALISTS` | `MEDIUM` |
| `EQUIPMENT_QUANTITY_EXCEEDED` | `CRITICAL` |
| `WEAPON_QUANTITY_EXCEEDED` | `CRITICAL` |

### Alert generation rules

#### Units without equipment

```sql
SELECT mu.unit_id, mu.name AS unit_name
FROM military_units mu
WHERE NOT EXISTS (
    SELECT 1
    FROM equipment_in_units eiu
    WHERE eiu.unit_id = mu.unit_id
);
```

#### Units without weapons

```sql
SELECT mu.unit_id, mu.name AS unit_name
FROM military_units mu
WHERE NOT EXISTS (
    SELECT 1
    FROM weapon_in_units wiu
    WHERE wiu.unit_id = mu.unit_id
);
```

#### Buildings without subdivisions

Uses `v_buildings_usage`:

```sql
SELECT building_id, building_name, unit_name
FROM v_buildings_usage
WHERE subdivisions_count = 0;
```

#### Overloaded buildings

Simple threshold:

```sql
SELECT building_id, building_name, unit_name, subdivisions_count
FROM v_buildings_usage
WHERE subdivisions_count > :maxSubdivisionsPerBuilding;
```

Default threshold:

```text
maxSubdivisionsPerBuilding = 3
```

#### Specialties without specialists

```sql
SELECT s.specialty_id, s.name AS specialty_name
FROM specialties s
WHERE NOT EXISTS (
    SELECT 1
    FROM personnel_specialties ps
    WHERE ps.specialty_id = s.specialty_id
);
```

#### Equipment quantity exceeded

Uses configurable threshold by equipment type or category.

```sql
SELECT
    vue.unit_id,
    vue.unit_name,
    vue.equipment_type,
    vue.quantity
FROM v_unit_equipment vue
WHERE vue.quantity > :maxEquipmentQuantity;
```

Default threshold:

```text
maxEquipmentQuantity = 100
```

#### Weapon quantity exceeded

```sql
SELECT
    vuw.unit_id,
    vuw.unit_name,
    vuw.weapon_type,
    vuw.quantity
FROM v_unit_weapons vuw
WHERE vuw.quantity > :maxWeaponQuantity;
```

Default threshold:

```text
maxWeaponQuantity = 500
```

## 3. Alert to Action

Each alert contains actions. Actions are rendered as buttons in the frontend and map to routes or query terminal presets.

### Alert actions

```java
public enum AlertActionType {
    OPEN_OBJECT,
    SHOW_RELATED_DATA,
    GENERATE_REPORT,
    OPEN_QUERY_TERMINAL
}
```

Example:

```json
{
  "id": "UNIT_WITHOUT_EQUIPMENT:12",
  "type": "UNIT_WITHOUT_EQUIPMENT",
  "severity": "HIGH",
  "title": "Unit has no equipment",
  "message": "1-й танковый полк has no registered equipment.",
  "objectType": "MILITARY_UNIT",
  "objectId": 12,
  "actions": [
    {
      "type": "OPEN_OBJECT",
      "label": "Open unit",
      "route": "/units/12"
    },
    {
      "type": "SHOW_RELATED_DATA",
      "label": "Show equipment",
      "route": "/units/12?tab=equipment"
    },
    {
      "type": "GENERATE_REPORT",
      "label": "Generate resource report",
      "route": "/reports/equipment?unitId=12"
    },
    {
      "type": "OPEN_QUERY_TERMINAL",
      "label": "Open query",
      "route": "/intelligence",
      "queryPreset": {
        "templateCode": "FIND_EQUIPMENT_AVAILABILITY",
        "parameters": {
          "condition": "WITHOUT",
          "unitId": 12
        }
      }
    }
  ]
}
```

### Action mapping

| Alert type | Open object | Related data | Report | Query terminal |
|---|---|---|---|---|
| `UNIT_WITHOUT_EQUIPMENT` | unit passport | equipment tab | equipment report | `FIND_EQUIPMENT_AVAILABILITY` |
| `UNIT_WITHOUT_WEAPONS` | unit passport | weapons tab | weapons report | `FIND_WEAPON_AVAILABILITY` |
| `BUILDING_WITHOUT_SUBDIVISIONS` | building card | building usage | infrastructure report | `FIND_BUILDING_USAGE` |
| `BUILDING_OVERLOADED` | building card | assigned subdivisions | infrastructure report | `FIND_BUILDING_USAGE` |
| `SPECIALTY_WITHOUT_SPECIALISTS` | specialty card | specialists list | personnel report | `FIND_SPECIALTY_COVERAGE` |
| `EQUIPMENT_QUANTITY_EXCEEDED` | unit passport | equipment tab | equipment report | `FIND_UNIT_EQUIPMENT` |
| `WEAPON_QUANTITY_EXCEEDED` | unit passport | weapons tab | weapons report | `FIND_UNIT_WEAPONS` |

## 4. Readiness Radar

Readiness Radar is a five-axis score:

```text
personnel
equipment
weapons
specialists
infrastructure
```

Each score is `0..100`.

Overall readiness:

```text
overall = average(personnel, equipment, weapons, specialists, infrastructure)
```

### Personnel score

Simple formula:

```text
personnelScore = 100 - unitsWithoutPersonnelRatio * 100
```

Using available data:

```text
personnelScore = min(100, personnelCount / max(1, subdivisionsCount) * 10)
```

Interpretation:

- more personnel distributed across subdivisions increases score;
- empty scope returns `0`;
- score is capped at `100`.

### Equipment score

```text
equipmentScore = 100 - unitsWithoutEquipmentRatio * 100 - exceededEquipmentPenalty
```

Where:

```text
unitsWithoutEquipmentRatio = unitsWithoutEquipment / totalUnits
exceededEquipmentPenalty = min(30, equipmentExceededAlerts * 5)
```

### Weapons score

```text
weaponsScore = 100 - unitsWithoutWeaponsRatio * 100 - exceededWeaponsPenalty
```

Where:

```text
unitsWithoutWeaponsRatio = unitsWithoutWeapons / totalUnits
exceededWeaponsPenalty = min(30, weaponExceededAlerts * 5)
```

### Specialists score

```text
specialistsScore = 100 - missingSpecialtiesRatio * 100
```

Where:

```text
missingSpecialtiesRatio = specialtiesWithoutSpecialists / totalSpecialties
```

### Infrastructure score

```text
infrastructureScore = 100 - unusedBuildingPenalty - overloadedBuildingPenalty
```

Where:

```text
unusedBuildingPenalty = min(40, buildingsWithoutSubdivisions * 5)
overloadedBuildingPenalty = min(40, overloadedBuildings * 5)
```

### Score clamping

```java
private int clampScore(double score) {
    return (int) Math.max(0, Math.min(100, Math.round(score)));
}
```

## 5. Backend services

Package structure:

```text
dashboard/
  controller/
    DashboardController.java
  dto/
    DashboardSummaryDto.java
    DashboardCountersDto.java
    ProblemZoneDto.java
    LatestEventDto.java
  service/
    DashboardService.java
    DashboardAggregationService.java
  repository/
    DashboardRepository.java

alert/
  controller/
    AlertController.java
  dto/
    AlertDto.java
    AlertActionDto.java
    AlertSummaryDto.java
  service/
    AlertService.java
    AlertGenerationService.java
    AlertActionFactory.java
  repository/
    AlertRepository.java

readiness/
  controller/
    ReadinessController.java
  dto/
    ReadinessRadarDto.java
    ReadinessAxisDto.java
    ReadinessSummaryDto.java
  service/
    ReadinessService.java
    ReadinessCalculationService.java
  repository/
    ReadinessRepository.java
```

### DashboardService

```java
public interface DashboardService {

    DashboardSummaryDto summary(DashboardScopeRequest scope);

    List<ProblemZoneDto> problemZones(DashboardScopeRequest scope);

    List<LatestEventDto> latestEvents(DashboardScopeRequest scope, int limit);
}
```

### AlertService

```java
public interface AlertService {

    Page<AlertDto> search(AlertFilter filter, Pageable pageable);

    AlertDto findById(String alertId);

    void acknowledge(String alertId);

    void resolve(String alertId);

    AlertSummaryDto summary();
}
```

### AlertGenerationService

```java
public interface AlertGenerationService {

    List<AlertDto> generateAlerts(UserContext user, AlertFilter filter);
}
```

### ReadinessService

```java
public interface ReadinessService {

    ReadinessRadarDto radar(DashboardScopeRequest scope);

    ReadinessSummaryDto summary(DashboardScopeRequest scope);
}
```

### ReadinessCalculationService

```java
@Service
public class ReadinessCalculationService {

    public ReadinessRadarDto calculate(ReadinessRawMetrics metrics) {
        int personnel = personnelScore(metrics);
        int equipment = equipmentScore(metrics);
        int weapons = weaponsScore(metrics);
        int specialists = specialistsScore(metrics);
        int infrastructure = infrastructureScore(metrics);

        int overall = clampScore((personnel + equipment + weapons + specialists + infrastructure) / 5.0);

        return new ReadinessRadarDto(
                overall,
                List.of(
                        new ReadinessAxisDto("personnel", "Personnel", personnel),
                        new ReadinessAxisDto("equipment", "Equipment", equipment),
                        new ReadinessAxisDto("weapons", "Weapons", weapons),
                        new ReadinessAxisDto("specialists", "Specialists", specialists),
                        new ReadinessAxisDto("infrastructure", "Infrastructure", infrastructure)
                )
        );
    }
}
```

## 6. DTO

### Dashboard DTO

```java
public record DashboardSummaryDto(
        DashboardCountersDto counters,
        ReadinessRadarDto readiness,
        List<ProblemZoneDto> problemZones,
        List<LatestEventDto> latestEvents
) {
}
```

```java
public record DashboardCountersDto(
        long formations,
        long units,
        long subdivisions,
        long personnel,
        long equipmentQuantity,
        long weaponQuantity,
        long buildings,
        long openAlerts
) {
}
```

```java
public record ProblemZoneDto(
        String type,
        String label,
        AlertSeverity severity,
        long count,
        String route
) {
}
```

```java
public record LatestEventDto(
        Long id,
        String eventType,
        String objectType,
        Long objectId,
        String actor,
        String message,
        Instant createdAt
) {
}
```

### Alert DTO

```java
public record AlertDto(
        String id,
        AlertType type,
        AlertSeverity severity,
        String title,
        String message,
        String objectType,
        Long objectId,
        String status,
        Instant createdAt,
        List<AlertActionDto> actions
) {
}
```

```java
public record AlertActionDto(
        AlertActionType type,
        String label,
        String route,
        Map<String, Object> queryPreset
) {
}
```

```java
public record AlertSummaryDto(
        long total,
        long critical,
        long high,
        long medium,
        long low,
        long acknowledged,
        long resolved
) {
}
```

### Readiness DTO

```java
public record ReadinessRadarDto(
        int overall,
        List<ReadinessAxisDto> axes
) {
}
```

```java
public record ReadinessAxisDto(
        String key,
        String label,
        int score
) {
}
```

```java
public record ReadinessSummaryDto(
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

### Scope request

```java
public record DashboardScopeRequest(
        String scopeType,
        Long scopeId
) {
}
```

### Alert filter

```java
public record AlertFilter(
        AlertSeverity severity,
        AlertType type,
        String status,
        String objectType,
        Long objectId
) {
}
```

## 7. Endpoints

### Dashboard

```http
GET /api/dashboard/summary
GET /api/dashboard/counters
GET /api/dashboard/problem-zones
GET /api/dashboard/latest-events?limit=10
```

Scoped variants:

```http
GET /api/dashboard/summary?scopeType=FORMATION&scopeId=1
GET /api/dashboard/summary?scopeType=MILITARY_UNIT&scopeId=12
```

### Alerts

```http
GET  /api/alerts
GET  /api/alerts/summary
GET  /api/alerts/{id}
POST /api/alerts/{id}/acknowledge
POST /api/alerts/{id}/resolve
```

Filters:

```http
GET /api/alerts?severity=HIGH&type=UNIT_WITHOUT_EQUIPMENT&status=OPEN&page=0&size=20
```

### Readiness

```http
GET /api/readiness/radar
GET /api/readiness/summary
```

Scoped variants:

```http
GET /api/readiness/radar?scopeType=FORMATION&scopeId=1
GET /api/readiness/radar?scopeType=MILITARY_UNIT&scopeId=12
```

## 8. Scope handling

All dashboard, alert and readiness endpoints use the same scope flow:

```text
1. Read current user from UserContext.
2. Resolve requested scope.
3. Check requested scope with AccessControlService.
4. Build allowed formation/unit/subdivision ids.
5. Apply ids to aggregation SQL.
6. Return scoped counters, alerts and readiness.
```

Rules:

| User role | Dashboard data |
|---|---|
| `ADMIN_DISTRICT` | full district |
| `STAFF_ANALYST` | full district read-only |
| `ARMY_COMMANDER` | assigned army |
| `FORMATION_COMMANDER` | assigned formation |
| `UNIT_COMMANDER` | assigned unit |
| `COMPANY_COMMANDER` | assigned company branch |
| `PLATOON_COMMANDER` | assigned platoon branch |
| `SQUAD_COMMANDER` | assigned squad |
| `SOLDIER` | personal readiness context |

Example repository method:

```java
public DashboardCountersDto counters(UserContext user, DashboardScopeRequest scope) {
    ScopeIds ids = accessScopeResolver.resolve(user, scope);
    return dashboardRepository.counters(ids);
}
```

SQL uses allowed ids:

```sql
SELECT COUNT(*) AS units_count
FROM military_units mu
WHERE mu.unit_id = ANY(:allowedUnitIds);
```

For formation scopes:

```sql
SELECT COUNT(DISTINCT mu.unit_id)
FROM military_units mu
JOIN v_formation_closure fc
    ON fc.descendant_formation_id = mu.formation_id
WHERE fc.root_formation_id = ANY(:allowedFormationIds);
```

## 9. Frontend components

### Dashboard components

```text
TacticalDashboardPage
DashboardHeader
CommandScopeBadge
CommandMetricsGrid
MetricCounterCard
ReadinessSummaryCard
ProblemZonesPanel
LatestEventsFeed
DashboardRefreshButton
```

### Alert components

```text
AlertCenterPage
AlertSeverityTabs
AlertFilterBar
AlertList
AlertCard
AlertDetailsDrawer
AlertActionButtons
AlertSummaryStrip
```

### Readiness components

```text
ReadinessRadarCard
ReadinessRadarChart
ReadinessAxisList
ReadinessScoreBadge
ReadinessTrendPill
```

### Shared components

```text
SeverityBadge
StatusBadge
MetricPill
EmptyState
LoadingSkeleton
ErrorPanel
```

## 10. Frontend data hooks

```ts
export function useDashboardSummaryQuery(scope?: DashboardScope) {
  return useQuery({
    queryKey: ["dashboard", "summary", scope],
    queryFn: () => dashboardApi.summary(scope),
    refetchInterval: 60_000,
  })
}
```

```ts
export function useAlertsQuery(filter: AlertFilter) {
  return useQuery({
    queryKey: ["alerts", filter],
    queryFn: () => alertApi.search(filter),
    placeholderData: keepPreviousData,
  })
}
```

```ts
export function useReadinessRadarQuery(scope?: DashboardScope) {
  return useQuery({
    queryKey: ["readiness", "radar", scope],
    queryFn: () => readinessApi.radar(scope),
  })
}
```

Alert action:

```ts
export function useAcknowledgeAlertMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: alertApi.acknowledge,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["alerts"] })
      queryClient.invalidateQueries({ queryKey: ["dashboard"] })
      queryClient.invalidateQueries({ queryKey: ["readiness"] })
    },
  })
}
```

## 11. Visual style

### Tactical Dashboard

Style:

- dark zinc background;
- compact metric cards;
- readiness score as large numeric value;
- restrained emerald/cyan accents;
- red/amber markers only for problems;
- charts on neutral surfaces;
- dense layout optimized for scanning.

Metric card:

```text
┌────────────────────┐
│ Units              │
│ 14                 │
│ +2 active alerts   │
└────────────────────┘
```

### Alert Center

Alert card visual:

```text
┌───────────────────────────────────────────────┐
│ HIGH  Unit has no equipment                   │
│ 1-й танковый полк has no registered equipment │
│ [Open unit] [Show equipment] [Report] [Query] │
└───────────────────────────────────────────────┘
```

Severity colors:

| Severity | Color |
|---|---|
| `LOW` | zinc / muted |
| `MEDIUM` | cyan |
| `HIGH` | amber |
| `CRITICAL` | red |

### Readiness Radar

Radar chart uses Recharts:

```tsx
<RadarChart data={data.axes}>
  <PolarGrid stroke="#3f3f46" />
  <PolarAngleAxis dataKey="label" stroke="#a1a1aa" />
  <PolarRadiusAxis domain={[0, 100]} stroke="#52525b" />
  <Radar
    dataKey="score"
    stroke="#34d399"
    fill="#34d399"
    fillOpacity={0.22}
  />
</RadarChart>
```

Layout:

```text
┌─────────────────────────────┐
│ Overall readiness  82       │
│                             │
│        Radar chart          │
│                             │
│ Personnel       91          │
│ Equipment       76          │
│ Weapons         80          │
│ Specialists     72          │
│ Infrastructure  89          │
└─────────────────────────────┘
```

## 12. Page composition

### Dashboard page

```tsx
export function DashboardPage() {
  const { data, isLoading } = useDashboardSummaryQuery()

  if (isLoading) {
    return <DashboardSkeleton />
  }

  return (
    <div className="space-y-6">
      <DashboardHeader />
      <CommandMetricsGrid counters={data.counters} />
      <div className="grid gap-6 xl:grid-cols-[420px_1fr]">
        <ReadinessRadarCard readiness={data.readiness} />
        <ProblemZonesPanel zones={data.problemZones} />
      </div>
      <LatestEventsFeed events={data.latestEvents} />
    </div>
  )
}
```

### Alert card

```tsx
export function AlertCard({ alert }: { alert: Alert }) {
  return (
    <article className="rounded-md border border-zinc-800 bg-zinc-950 p-4">
      <div className="flex items-start justify-between gap-4">
        <div>
          <SeverityBadge severity={alert.severity} />
          <h3 className="mt-2 text-sm font-semibold text-zinc-100">{alert.title}</h3>
          <p className="mt-1 text-sm text-zinc-400">{alert.message}</p>
        </div>
        <StatusBadge status={alert.status} />
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {alert.actions.map((action) => (
          <AlertActionButton key={action.label} action={action} />
        ))}
      </div>
    </article>
  )
}
```

### Alert action button

```tsx
export function AlertActionButton({ action }: { action: AlertAction }) {
  const navigate = useNavigate()
  const setPreset = useIntelligencePresetStore((state) => state.setPreset)

  function handleClick() {
    if (action.type === "OPEN_QUERY_TERMINAL" && action.queryPreset) {
      setPreset(action.queryPreset)
    }
    navigate(action.route)
  }

  return (
    <Button variant="secondary" size="sm" onClick={handleClick}>
      {action.label}
    </Button>
  )
}
```

## 13. Example API responses

### Dashboard summary

```json
{
  "counters": {
    "formations": 8,
    "units": 14,
    "subdivisions": 92,
    "personnel": 1280,
    "equipmentQuantity": 420,
    "weaponQuantity": 3100,
    "buildings": 64,
    "openAlerts": 9
  },
  "readiness": {
    "overall": 82,
    "axes": [
      { "key": "personnel", "label": "Personnel", "score": 91 },
      { "key": "equipment", "label": "Equipment", "score": 76 },
      { "key": "weapons", "label": "Weapons", "score": 80 },
      { "key": "specialists", "label": "Specialists", "score": 72 },
      { "key": "infrastructure", "label": "Infrastructure", "score": 89 }
    ]
  },
  "problemZones": [
    {
      "type": "UNIT_WITHOUT_EQUIPMENT",
      "label": "Units without equipment",
      "severity": "HIGH",
      "count": 3,
      "route": "/alerts?type=UNIT_WITHOUT_EQUIPMENT"
    }
  ],
  "latestEvents": []
}
```

### Alert

```json
{
  "id": "UNIT_WITHOUT_WEAPONS:12",
  "type": "UNIT_WITHOUT_WEAPONS",
  "severity": "HIGH",
  "title": "Unit has no weapons",
  "message": "1-й танковый полк has no registered weapons.",
  "objectType": "MILITARY_UNIT",
  "objectId": 12,
  "status": "OPEN",
  "createdAt": "2026-05-13T11:00:00Z",
  "actions": [
    {
      "type": "OPEN_OBJECT",
      "label": "Open unit",
      "route": "/units/12",
      "queryPreset": {}
    },
    {
      "type": "OPEN_QUERY_TERMINAL",
      "label": "Open query",
      "route": "/intelligence",
      "queryPreset": {
        "templateCode": "FIND_WEAPON_AVAILABILITY",
        "parameters": {
          "condition": "WITHOUT",
          "unitId": 12
        }
      }
    }
  ]
}
```

## Summary

Dashboard, Alert Center and Readiness Radar share one aggregation model:

```text
Scope -> Metrics -> Alerts -> Readiness -> Dashboard
```

The logic remains simple:

- count objects;
- detect missing or overloaded resources;
- assign severity;
- generate actions;
- compute readiness scores from ratios and penalties;
- return only data inside user scope.

