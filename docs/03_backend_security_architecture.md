# TACTICAL DISTRICT COMMAND: backend security architecture

## Назначение

Документ описывает backend security architecture для **TACTICAL DISTRICT COMMAND** на Java 21+ и Spring Boot 4.x.

Модель доступа:

```text
Authentication = JWT
Authorization = role permissions + command scope
Scope source = command_assignments
```

Frontend скрывает недоступные кнопки и разделы интерфейса. Backend выполняет все проверки доступа самостоятельно.

## 1. Spring Security architecture

Security-слой состоит из следующих компонентов:

```text
HTTP Request
  -> SecurityFilterChain
  -> JwtAuthenticationFilter
  -> JwtService
  -> UserPrincipal
  -> @PreAuthorize / PermissionEvaluator
  -> Controller
  -> Service
  -> PermissionService
  -> AccessControlService
  -> Repository / SQL
```

Основные классы:

```text
security/
  SecurityConfig.java
  JwtAuthenticationFilter.java
  JwtService.java
  RestAuthenticationEntryPoint.java
  RestAccessDeniedHandler.java
  TacticalPermissionEvaluator.java

auth/
  AuthController.java
  AuthService.java
  RefreshTokenService.java
  LoginRequest.java
  TokenResponse.java

user/
  UserPrincipal.java
  TacticalUserDetailsService.java
  UserContext.java
  UserContextProvider.java

access/
  RoleCode.java
  PermissionAction.java
  ObjectType.java
  PermissionService.java
  AccessControlService.java
  PermissionMatrix.java
```

Security configuration:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

## 2. Login / refresh / logout flow

### Login

```http
POST /api/auth/login
```

Flow:

```text
1. Пользователь отправляет username/password.
2. AuthService загружает пользователя из users.
3. PasswordEncoder проверяет пароль.
4. Backend проверяет users.is_active.
5. Backend загружает роли из user_roles.
6. Backend выпускает access JWT.
7. Backend создает refresh token, сохраняет его hash в БД.
8. Backend возвращает accessToken, refreshToken и краткий профиль пользователя.
```

Response:

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "b7f8a5...",
  "expiresIn": 900,
  "user": {
    "userId": 10,
    "soldierId": 42,
    "username": "unit.cmd.1",
    "roles": ["UNIT_COMMANDER"]
  }
}
```

### Refresh

```http
POST /api/auth/refresh
```

Flow:

```text
1. Frontend отправляет refreshToken.
2. Backend ищет hash refresh token в БД.
3. Backend проверяет срок действия, revoked_at и пользователя.
4. Backend отзывает использованный refresh token.
5. Backend выпускает новую пару access/refresh token.
6. Backend возвращает новые токены.
```

Refresh token rotation снижает риск повторного использования украденного refresh token.

### Logout

```http
POST /api/auth/logout
```

Flow:

```text
1. Backend получает текущего пользователя из JWT.
2. Backend отзывает активные refresh tokens пользователя или конкретный refresh token.
3. Access token остается stateless и истекает по exp.
4. Frontend удаляет локальные токены.
```

Таблица refresh tokens:

```sql
CREATE TABLE refresh_tokens (
    refresh_token_id SERIAL PRIMARY KEY,
    user_id          INTEGER NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash       VARCHAR(255) NOT NULL UNIQUE,
    issued_at        TIMESTAMP NOT NULL DEFAULT now(),
    expires_at       TIMESTAMP NOT NULL,
    revoked_at       TIMESTAMP NULL,
    replaced_by_hash VARCHAR(255) NULL,
    user_agent       TEXT NULL,
    ip_address       VARCHAR(64) NULL
);
```

## 3. JWT payload

Access token содержит идентичность и роли. Command scope не хранится как источник истины в JWT, потому что назначения меняются в БД.

Payload:

```json
{
  "iss": "tactical-district-command",
  "sub": "unit.cmd.1",
  "uid": 10,
  "sid": 42,
  "roles": ["UNIT_COMMANDER"],
  "typ": "access",
  "jti": "5b5a0f7e-7e12-4c4a-9d15-b39b7b8b8d3a",
  "iat": 1778660000,
  "exp": 1778660900
}
```

Поля:

| Claim | Назначение |
|---|---|
| `iss` | issuer приложения |
| `sub` | username |
| `uid` | `users.user_id` |
| `sid` | `users.personnel_id`, используется как `soldier_id` |
| `roles` | роли пользователя |
| `typ` | тип токена: `access` |
| `jti` | идентификатор токена |
| `iat` | время выпуска |
| `exp` | время истечения |

Рекомендуемое время жизни:

```text
access token: 10-15 минут
refresh token: 7-30 дней
```

## 4. UserDetails / UserPrincipal

`UserPrincipal` представляет authenticated principal внутри Spring Security.

```java
public final class UserPrincipal implements UserDetails {

