# TACTICAL DISTRICT COMMAND: backend application design

## Назначение

Документ описывает структуру backend-приложения **TACTICAL DISTRICT COMMAND** на Java 21+ и Spring Boot 4.x.

Backend предоставляет REST API для:

- аутентификации и управления пользователями;
- доступа к иерархии военного округа;
- управления личным составом;
- учета военных частей, техники, вооружения, сооружений и специальностей;
- выполнения аналитических SQL-запросов;
- построения dashboard, alert center и отчетов;
- аудита CRUD-операций.

Обычный CRUD выполняется через Spring Data JPA. Сложные SQL-запросы, аналитические выборки и query terminal выполняются через `NamedParameterJdbcTemplate`.

## 1. Структура packages

Базовый package:

```text
com.tacticaldistrict.command
```

Рекомендуемая структура:

```text
backend/src/main/java/com/tacticaldistrict/command/
  TacticalDistrictCommandApplication.java

  auth/
    controller/
    dto/
    entity/
    repository/
    service/

  user/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  personnel/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  hierarchy/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  unit/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  equipment/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  weapon/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  building/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  specialty/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  intelligence/
    controller/
    dto/
    repository/
    service/

  alert/
    controller/
    dto/
    entity/
    mapper/
    repository/
    service/

  report/
    controller/
    dto/
    repository/
    service/

  dashboard/
    controller/
    dto/
    repository/
    service/

  security/
    jwt/
    method/
    model/
    service/

  audit/
    entity/
    repository/
    service/
    aspect/

  common/
    config/
    dto/
    exception/
    pagination/
    web/
```

Resources:

```text
backend/src/main/resources/
  application.yml
  db/
    migration/
      V001__schema.sql
      V002__seed.sql
      V003__views.sql
      V004__triggers.sql
      V005__security_tables.sql
```

## 2. Основные Entity

Entity отражают существующую БД. Для read-only views используются DTO projections или JdbcTemplate, а не обязательные JPA entity.

### Auth / User

```java
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "personnel_id")
    private Long personnelId;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
```

```java
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleCode code;

    @Column(nullable = false)
    private String name;

    private String description;
}
```

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
```

### Hierarchy

```java
@Entity
@Table(name = "military_formations")
public class MilitaryFormationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "formation_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "formation_type", nullable = false)
    private String formationType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "formation_date")
    private LocalDate formationDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "commander_id")
    private Long commanderId;
}
```

```java
@Entity
@Table(name = "subdivisions")
public class SubdivisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subdivision_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "commander_id")
    private Long commanderId;
}
```

### Unit

```java
@Entity
@Table(name = "military_units")
public class MilitaryUnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "formation_id", nullable = false)
    private Long formationId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "commander_id")
    private Long commanderId;
}
```

### Personnel

```java
@Entity
@Table(name = "personnel")
public class PersonnelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personnel_id")
    private Long id;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "personal_number", nullable = false, unique = true)
    private String personalNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "service_start", nullable = false)
    private LocalDate serviceStart;

    @Column(name = "subdivision_id", nullable = false)
    private Long subdivisionId;
}
```

### Equipment / Weapon

```java
@Entity
@Table(name = "equipment_types")
public class EquipmentTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;
}
```

```java
@Entity
@Table(name = "weapon_types")
public class WeaponTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;
}
```

Для join-таблиц с составным ключом используются `@Embeddable` id:

```java
@Embeddable
public record UnitEquipmentId(
        @Column(name = "unit_id") Long unitId,
        @Column(name = "type_id") Long typeId
) implements Serializable {
}
```

```java
@Entity
@Table(name = "equipment_in_units")
public class UnitEquipmentEntity {

    @EmbeddedId
    private UnitEquipmentId id;

    @Column(nullable = false)
    private Integer quantity;
}
```

### Building / Specialty

```java
@Entity
@Table(name = "buildings")
public class BuildingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "unit_id", nullable = false)
    private Long unitId;
}
```

```java
@Entity
@Table(name = "specialties")
public class SpecialtyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "specialty_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
```

### Audit

```java
@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_event_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String action;

    @Column(name = "object_type", nullable = false)
    private String objectType;

    @Column(name = "object_id")
    private Long objectId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
