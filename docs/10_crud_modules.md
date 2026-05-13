# TACTICAL DISTRICT COMMAND: CRUD modules

## Назначение

Документ описывает CRUD-модули для основных сущностей **TACTICAL DISTRICT COMMAND**:

- военнослужащие;
- части;
- подразделения;
- техника;
- вооружение;
- сооружения;
- специальности;
- пользователи;
- назначения командиров.

CRUD-интерфейсы работают с учетом ролей и command scope. Frontend скрывает недоступные кнопки, backend выполняет обязательные permission checks.

## Общие правила CRUD

```text
Read list/detail -> role permission + scope filter
Create -> role permission + parent object in scope
Update -> role permission + target object in scope
Delete -> role permission + target object in scope + DB constraints
Export -> role permission + scoped dataset
```

Data access:

```text
Regular CRUD -> Spring Data JPA
Scoped lists and complex joins -> NamedParameterJdbcTemplate
Validation -> Jakarta Validation + database constraints + triggers
Audit -> audit_events
```

## Role behavior

| Role | CRUD behavior |
|---|---|
| `ADMIN_DISTRICT` | полный CRUD по всем сущностям |
| `STAFF_ANALYST` | read-only, export scoped/all data |
| `ARMY_COMMANDER` | read/update внутри назначенной армии |
| `FORMATION_COMMANDER` | read/update внутри назначенного формирования |
| `UNIT_COMMANDER` | read/update своей части, подразделений, личного состава, техники, вооружения и сооружений |
| `COMPANY_COMMANDER` | read/update личного состава и структуры внутри своей роты |
| `PLATOON_COMMANDER` | read/update личного состава внутри своего взвода |
| `SQUAD_COMMANDER` | read/update ограниченных данных своего отделения |
| `SOLDIER` | read только собственной карточки |

## 1. CRUD endpoints

### Personnel

```http
GET    /api/personnel
GET    /api/personnel/{id}
POST   /api/personnel
PUT    /api/personnel/{id}
DELETE /api/personnel/{id}
GET    /api/personnel/export.csv
```

Related:

```http
GET    /api/personnel/{id}/specialties
POST   /api/personnel/{id}/specialties/{specialtyId}
DELETE /api/personnel/{id}/specialties/{specialtyId}
PUT    /api/personnel/{id}/rank
```

List filters:

```http
GET /api/personnel?search=иванов&unitId=12&subdivisionId=44&rank=Капитан&specialty=Связист&page=0&size=20
```

### Military units

```http
GET    /api/units
GET    /api/units/{id}
POST   /api/units
PUT    /api/units/{id}
DELETE /api/units/{id}
GET    /api/units/export.csv
```

Related:

```http
GET /api/units/{id}/passport
GET /api/units/{id}/personnel
GET /api/units/{id}/equipment
GET /api/units/{id}/weapons
GET /api/units/{id}/buildings
```

### Subdivisions

```http
GET    /api/subdivisions
GET    /api/subdivisions/{id}
POST   /api/subdivisions
PUT    /api/subdivisions/{id}
DELETE /api/subdivisions/{id}
GET    /api/subdivisions/export.csv
```

Filters:

```http
GET /api/subdivisions?unitId=12&type=Рота&parentId=40
```

### Equipment

Equipment directories:

```http
GET    /api/equipment/categories
POST   /api/equipment/categories
PUT    /api/equipment/categories/{id}
DELETE /api/equipment/categories/{id}

GET    /api/equipment/types
POST   /api/equipment/types
PUT    /api/equipment/types/{id}
DELETE /api/equipment/types/{id}
```

Equipment in units:

```http
GET    /api/units/{unitId}/equipment
PUT    /api/units/{unitId}/equipment/{typeId}
DELETE /api/units/{unitId}/equipment/{typeId}
GET    /api/equipment/export.csv
```

### Weapons

Weapon directories:

```http
GET    /api/weapons/categories
POST   /api/weapons/categories
PUT    /api/weapons/categories/{id}
DELETE /api/weapons/categories/{id}

GET    /api/weapons/types
POST   /api/weapons/types
PUT    /api/weapons/types/{id}
DELETE /api/weapons/types/{id}
```

Weapons in units:

```http
GET    /api/units/{unitId}/weapons
PUT    /api/units/{unitId}/weapons/{typeId}
DELETE /api/units/{unitId}/weapons/{typeId}
GET    /api/weapons/export.csv
```

### Buildings

```http
GET    /api/buildings
GET    /api/buildings/{id}
POST   /api/buildings
PUT    /api/buildings/{id}
DELETE /api/buildings/{id}
GET    /api/buildings/export.csv
```

Related:

```http
GET    /api/buildings/{id}/subdivisions
POST   /api/buildings/{id}/subdivisions/{subdivisionId}
DELETE /api/buildings/{id}/subdivisions/{subdivisionId}
```

### Specialties

```http
GET    /api/specialties
GET    /api/specialties/{id}
POST   /api/specialties
PUT    /api/specialties/{id}
DELETE /api/specialties/{id}
GET    /api/specialties/export.csv
```

### Users

```http
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
POST   /api/users/{id}/activate
POST   /api/users/{id}/deactivate
```

Roles:

```http
GET    /api/roles
POST   /api/users/{id}/roles/{roleCode}
DELETE /api/users/{id}/roles/{roleCode}
```

Command assignments:

```http
GET    /api/users/{id}/command-assignments
POST   /api/users/{id}/command-assignments
PUT    /api/users/{id}/command-assignments/{assignmentId}
DELETE /api/users/{id}/command-assignments/{assignmentId}
```

### Commander assignments

Commander assignment endpoints wrap updates of `commander_id` in `military_formations`, `military_units` and `subdivisions`.

```http
PUT /api/formations/{formationId}/commander
PUT /api/units/{unitId}/commander
PUT /api/subdivisions/{subdivisionId}/commander
DELETE /api/formations/{formationId}/commander
DELETE /api/units/{unitId}/commander
DELETE /api/subdivisions/{subdivisionId}/commander
```

## 2. Frontend forms

### PersonnelForm

Fields:

```text
lastName
firstName
middleName
personalNumber
birthDate
serviceStart
subdivisionId
rankId
specialtyIds
```

UI:

- text inputs for name fields;
- date pickers for birth and service dates;
- combobox for subdivision;
- select for rank;
- multi-select for specialties.

### UnitForm

Fields:

```text
name
formationId
locationId
commanderId
```

UI:

- text input for name;
- scoped formation combobox;
- location selector;
- commander selector filtered by eligible personnel.

### SubdivisionForm

Fields:

```text
name
type
unitId
parentId
commanderId
```

UI:

- type select: Батальон, Рота, Взвод, Отделение;
- scoped unit combobox;
- parent subdivision combobox;
- commander selector.

### UnitEquipmentForm

Fields:

```text
unitId
typeId
quantity
```

UI:

- equipment type combobox;
- quantity number input;
- category displayed as read-only metadata.

### UnitWeaponForm

Fields:

```text
unitId
typeId
quantity
```

UI:

- weapon type combobox;
- quantity number input;
- category displayed as read-only metadata.

### BuildingForm

Fields:

```text
name
unitId
subdivisionIds
```

UI:

- building name input;
- scoped unit selector;
- multi-select subdivisions from the same unit.

### SpecialtyForm

Fields:

```text
name
```

UI:

- single text input;
- duplicate validation.

### UserForm

Fields:

```text
username
displayName
personnelId
password
roles
isActive
```

UI:

- username input;
- display name input;
- personnel combobox;
- password input for create/reset;
- role multi-select;
- active switch.

### CommandAssignmentForm

Fields:

```text
soldierId
objectType
objectId
startsAt
endsAt
isPrimary
```

UI:

- soldier selector;
- object type select;
- object combobox dependent on object type;
- date fields;
- primary checkbox.

### CommanderAssignmentForm

Fields:

```text
targetType
targetId
commanderId
```

UI:

- selected object displayed read-only;
- commander combobox;
- current commander preview;
- validation summary returned by backend triggers.

## 3. Validation rules

### Personnel

Frontend Zod:

```ts
export const personnelSchema = z.object({
  lastName: z.string().min(1).max(100),
  firstName: z.string().min(1).max(100),
  middleName: z.string().max(100).optional(),
  personalNumber: z.string().min(1).max(20),
  birthDate: z.string().min(1),
  serviceStart: z.string().min(1),
  subdivisionId: z.number().positive(),
})
```