    private final Long userId;
    private final Long soldierId;
    private final String username;
    private final String passwordHash;
    private final boolean active;
    private final Set<RoleCode> roles;

    public UserPrincipal(
            Long userId,
            Long soldierId,
            String username,
            String passwordHash,
            boolean active,
            Set<RoleCode> roles
    ) {
        this.userId = userId;
        this.soldierId = soldierId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.active = active;
        this.roles = Set.copyOf(roles);
    }

    public Long userId() {
        return userId;
    }

    public Long soldierId() {
        return soldierId;
    }

    public Set<RoleCode> roles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
```

UserDetailsService:

```java
@Service
public class TacticalUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public TacticalUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findPrincipalByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
```

## 5. UserContext

`UserContext` является удобной domain-моделью текущего пользователя для service-слоя.

```java
public record UserContext(
        Long userId,
        Long soldierId,
        String username,
        Set<RoleCode> roles
) {
    public boolean hasRole(RoleCode role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(RoleCode... requiredRoles) {
        return Arrays.stream(requiredRoles).anyMatch(roles::contains);
    }
}
```

Provider:

```java
@Component
public class UserContextProvider {

    public UserContext current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }

        return new UserContext(
                principal.userId(),
                principal.soldierId(),
                principal.getUsername(),
                principal.roles()
        );
    }
}
```

## 6. AccessControlService

`AccessControlService` отвечает только за scope: находится ли объект внутри области командования пользователя.

Command assignment:

```text
command_assignments.soldier_id
command_assignments.object_type
command_assignments.object_id
```

`soldier_id` соответствует `users.personnel_id` и `personnel.personnel_id`.

Интерфейс:

```java
public interface AccessControlService {

    boolean isInScope(UserContext user, ObjectType objectType, Long objectId);

    Set<Long> accessibleFormationIds(UserContext user);

    Set<Long> accessibleUnitIds(UserContext user);

    Set<Long> accessibleSubdivisionIds(UserContext user);

    Set<Long> accessiblePersonnelIds(UserContext user);
}
```

Пример реализации для unit scope:

```java
@Service
public class SqlAccessControlService implements AccessControlService {

    private final JdbcClient jdbcClient;

    public SqlAccessControlService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public boolean isInScope(UserContext user, ObjectType objectType, Long objectId) {
        if (user.hasRole(RoleCode.ADMIN_DISTRICT)) {
            return true;
        }
        if (user.soldierId() == null) {
            return false;
        }

        return switch (objectType) {
            case FORMATION, ARMY, CORPS, DIVISION, BRIGADE ->
                    isFormationInScope(user.soldierId(), objectId);
            case MILITARY_UNIT ->
                    isUnitInScope(user.soldierId(), objectId);
            case BATTALION, COMPANY, PLATOON, SQUAD ->
                    isSubdivisionInScope(user.soldierId(), objectId);
            case PERSONNEL, SELF ->
                    isPersonnelInScope(user.soldierId(), objectId);
            case EQUIPMENT, WEAPON, BUILDING ->
                    isResourceInScope(user.soldierId(), objectType, objectId);
            case DISTRICT ->
                    hasDistrictAssignment(user.soldierId(), objectId);
        };
    }

    private boolean isUnitInScope(Long soldierId, Long unitId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM command_assignments ca
                    JOIN military_units mu ON mu.unit_id = :unitId
                    LEFT JOIN v_formation_closure fc
                        ON fc.descendant_formation_id = mu.formation_id
                    WHERE ca.soldier_id = :soldierId
                      AND (
                          (ca.object_type = 'MILITARY_UNIT' AND ca.object_id = mu.unit_id)
                          OR (ca.object_type IN ('DISTRICT', 'FORMATION', 'ARMY', 'CORPS', 'DIVISION', 'BRIGADE')
                              AND ca.object_id = fc.root_formation_id)
                      )
                )
                """;

        return Boolean.TRUE.equals(jdbcClient.sql(sql)
                .param("soldierId", soldierId)
                .param("unitId", unitId)
                .query(Boolean.class)
                .single());
    }

    private boolean isFormationInScope(Long soldierId, Long formationId) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM command_assignments ca
                    JOIN v_formation_closure fc
                        ON fc.descendant_formation_id = :formationId
                    WHERE ca.soldier_id = :soldierId
                      AND ca.object_type IN ('DISTRICT', 'FORMATION', 'ARMY', 'CORPS', 'DIVISION', 'BRIGADE')
                      AND ca.object_id = fc.root_formation_id
                )
                """;