```

## 3. DTO records

DTO объявляются как Java records.

### Auth DTO

```java
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
```

```java
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        CurrentUserResponse user
) {
}
```

```java
public record CurrentUserResponse(
        Long userId,
        Long soldierId,
        String username,
        String displayName,
        Set<String> roles,
        List<CommandAssignmentResponse> assignments,
        Set<String> permissions
) {
}
```

### Personnel DTO

```java
public record PersonnelResponse(
        Long id,
        String lastName,
        String firstName,
        String middleName,
        String personalNumber,
        LocalDate birthDate,
        LocalDate serviceStart,
        Long subdivisionId,
        String subdivisionName,
        String rankName
) {
}
```

```java
public record CreatePersonnelRequest(
        @NotBlank String lastName,
        @NotBlank String firstName,
        String middleName,
        @NotBlank String personalNumber,
        @NotNull LocalDate birthDate,
        @NotNull LocalDate serviceStart,
        @NotNull Long subdivisionId
) {
}
```

```java
public record UpdatePersonnelRequest(
        @NotBlank String lastName,
        @NotBlank String firstName,
        String middleName,
        @NotNull Long subdivisionId
) {
}
```

### Unit DTO

```java
public record UnitResponse(
        Long id,
        String name,
        Long formationId,
        String formationName,
        Long locationId,
        Long commanderId,
        String commanderName
) {
}
```

```java
public record CreateUnitRequest(
        @NotBlank String name,
        @NotNull Long formationId,
        Long locationId,
        Long commanderId
) {
}
```

```java
public record UpdateUnitRequest(
        @NotBlank String name,
        @NotNull Long formationId,
        Long locationId,
        Long commanderId
) {
}
```

### Equipment DTO

```java
public record UnitEquipmentResponse(
        Long unitId,
        String unitName,
        Long typeId,
        String typeName,
        String categoryName,
        Integer quantity
) {
}
```

```java
public record UpdateQuantityRequest(
        @NotNull
        @PositiveOrZero
        Integer quantity
) {
}
```

### Intelligence DTO

```java
public record QueryDefinitionResponse(
        String code,
        String title,
        String description,
        List<QueryParameterResponse> parameters
) {
}
```

```java
public record QueryExecutionRequest(
        Map<String, Object> parameters
) {
}
```

```java
public record QueryResultResponse(
        String code,
        String title,
        List<String> columns,
        List<Map<String, Object>> rows
) {
}
```

### Common DTO

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
```

```java
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}
```

## 4. Mapper через MapStruct

MapStruct используется для преобразования JPA entities в DTO.

```java
@Mapper(componentModel = "spring")
public interface PersonnelMapper {

    PersonnelResponse toResponse(PersonnelEntity entity);

    PersonnelEntity toEntity(CreatePersonnelRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdatePersonnelRequest request, @MappingTarget PersonnelEntity entity);
}
```

```java
@Mapper(componentModel = "spring")
public interface UnitMapper {

    @Mapping(target = "id", source = "id")
    UnitResponse toResponse(MilitaryUnitEntity entity);

    MilitaryUnitEntity toEntity(CreateUnitRequest request);

    void updateEntity(UpdateUnitRequest request, @MappingTarget MilitaryUnitEntity entity);
}
```

Для DTO, которые собираются из view или сложного SQL, используется ручной `RowMapper`:

```java
public final class UnitEquipmentRowMapper {

    private UnitEquipmentRowMapper() {
    }

    public static UnitEquipmentResponse map(ResultSet rs, int rowNum) throws SQLException {
        return new UnitEquipmentResponse(
                rs.getLong("unit_id"),
                rs.getString("unit_name"),
                rs.getLong("type_id"),
                rs.getString("equipment_type"),
                rs.getString("equipment_category"),
                rs.getInt("quantity")
        );
    }
}
```

## 5. Controllers

Controllers принимают HTTP-запросы, валидируют DTO и делегируют работу service-слою.

```java
@RestController
@RequestMapping("/api/personnel")
@Tag(name = "Personnel")
public class PersonnelController {

    private final PersonnelService personnelService;

    public PersonnelController(PersonnelService personnelService) {
        this.personnelService = personnelService;
    }

    @GetMapping
    public PageResponse<PersonnelResponse> search(PersonnelFilter filter, Pageable pageable) {
        return personnelService.search(filter, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'PERSONNEL', 'READ')")
    public PersonnelResponse findById(@PathVariable Long id) {
        return personnelService.findById(id);
    }

    @PostMapping
    public PersonnelResponse create(@Valid @RequestBody CreatePersonnelRequest request) {
        return personnelService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'PERSONNEL', 'UPDATE')")
    public PersonnelResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePersonnelRequest request
    ) {
        return personnelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(#id, 'PERSONNEL', 'DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        personnelService.delete(id);
    }
}
```