Backend:

- `last_name` and `first_name` are required;
- `personal_number` is unique;
- `birth_date <= CURRENT_DATE - INTERVAL '18 years'`;
- `service_start <= CURRENT_DATE`;
- `subdivision_id` must exist;
- personnel must be inside user's scope for update/delete.

### Units

Rules:

- `name` is required and unique;
- `formation_id` is required;
- `location_id` must exist when provided;
- `commander_id` must reference existing personnel when provided;
- commander assignment must pass DB commander triggers.

### Subdivisions

Rules:

- `name` is required;
- `type` is one of `Батальон`, `Рота`, `Взвод`, `Отделение`;
- `(name, unit_id)` is unique;
- `parent_id` cannot equal self;
- hierarchy must pass `validate_subdivision_hierarchy`;
- subdivision and parent must belong to the same unit.

### Equipment / Weapons

Rules:

- type must exist;
- unit must exist;
- quantity is required;
- quantity must be `>= 0`;
- `(unit_id, type_id)` is unique;
- update replaces quantity for the selected unit/type pair.

### Buildings

Rules:

- name is required;
- unit must exist;
- linked subdivisions must belong to the same unit;
- validation uses `validate_building` and `building_unit_consistency`.

### Specialties

Rules:

- name is required;
- name is unique;
- delete is blocked when specialty is assigned to personnel.

### Users

Rules:

- username is required and unique;
- password is required on create;
- password is BCrypt-hashed on backend;
- `personnelId` must exist when provided;
- user cannot remove the last `ADMIN_DISTRICT` role.

### Command assignments

Rules:

- `soldier_id` is required;
- `object_type` must be one of allowed values;
- `object_id` must exist in the table matching `object_type`;
- `ends_at >= starts_at`;
- only one primary assignment per soldier.

### Commander assignments

Rules:

- commander must exist in `personnel`;
- commander must satisfy command requirements;
- object must be inside user's scope;
- DB triggers validate formation/unit/subdivision commander rules.

## 4. PermissionGuard for buttons

Button permissions:

```text
create -> entity:create
edit -> entity:update
delete -> entity:delete
assign commander -> commander:assign
export -> entity:export
```

Examples:

```tsx
<PermissionGuard permissions={["personnel:create"]}>
  <Button>
    <Plus className="size-4" />
    Create
  </Button>
</PermissionGuard>
```

```tsx
<PermissionGuard permissions={["personnel:update"]}>
  <DropdownMenuItem>Edit</DropdownMenuItem>
</PermissionGuard>
```

```tsx
<PermissionGuard permissions={["commander:assign"]}>
  <Button variant="secondary">
    <ShieldCheck className="size-4" />
    Assign commander
  </Button>
</PermissionGuard>
```

```tsx
<PermissionGuard permissions={["personnel:export"]}>
  <Button variant="outline">
    <Download className="size-4" />
    Export CSV
  </Button>
</PermissionGuard>
```

Permission matrix:

| Entity | Create | Edit | Delete | Assign commander | Export |
|---|---|---|---|---|---|
| Personnel | `personnel:create` | `personnel:update` | `personnel:delete` | - | `personnel:export` |
| Units | `unit:create` | `unit:update` | `unit:delete` | `commander:assign` | `unit:export` |
| Subdivisions | `subdivision:create` | `subdivision:update` | `subdivision:delete` | `commander:assign` | `subdivision:export` |
| Equipment | `equipment:create` | `equipment:update` | `equipment:delete` | - | `equipment:export` |
| Weapons | `weapon:create` | `weapon:update` | `weapon:delete` | - | `weapon:export` |
| Buildings | `building:create` | `building:update` | `building:delete` | - | `building:export` |
| Specialties | `specialty:create` | `specialty:update` | `specialty:delete` | - | `specialty:export` |
| Users | `user:create` | `user:update` | `user:delete` | - | `user:export` |

## 5. Backend permission checks

Backend checks are performed in service layer and optionally with `@PreAuthorize`.

### Read list