        return Boolean.TRUE.equals(jdbcClient.sql(sql)
                .param("soldierId", soldierId)
                .param("formationId", formationId)
                .query(Boolean.class)
                .single());
    }
}
```

## 7. PermissionService

`PermissionService` объединяет role permission и scope permission.

```java
public interface PermissionService {

    boolean canRead(UserContext user, ObjectType objectType, Long objectId);

    boolean canCreate(UserContext user, ObjectType parentType, Long parentId, ObjectType targetType);

    boolean canUpdate(UserContext user, ObjectType objectType, Long objectId);

    boolean canDelete(UserContext user, ObjectType objectType, Long objectId);

    boolean hasPermission(UserContext user, PermissionAction action, ObjectType objectType, Long objectId);
}
```

Implementation:

```java
@Service
public class DefaultPermissionService implements PermissionService {

    private final PermissionMatrix permissionMatrix;
    private final AccessControlService accessControlService;

    public DefaultPermissionService(
            PermissionMatrix permissionMatrix,
            AccessControlService accessControlService
    ) {
        this.permissionMatrix = permissionMatrix;
        this.accessControlService = accessControlService;
    }

    @Override
    public boolean canRead(UserContext user, ObjectType objectType, Long objectId) {
        return hasPermission(user, PermissionAction.READ, objectType, objectId);
    }

    @Override
    public boolean canCreate(UserContext user, ObjectType parentType, Long parentId, ObjectType targetType) {
        boolean roleAllowed = permissionMatrix.isAllowed(user.roles(), PermissionAction.CREATE, targetType);
        boolean scopeAllowed = accessControlService.isInScope(user, parentType, parentId);
        return roleAllowed && scopeAllowed;
    }

    @Override
    public boolean canUpdate(UserContext user, ObjectType objectType, Long objectId) {
        return hasPermission(user, PermissionAction.UPDATE, objectType, objectId);
    }

    @Override
    public boolean canDelete(UserContext user, ObjectType objectType, Long objectId) {
        return hasPermission(user, PermissionAction.DELETE, objectType, objectId);
    }