## 6. Services

Service-слой содержит бизнес-логику, проверку доступа и транзакционные границы.

```java
@Service
public class PersonnelService {

    private final PersonnelRepository personnelRepository;
    private final PersonnelMapper personnelMapper;
    private final HierarchyService hierarchyService;
    private final UserContextProvider userContextProvider;
    private final PermissionService permissionService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<PersonnelResponse> search(PersonnelFilter filter, Pageable pageable) {
        UserContext user = userContextProvider.current();
        Page<PersonnelEntity> page = personnelRepository.searchInScope(user, filter, pageable);
        return PageResponseMapper.from(page.map(personnelMapper::toResponse));
    }

    @Transactional
    public PersonnelResponse create(CreatePersonnelRequest request) {
        UserContext user = userContextProvider.current();
        ObjectType parentType = hierarchyService.resolveSubdivisionObjectType(request.subdivisionId());

        if (!permissionService.canCreate(user, parentType, request.subdivisionId(), ObjectType.PERSONNEL)) {
            throw new AccessDeniedException("Access denied");
        }

        PersonnelEntity entity = personnelMapper.toEntity(request);
        PersonnelEntity saved = personnelRepository.save(entity);
        auditService.created(user, ObjectType.PERSONNEL, saved.getId());

        return personnelMapper.toResponse(saved);
    }
}
```

Сложные SQL-запросы выполняются отдельными service/repository классами:

```java
@Service
public class IntelligenceService {

    private final QueryRegistry queryRegistry;
    private final IntelligenceRepository intelligenceRepository;
    private final QueryPermissionService queryPermissionService;

    public QueryResultResponse execute(String code, QueryExecutionRequest request) {
        QueryDefinition definition = queryRegistry.getRequired(code);
        queryPermissionService.checkCanExecute(definition, request);
        return intelligenceRepository.execute(definition, request.parameters());
    }
}
```

## 7. Repositories

CRUD repositories используют Spring Data JPA.

```java
public interface PersonnelRepository
        extends JpaRepository<PersonnelEntity, Long>, PersonnelScopeRepository {
}
```

```java
public interface MilitaryUnitRepository extends JpaRepository<MilitaryUnitEntity, Long> {

    Page<MilitaryUnitEntity> findByFormationId(Long formationId, Pageable pageable);
}
```

Для scope-фильтрации используются custom repositories.

```java
public interface PersonnelScopeRepository {

    Page<PersonnelEntity> searchInScope(UserContext user, PersonnelFilter filter, Pageable pageable);
}
```

```java
@Repository
public class PersonnelScopeRepositoryImpl implements PersonnelScopeRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PersonnelScopeRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Page<PersonnelEntity> searchInScope(UserContext user, PersonnelFilter filter, Pageable pageable) {
        String sql = """
                SELECT DISTINCT p.*
                FROM personnel p
                JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
                JOIN military_units mu ON mu.unit_id = s.unit_id
                LEFT JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id
                JOIN command_assignments ca ON ca.soldier_id = :soldierId
                WHERE (:search IS NULL OR lower(p.last_name || ' ' || p.first_name) LIKE lower(:search))
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("soldierId", user.soldierId())
                .addValue("search", filter.search() == null ? null : "%" + filter.search() + "%");

        List<PersonnelEntity> content = jdbcTemplate.query(sql, params, personnelRowMapper());
        return new PageImpl<>(content, pageable, content.size());
    }
}
```

Repository для сложных запросов:

```java
@Repository
public class IntelligenceRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public IntelligenceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public QueryResultResponse execute(QueryDefinition definition, Map<String, Object> parameters) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(definition.sql(), parameters);
        List<String> columns = rows.isEmpty()
                ? definition.columns()
                : rows.getFirst().keySet().stream().toList();

        return new QueryResultResponse(definition.code(), definition.title(), columns, rows);
    }
}
```

## 8. GlobalExceptionHandler