```java
@Transactional(readOnly = true)
public Page<PersonnelResponse> search(PersonnelFilter filter, Pageable pageable) {
    UserContext user = userContextProvider.current();

    if (!permissionService.canReadCollection(user, ObjectType.PERSONNEL)) {
        throw new AccessDeniedException("Access denied");
    }

    return personnelRepository.searchInScope(user, filter, pageable)
            .map(personnelMapper::toResponse);
}
```

### Read detail

```java
@PreAuthorize("hasPermission(#id, 'PERSONNEL', 'READ')")
public PersonnelResponse findById(Long id) {
    return personnelRepository.findById(id)
            .map(personnelMapper::toResponse)
            .orElseThrow(() -> new EntityNotFoundException("Personnel not found"));
}
```

### Create

```java
public PersonnelResponse create(CreatePersonnelRequest request) {
    UserContext user = userContextProvider.current();
    ObjectType parentType = hierarchyService.resolveSubdivisionObjectType(request.subdivisionId());

    if (!permissionService.canCreate(user, parentType, request.subdivisionId(), ObjectType.PERSONNEL)) {
        throw new AccessDeniedException("Access denied");
    }

    PersonnelEntity saved = personnelRepository.save(personnelMapper.toEntity(request));
    auditService.created(user, ObjectType.PERSONNEL, saved.getId());

    return personnelMapper.toResponse(saved);
}
```

### Update

```java
public PersonnelResponse update(Long id, UpdatePersonnelRequest request) {
    UserContext user = userContextProvider.current();

    if (!permissionService.canUpdate(user, ObjectType.PERSONNEL, id)) {
        throw new AccessDeniedException("Access denied");
    }

    PersonnelEntity entity = personnelRepository.getReferenceById(id);
    personnelMapper.updateEntity(request, entity);
    auditService.updated(user, ObjectType.PERSONNEL, id);

    return personnelMapper.toResponse(entity);
}
```

### Delete

```java
public void delete(Long id) {
    UserContext user = userContextProvider.current();

    if (!permissionService.canDelete(user, ObjectType.PERSONNEL, id)) {
        throw new AccessDeniedException("Access denied");
    }

    personnelRepository.deleteById(id);
    auditService.deleted(user, ObjectType.PERSONNEL, id);
}
```

### Assign commander

```java
public UnitResponse assignUnitCommander(Long unitId, AssignCommanderRequest request) {
    UserContext user = userContextProvider.current();

    if (!permissionService.canAssignCommander(user, ObjectType.MILITARY_UNIT, unitId)) {
        throw new AccessDeniedException("Access denied");
    }

    MilitaryUnitEntity unit = unitRepository.getReferenceById(unitId);
    unit.setCommanderId(request.commanderId());

    auditService.commanderAssigned(user, ObjectType.MILITARY_UNIT, unitId, request.commanderId());

    return unitMapper.toResponse(unit);
}
```

## 6. Optimistic UI or refetch

Default strategy:

```text
Create -> mutation -> invalidate list
Update -> mutation -> invalidate detail and list
Delete -> mutation -> invalidate list
Assign commander -> mutation -> invalidate object passport, hierarchy tree, list
Export -> no cache invalidation
```

Use regular refetch for:

- personnel;
- units;
- subdivisions;
- equipment;
- weapons;
- buildings;
- users;
- commander assignment.

Use optimistic UI only for small low-risk toggles:

- activate/deactivate user;
- acknowledge alert;
- column visibility;
- local table selection.

Mutation example:

```ts
export function useUpdatePersonnelMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdatePersonnelRequest }) =>
      personnelApi.update(id, payload),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: personnelKeys.all })
      queryClient.invalidateQueries({ queryKey: personnelKeys.detail(variables.id) })
    },
  })
}
```

## 7. Error handling

Backend returns unified error response:

```json
{
  "timestamp": "2026-05-13T11:20:00Z",
  "status": 409,
  "code": "DATA_INTEGRITY_ERROR",
  "message": "Data integrity constraint violation",
  "path": "/api/units/12/commander",
  "validationErrors": {}
}
```

Frontend behavior:

| Status | UI behavior |
|---|---|
| `400` | show field validation errors |
| `401` | clear session and redirect to login |
| `403` | show access denied toast and disable action |
| `404` | show not found state |
| `409` | show conflict message from DB constraint/trigger |
| `500` | show generic error panel |

Form validation errors:

```ts
if (error.status === 400 && error.validationErrors) {
  Object.entries(error.validationErrors).forEach(([field, message]) => {
    form.setError(field as keyof FormValues, { message })
  })
}
```

Database trigger errors:

```text
commander does not satisfy requirements
subdivision hierarchy is invalid
building and subdivision belong to different units
unit with resources cannot be deleted
```

These errors are returned as `409 DATA_INTEGRITY_ERROR` with readable messages.

## 8. Audit log

Every mutating CRUD action creates an audit event.

Actions:

```text
CREATE
UPDATE
DELETE
SOFT_DELETE
RESTORE
ASSIGN_COMMANDER
UNASSIGN_COMMANDER
ASSIGN_ROLE
REMOVE_ROLE
ASSIGN_SCOPE
REMOVE_SCOPE
EXPORT
```

Audit event:

```java
public record AuditEventDto(
        Long id,
        Long userId,
        String username,
        String action,
        String objectType,
        Long objectId,
        String status,
        String message,
        Instant createdAt
) {
}
```

Example:

```json
{
  "id": 501,
  "userId": 5,
  "username": "unit.cmd.1",
  "action": "UPDATE",
  "objectType": "PERSONNEL",
  "objectId": 44,
  "status": "SUCCESS",
  "message": "Personnel record updated",
  "createdAt": "2026-05-13T11:40:00Z"
}
```

Audit endpoints:

```http
GET /api/audit/events
GET /api/audit/events/{id}
```

Filters:

```http
GET /api/audit/events?action=UPDATE&objectType=PERSONNEL&userId=5&page=0&size=20
```

## 9. Soft delete

Soft delete is useful for application-owned security and operational records:

- users;
- alerts;
- generated reports;
- audit-visible configuration records.

Hard delete is acceptable for simple dictionary records only when not referenced:

- unused specialties;
- unused equipment types;
- unused weapon types.

For core military structure, prefer restricted delete:

- personnel;
- units;
- subdivisions;
- buildings;
- equipment assignments;
- weapon assignments.

Reason:

- existing DB uses foreign keys and delete triggers;
- deleting command-linked or resource-linked objects can break historical consistency;
- PostgreSQL triggers already protect commanders, ranks, units with resources and command requirements.

### Users soft delete

Use `is_active = false`.

```http
POST /api/users/{id}/deactivate
POST /api/users/{id}/activate
```

No physical deletion for users with audit history.

### Alerts soft lifecycle

Alert lifecycle:

```text
OPEN -> ACKNOWLEDGED -> RESOLVED
```

Fields:

```text
status
acknowledged_at
acknowledged_by
resolved_at
resolved_by
```

### Reports soft lifecycle

Generated reports can have:

```text
deleted_at
deleted_by
```

## Frontend CRUD page pattern

```text
PageHeader
  title
  scope badge
  create button
  export button

FilterBar
  search
  domain filters
  reset

DataTable
  sortable columns
  pagination
  row actions

Dialog/Drawer
  create/edit form
  validation
  submit state

ConfirmDeleteDialog
  destructive confirmation
```

Row actions:

```tsx
function PersonnelRowActions({ personnel }: { personnel: Personnel }) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon">
          <MoreHorizontal className="size-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem>Open</DropdownMenuItem>

        <PermissionGuard permissions={["personnel:update"]}>
          <DropdownMenuItem>Edit</DropdownMenuItem>
        </PermissionGuard>

        <PermissionGuard permissions={["personnel:delete"]}>
          <DropdownMenuItem className="text-red-400">Delete</DropdownMenuItem>
        </PermissionGuard>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
```

## CRUD modules summary

| Module | Main UI | Backend source |
|---|---|---|
| Personnel | table + profile drawer + form | `personnel`, `v_personnel_full` |
| Units | table + unit passport | `military_units` |
| Subdivisions | tree/list + form | `subdivisions` |
| Equipment | unit resource panel | `equipment_in_units`, `v_unit_equipment` |
| Weapons | unit resource panel | `weapon_in_units`, `v_unit_weapons` |
| Buildings | building registry + usage panel | `buildings`, `v_buildings_usage` |
| Specialties | dictionary table | `specialties` |
| Users | admin table + role editor | `users`, `roles`, `user_roles` |
| Commander assignments | commander selector | `commander_id` fields + triggers |