    @Override
    public boolean hasPermission(UserContext user, PermissionAction action, ObjectType objectType, Long objectId) {
        boolean roleAllowed = permissionMatrix.isAllowed(user.roles(), action, objectType);
        boolean scopeAllowed = accessControlService.isInScope(user, objectType, objectId);
        return roleAllowed && scopeAllowed;
    }
}
```

Enums:

```java
public enum RoleCode {
    ADMIN_DISTRICT,
    STAFF_ANALYST,
    ARMY_COMMANDER,
    FORMATION_COMMANDER,
    UNIT_COMMANDER,
    COMPANY_COMMANDER,
    PLATOON_COMMANDER,
    SQUAD_COMMANDER,
    SOLDIER
}
```

```java
public enum PermissionAction {
    READ,
    CREATE,
    UPDATE,
    DELETE,
    EXECUTE_QUERY,
    ACKNOWLEDGE_ALERT
}
```

```java
public enum ObjectType {
    DISTRICT,
    FORMATION,
    ARMY,
    CORPS,
    DIVISION,
    BRIGADE,
    MILITARY_UNIT,
    BATTALION,
    COMPANY,
    PLATOON,
    SQUAD,
    PERSONNEL,
    SELF,
    EQUIPMENT,
    WEAPON,
    BUILDING,
    SPECIALTY,
    QUERY,
    ALERT
}
```

## 8. canRead / canCreate / canUpdate / canDelete

Access decision выполняется одинаково для всех действий:

```text
1. Получить UserContext.
2. Проверить, разрешает ли роль действие над типом объекта.
3. Проверить, входит ли объект в command scope пользователя.
4. Вернуть 200/201/204 или 403.
```

Read:

```java
public PersonnelDto getPersonnel(Long personnelId) {
    UserContext user = userContextProvider.current();

    if (!permissionService.canRead(user, ObjectType.PERSONNEL, personnelId)) {
        throw new AccessDeniedException("Access denied");
    }

    return personnelRepository.findDtoById(personnelId)
            .orElseThrow(() -> new EntityNotFoundException("Personnel not found"));
}
```

Create:

```java
public PersonnelDto createPersonnel(Long subdivisionId, CreatePersonnelRequest request) {
    UserContext user = userContextProvider.current();

    if (!permissionService.canCreate(user, ObjectType.PLATOON, subdivisionId, ObjectType.PERSONNEL)) {
        throw new AccessDeniedException("Access denied");
    }

    return personnelRepository.create(subdivisionId, request);
}
```

Update:

```java
public EquipmentDto updateUnitEquipment(Long unitId, Long typeId, UpdateQuantityRequest request) {
    UserContext user = userContextProvider.current();

    if (!permissionService.canUpdate(user, ObjectType.MILITARY_UNIT, unitId)) {
        throw new AccessDeniedException("Access denied");
    }

    return equipmentRepository.updateQuantity(unitId, typeId, request.quantity());
}
```

Delete:

```java
public void deleteSubdivision(Long subdivisionId) {
    UserContext user = userContextProvider.current();

    if (!permissionService.canDelete(user, ObjectType.PLATOON, subdivisionId)) {
        throw new AccessDeniedException("Access denied");
    }

    subdivisionRepository.deleteById(subdivisionId);
}
```

## 9. Фильтрация данных по scope

Backend не возвращает данные вне scope. Фильтрация выполняется в SQL-запросах или repository-методах.

Плохой подход:

```text
1. Загрузить все записи.
2. Отфильтровать в Java.
```

Правильный подход:

```text
1. Определить scope пользователя.
2. Передать soldierId/user roles в SQL.
3. Вернуть только доступные строки.
```

Пример списка частей:

```java
public List<UnitDto> findVisibleUnits(UserContext user) {
    if (user.hasRole(RoleCode.ADMIN_DISTRICT) || user.hasRole(RoleCode.STAFF_ANALYST)) {
        return unitRepository.findAllUnits();
    }

    return unitRepository.findUnitsInScope(user.soldierId());
}
```

SQL:

```sql
SELECT DISTINCT
    mu.unit_id,
    mu.name,
    mu.formation_id,
    mf.name AS formation_name
FROM military_units mu
JOIN military_formations mf ON mf.formation_id = mu.formation_id
LEFT JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id
JOIN command_assignments ca
    ON ca.soldier_id = :soldierId
WHERE
    (ca.object_type = 'MILITARY_UNIT' AND ca.object_id = mu.unit_id)
    OR (
        ca.object_type IN ('DISTRICT', 'FORMATION', 'ARMY', 'CORPS', 'DIVISION', 'BRIGADE')
        AND ca.object_id = fc.root_formation_id
    );
```

Фильтрация personnel:

```sql
SELECT DISTINCT p.*
FROM personnel p
JOIN subdivisions s ON s.subdivision_id = p.subdivision_id
JOIN military_units mu ON mu.unit_id = s.unit_id
LEFT JOIN v_formation_closure fc ON fc.descendant_formation_id = mu.formation_id
JOIN command_assignments ca ON ca.soldier_id = :soldierId
WHERE
    p.personnel_id = :soldierId
    OR (ca.object_type = 'MILITARY_UNIT' AND ca.object_id = mu.unit_id)
    OR (
        ca.object_type IN ('DISTRICT', 'FORMATION', 'ARMY', 'CORPS', 'DIVISION', 'BRIGADE')
        AND ca.object_id = fc.root_formation_id
    )
    OR (
        ca.object_type IN ('BATTALION', 'COMPANY', 'PLATOON', 'SQUAD')
        AND s.subdivision_id IN (
            WITH RECURSIVE sub_tree AS (
                SELECT subdivision_id, parent_id
                FROM subdivisions
                WHERE subdivision_id = ca.object_id
                UNION ALL
                SELECT child.subdivision_id, child.parent_id
                FROM subdivisions child
                JOIN sub_tree parent ON child.parent_id = parent.subdivision_id
            )
            SELECT subdivision_id FROM sub_tree
        )
    );