Ошибки приводятся к единому JSON-формату.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleNotFound(EntityNotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorResponse handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (left, right) -> left
                ));

        return new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed",
                request.getRequestURI(),
                errors
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "DATA_INTEGRITY_ERROR", "Data integrity constraint violation", request);
    }

    private ApiErrorResponse error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                Map.of()
        );
    }
}
```

## 9. Pagination / sorting / filtering

Списковые endpoint'ы принимают стандартные параметры:

```http
GET /api/personnel?page=0&size=20&sort=lastName,asc&lastName=Иванов&rank=Майор
```

Общие правила:

- `page` начинается с `0`;
- `size` имеет верхний лимит;
- `sort` принимает whitelist полей;
- фильтры описываются отдельными record-классами;
- scope-фильтр применяется всегда;
- пользовательские фильтры применяются поверх scope.

Filter DTO:

```java
public record PersonnelFilter(
        String search,
        String lastName,
        Long subdivisionId,
        Long unitId,
        String rank,
        String specialty
) {
}
```

Pageable config:

```java
@Configuration
public class PageableConfig implements WebMvcConfigurer {

    @Bean
    PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(100);
            resolver.setOneIndexedParameters(false);
        };
    }
}
```

Response:

```json
{
  "content": [
    {
      "id": 17,
      "lastName": "Иванов",
      "firstName": "Петр",
      "middleName": "Сергеевич",
      "personalNumber": "PN-10017",
      "birthDate": "1992-04-12",
      "serviceStart": "2014-09-01",
      "subdivisionId": 8,
      "subdivisionName": "1-й взвод",
      "rankName": "Майор"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## 10. OpenAPI / Swagger

Используется `springdoc-openapi`.

Dependency:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc.version}</version>
</dependency>
```

Configuration:

```java
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI tacticalDistrictOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TACTICAL DISTRICT COMMAND API")
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

Swagger UI:

```http
GET /swagger-ui/index.html
GET /v3/api-docs
```

Controller annotation:

```java
@Operation(summary = "Get personnel by id")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Personnel found"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "404", description = "Personnel not found")
})
```

## 11. Audit log для CRUD-операций

Audit фиксирует изменения данных.

Событие содержит:

- user id;
- action;
- object type;
- object id;
- status;
- timestamp;
- request path;
- ip address.

Actions:

```text
CREATE
UPDATE
DELETE
LOGIN
LOGOUT
EXECUTE_QUERY
ACKNOWLEDGE_ALERT
```

AuditService:

```java
public interface AuditService {

    void created(UserContext user, ObjectType objectType, Long objectId);

    void updated(UserContext user, ObjectType objectType, Long objectId);

    void deleted(UserContext user, ObjectType objectType, Long objectId);

    void queryExecuted(UserContext user, String queryCode);
}
```

Aspect:

```java
@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;
    private final UserContextProvider userContextProvider;

    @AfterReturning("@annotation(audited)")
    public void afterSuccess(JoinPoint joinPoint, Audited audited) {
        UserContext user = userContextProvider.current();
        Long objectId = extractObjectId(joinPoint);
        auditService.updated(user, audited.objectType(), objectId);
    }

    private Long extractObjectId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof Long id)) {
            return null;
        }
        return id;
    }
}
```

Annotation:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    ObjectType objectType();
    PermissionAction action();
}
```

Usage:

```java
@Audited(objectType = ObjectType.PERSONNEL, action = PermissionAction.UPDATE)
public PersonnelResponse update(Long id, UpdatePersonnelRequest request) {
    ...
}
```

## 12. Примеры REST endpoints

### Auth

```http
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me
```

### Users

```http
GET    /api/users
GET    /api/users/{id}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}
POST   /api/users/{id}/roles
DELETE /api/users/{id}/roles/{roleCode}
```

### Hierarchy

```http
GET    /api/hierarchy/tree
GET    /api/formations
GET    /api/formations/{id}
POST   /api/formations
PUT    /api/formations/{id}
DELETE /api/formations/{id}
GET    /api/subdivisions
GET    /api/subdivisions/{id}
POST   /api/subdivisions
PUT    /api/subdivisions/{id}
DELETE /api/subdivisions/{id}
```

### Unit

```http
GET    /api/units
GET    /api/units/{id}
POST   /api/units
PUT    /api/units/{id}
DELETE /api/units/{id}
```

### Personnel

```http
GET    /api/personnel
GET    /api/personnel/{id}
POST   /api/personnel
PUT    /api/personnel/{id}
DELETE /api/personnel/{id}
GET    /api/personnel/{id}/specialties
POST   /api/personnel/{id}/specialties/{specialtyId}
DELETE /api/personnel/{id}/specialties/{specialtyId}
```

### Equipment

```http
GET /api/equipment/types
GET /api/equipment/categories
GET /api/units/{unitId}/equipment
PUT /api/units/{unitId}/equipment/{typeId}
```

### Weapon

```http
GET /api/weapons/types
GET /api/weapons/categories
GET /api/units/{unitId}/weapons
PUT /api/units/{unitId}/weapons/{typeId}
```

### Building

```http
GET    /api/buildings
GET    /api/buildings/{id}
POST   /api/buildings
PUT    /api/buildings/{id}
DELETE /api/buildings/{id}
GET    /api/buildings/usage
```

### Specialty

```http
GET    /api/specialties
GET    /api/specialties/{id}
POST   /api/specialties
PUT    /api/specialties/{id}
DELETE /api/specialties/{id}
```

### Intelligence

```http
GET  /api/intelligence/queries
POST /api/intelligence/queries/{code}/execute
```

### Dashboard

```http
GET /api/dashboard/summary
GET /api/dashboard/readiness
GET /api/dashboard/resources
```

### Alert

```http
GET  /api/alerts
GET  /api/alerts/{id}
POST /api/alerts/{id}/acknowledge
POST /api/alerts/{id}/resolve
```

### Report

```http
GET /api/reports/readiness
GET /api/reports/personnel
GET /api/reports/equipment
GET /api/reports/weapons
GET /api/reports/buildings
```

### Audit

```http
GET /api/audit/events
GET /api/audit/events/{id}
```

## 13. Примеры request/response DTO

### Login

Request:

```json
{
  "username": "unit.cmd.1",
  "password": "password"
}
```

Response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "d5db3b6e7e...",
  "expiresIn": 900,
  "user": {
    "userId": 5,
    "soldierId": 5,
    "username": "unit.cmd.1",
    "displayName": "Командир части",
    "roles": ["UNIT_COMMANDER"],
    "assignments": [
      {
        "objectType": "MILITARY_UNIT",
        "objectId": 1
      }
    ],
    "permissions": [
      "personnel:read",
      "personnel:update",
      "equipment:read",
      "equipment:update",
      "weapon:read",
      "weapon:update"
    ]
  }
}
```