```

## 10. Использование `@PreAuthorize`

`@PreAuthorize` применяется на controller или service methods.

Проверка только роли:

```java
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN_DISTRICT')")
public List<UserDto> users() {
    return userService.findAll();
}
```

Проверка роли и custom permission:

```java
@PutMapping("/units/{unitId}/equipment/{typeId}")
@PreAuthorize("hasPermission(#unitId, 'MILITARY_UNIT', 'UPDATE')")
public EquipmentDto updateEquipment(
        @PathVariable Long unitId,
        @PathVariable Long typeId,
        @RequestBody UpdateQuantityRequest request
) {
    return equipmentService.updateUnitEquipment(unitId, typeId, request);
}
```

Query terminal:

```java
@PostMapping("/queries/{code}/execute")
@PreAuthorize("@queryPermissionService.canExecute(authentication, #code, #request)")
public QueryResultDto execute(
        @PathVariable String code,
        @RequestBody QueryExecutionRequest request
) {
    return queryService.execute(code, request);
}
```

## 11. Custom PermissionEvaluator

Configuration:

```java
@Configuration
public class MethodSecurityConfig {

    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler(
            PermissionEvaluator permissionEvaluator
    ) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
```

Evaluator:

```java
@Component
public class TacticalPermissionEvaluator implements PermissionEvaluator {

    private final PermissionService permissionService;
    private final UserContextFactory userContextFactory;

    public TacticalPermissionEvaluator(
            PermissionService permissionService,
            UserContextFactory userContextFactory
    ) {
        this.permissionService = permissionService;
        this.userContextFactory = userContextFactory;
    }

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Object targetId,
            Object targetType,
            Object permission
    ) {
        if (authentication == null || targetId == null || targetType == null || permission == null) {
            return false;
        }

        UserContext user = userContextFactory.from(authentication);
        ObjectType objectType = ObjectType.valueOf(targetType.toString());
        PermissionAction action = PermissionAction.valueOf(permission.toString());
        Long objectId = Long.valueOf(targetId.toString());

        return permissionService.hasPermission(user, action, objectType, objectId);
    }

    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission
    ) {
        return hasPermission(authentication, (Object) targetId, targetType, permission);
    }
}
```

Factory:

```java
@Component
public class UserContextFactory {

    public UserContext from(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }

        return new UserContext(
                principal.userId(),
                principal.soldierId(),
                principal.getUsername(),
                principal.roles()
        );
    }
}
```

## 12. Обработка 401 и 403

`401 Unauthorized` используется, когда пользователь не аутентифицирован:

- нет JWT;
- JWT просрочен;
- JWT поврежден;
- JWT имеет неправильную подпись;
- пользователь отключен.

`403 Forbidden` используется, когда пользователь аутентифицирован, но не имеет прав:

- роль не разрешает действие;
- объект находится вне command scope;
- query terminal запрос недоступен роли.

AuthenticationEntryPoint:

```java
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), ApiError.unauthorized(
                "UNAUTHORIZED",
                "Authentication is required",
                request.getRequestURI()
        ));
    }
}
```

AccessDeniedHandler:

```java
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(response.getOutputStream(), ApiError.forbidden(
                "FORBIDDEN",
                "Access denied",
                request.getRequestURI()
        ));
    }
}
```

Error DTO:

```java
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public static ApiError unauthorized(String code, String message, String path) {
        return new ApiError(Instant.now(), 401, code, message, path);
    }

    public static ApiError forbidden(String code, String message, String path) {
        return new ApiError(Instant.now(), 403, code, message, path);
    }
}
```

## 13. Примеры классов и интерфейсов

### JwtAuthenticationFilter

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TacticalUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            TacticalUserDetailsService userDetailsService
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveBearerToken(request);

        if (token != null && jwtService.isValidAccessToken(token)) {
            String username = jwtService.username(token);
            UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }
}
```

### JwtService

```java
public interface JwtService {

    String createAccessToken(UserPrincipal principal);

    boolean isValidAccessToken(String token);

    String username(String token);

    Long userId(String token);

    Long soldierId(String token);

    Set<RoleCode> roles(String token);
}
```