### Create personnel

Request:

```json
{
  "lastName": "Иванов",
  "firstName": "Петр",
  "middleName": "Сергеевич",
  "personalNumber": "PN-10017",
  "birthDate": "1992-04-12",
  "serviceStart": "2014-09-01",
  "subdivisionId": 8
}
```

Response:

```json
{
  "id": 17,
  "lastName": "Иванов",
  "firstName": "Петр",
  "middleName": "Сергеевич",
  "personalNumber": "PN-10017",
  "birthDate": "1992-04-12",
  "serviceStart": "2014-09-01",
  "subdivisionId": 8,
  "subdivisionName": "1-й взвод",
  "rankName": null
}
```

### Update unit equipment

Request:

```json
{
  "quantity": 14
}
```

Response:

```json
{
  "unitId": 1,
  "unitName": "101-я военная часть",
  "typeId": 3,
  "typeName": "Танк",
  "categoryName": "Бронетехника",
  "quantity": 14
}
```

### Execute intelligence query

Request:

```json
{
  "parameters": {
    "formationName": "Западный военный округ"
  }
}
```

Response:

```json
{
  "code": "TASK_01",
  "title": "Военные части выбранного формирования",
  "columns": [
    "unit_id",
    "unit_name",
    "parent_formation_name",
    "parent_formation_type",
    "commander_last_name",
    "commander_first_name",
    "commander_middle_name",
    "commander_rank"
  ],
  "rows": [
    {
      "unit_id": 1,
      "unit_name": "101-я военная часть",
      "parent_formation_name": "1-я армия",
      "parent_formation_type": "Армия",
      "commander_last_name": "Иванов",
      "commander_first_name": "Петр",
      "commander_middle_name": "Сергеевич",
      "commander_rank": "Майор"
    }
  ]
}
```

### API error

```json
{
  "timestamp": "2026-05-13T11:20:00Z",
  "status": 403,
  "code": "FORBIDDEN",
  "message": "Access denied",
  "path": "/api/personnel/17",
  "validationErrors": {}
}
```

## Правило выбора доступа к данным

```text
CRUD -> Spring Data JPA
Views and analytical queries -> NamedParameterJdbcTemplate
Authorization -> PermissionService + AccessControlService
Audit -> AuditService / AuditAspect
Errors -> GlobalExceptionHandler
API documentation -> OpenAPI / Swagger
```