### AuthController

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    public CurrentUserResponse me() {
        return authService.currentUser();
    }
}
```

### AuthService

```java
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final TacticalUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserContextProvider userContextProvider;

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(request.username());
        String accessToken = jwtService.createAccessToken(principal);
        String refreshToken = refreshTokenService.create(principal.userId());

        return TokenResponse.of(accessToken, refreshToken, principal);
    }

    public TokenResponse refresh(RefreshRequest request) {
        UserPrincipal principal = refreshTokenService.rotate(request.refreshToken());
        String accessToken = jwtService.createAccessToken(principal);
        String refreshToken = refreshTokenService.create(principal.userId());

        return TokenResponse.of(accessToken, refreshToken, principal);
    }

    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    public CurrentUserResponse currentUser() {
        UserContext user = userContextProvider.current();
        return CurrentUserResponse.from(user);
    }
}
```

### PermissionMatrix

```java
@Component
public class PermissionMatrix {

    private final Map<RoleCode, Map<ObjectType, Set<PermissionAction>>> matrix = Map.of(
            RoleCode.ADMIN_DISTRICT, allPermissions(),
            RoleCode.STAFF_ANALYST, readOnlyAnalytics(),
            RoleCode.UNIT_COMMANDER, unitCommanderPermissions(),
            RoleCode.SOLDIER, soldierPermissions()
    );

    public boolean isAllowed(Set<RoleCode> roles, PermissionAction action, ObjectType objectType) {
        return roles.stream().anyMatch(role ->
                matrix.getOrDefault(role, Map.of())
                        .getOrDefault(objectType, Set.of())
                        .contains(action)
        );
    }

    private Map<ObjectType, Set<PermissionAction>> allPermissions() {
        return Arrays.stream(ObjectType.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        type -> EnumSet.allOf(PermissionAction.class)
                ));
    }

    private Map<ObjectType, Set<PermissionAction>> readOnlyAnalytics() {
        return Map.of(
                ObjectType.FORMATION, Set.of(PermissionAction.READ),
                ObjectType.MILITARY_UNIT, Set.of(PermissionAction.READ),
                ObjectType.PERSONNEL, Set.of(PermissionAction.READ),
                ObjectType.EQUIPMENT, Set.of(PermissionAction.READ),
                ObjectType.WEAPON, Set.of(PermissionAction.READ),
                ObjectType.BUILDING, Set.of(PermissionAction.READ),
                ObjectType.QUERY, Set.of(PermissionAction.EXECUTE_QUERY)
        );
    }

    private Map<ObjectType, Set<PermissionAction>> unitCommanderPermissions() {
        return Map.of(
                ObjectType.MILITARY_UNIT, Set.of(PermissionAction.READ, PermissionAction.UPDATE),
                ObjectType.PERSONNEL, Set.of(PermissionAction.READ, PermissionAction.CREATE, PermissionAction.UPDATE),
                ObjectType.EQUIPMENT, Set.of(PermissionAction.READ, PermissionAction.UPDATE),
                ObjectType.WEAPON, Set.of(PermissionAction.READ, PermissionAction.UPDATE),
                ObjectType.BUILDING, Set.of(PermissionAction.READ, PermissionAction.UPDATE)
        );
    }

    private Map<ObjectType, Set<PermissionAction>> soldierPermissions() {
        return Map.of(
                ObjectType.SELF, Set.of(PermissionAction.READ),
                ObjectType.PERSONNEL, Set.of(PermissionAction.READ)
        );
    }
}
```

### Controller с backend-проверкой доступа

```java
@RestController
@RequestMapping("/api/personnel")
public class PersonnelController {

    private final PersonnelService personnelService;

    public PersonnelController(PersonnelService personnelService) {
        this.personnelService = personnelService;
    }

    @GetMapping("/{personnelId}")
    @PreAuthorize("hasPermission(#personnelId, 'PERSONNEL', 'READ')")
    public PersonnelDto findById(@PathVariable Long personnelId) {
        return personnelService.findById(personnelId);
    }

    @PutMapping("/{personnelId}")
    @PreAuthorize("hasPermission(#personnelId, 'PERSONNEL', 'UPDATE')")
    public PersonnelDto update(
            @PathVariable Long personnelId,
            @RequestBody UpdatePersonnelRequest request
    ) {
        return personnelService.update(personnelId, request);
    }
}
```

## Итоговая схема проверки

```text
JWT confirms identity.
Roles define allowed actions.
command_assignments define allowed scope.
PermissionService combines action and scope.
Repositories return only scoped data.
Frontend hides unavailable UI controls.
Backend enforces every permission.
```
