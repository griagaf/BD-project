INSERT INTO roles (code, name, description) VALUES
('ADMIN_DISTRICT', 'District administrator', 'Full system access'),
('STAFF_ANALYST', 'Staff analyst', 'Read-only analytical access'),
('ARMY_COMMANDER', 'Army commander', 'Command access within assigned army'),
('FORMATION_COMMANDER', 'Formation commander', 'Command access within assigned formation'),
('UNIT_COMMANDER', 'Unit commander', 'Command access within assigned military unit'),
('COMPANY_COMMANDER', 'Company commander', 'Command access within assigned company'),
('PLATOON_COMMANDER', 'Platoon commander', 'Command access within assigned platoon'),
('SQUAD_COMMANDER', 'Squad commander', 'Command access within assigned squad'),
('SOLDIER', 'Soldier', 'Personal profile access')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions (code, name, description) VALUES
('dashboard:read', 'Read dashboard', 'Access tactical dashboard'),
('structure:read', 'Read structure', 'Read hierarchy and structure'),
('personnel:read', 'Read personnel', 'Read personnel records'),
('personnel:create', 'Create personnel', 'Create personnel records'),
('personnel:update', 'Update personnel', 'Update personnel records'),
('personnel:delete', 'Delete personnel', 'Delete personnel records'),
('unit:read', 'Read units', 'Read military units'),
('unit:create', 'Create units', 'Create military units'),
('unit:update', 'Update units', 'Update military units'),
('unit:delete', 'Delete units', 'Delete military units'),
('equipment:read', 'Read equipment', 'Read equipment records'),
('equipment:update', 'Update equipment', 'Update equipment records'),
('weapon:read', 'Read weapons', 'Read weapon records'),
('weapon:update', 'Update weapons', 'Update weapon records'),
('building:read', 'Read buildings', 'Read building records'),
('building:update', 'Update buildings', 'Update building records'),
('specialty:read', 'Read specialties', 'Read specialty records'),
('query:execute', 'Execute intelligence query', 'Execute Intelligence Query Terminal templates'),
('alert:read', 'Read alerts', 'Read alert center'),
('alert:update', 'Update alerts', 'Acknowledge or resolve alerts'),
('report:read', 'Read reports', 'Read tactical reports'),
('report:generate', 'Generate reports', 'Generate tactical reports'),
('user:manage', 'Manage users', 'Manage users, roles and assignments'),
('audit:read', 'Read audit events', 'Read audit log'),
('commander:assign', 'Assign commander', 'Assign commanders')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'ADMIN_DISTRICT'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:read', 'structure:read', 'personnel:read', 'unit:read',
    'equipment:read', 'weapon:read', 'building:read', 'specialty:read',
    'query:execute', 'alert:read', 'report:read', 'audit:read'
)
WHERE r.code = 'STAFF_ANALYST'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:read', 'structure:read', 'personnel:read', 'personnel:update',
    'unit:read', 'unit:update', 'equipment:read', 'equipment:update',
    'weapon:read', 'weapon:update', 'building:read', 'building:update',
    'specialty:read', 'query:execute', 'alert:read', 'alert:update',
    'report:read', 'report:generate', 'commander:assign'
)
WHERE r.code IN ('ARMY_COMMANDER', 'FORMATION_COMMANDER', 'UNIT_COMMANDER')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.code IN (
    'dashboard:read', 'structure:read', 'personnel:read', 'personnel:update',
    'unit:read', 'equipment:read', 'weapon:read', 'building:read',
    'specialty:read', 'alert:read', 'report:read'
)
WHERE r.code IN ('COMPANY_COMMANDER', 'PLATOON_COMMANDER', 'SQUAD_COMMANDER')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM roles r
JOIN permissions p ON p.code IN ('personnel:read', 'specialty:read')
WHERE r.code = 'SOLDIER'
ON CONFLICT DO NOTHING;

-- Password for all demo users: password
INSERT INTO users (username, password_hash, display_name, personnel_id, is_active) VALUES
('admin.district', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'District System Admin', 1, TRUE),
('analyst.staff', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Operational Staff Analyst', 2, TRUE),
('army.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Army Commander 1', 3, TRUE),
('brigade.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Brigade Commander 1', 4, TRUE),
('unit.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Military Unit Commander 1', 5, TRUE),
('company.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Company Commander 1', 6, TRUE),
('platoon.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Platoon Commander 1', 7, TRUE),
('squad.cmd.1', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Squad Commander 1', 8, TRUE),
('soldier.demo', '$2a$10$6/8diJojGYzPtOVCN9.Bn.u1Hh14wE8BgPrG.pme.IoYSIlqzeWMu', 'Demo Soldier', 9, TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u
JOIN roles r ON (
    (u.username = 'admin.district' AND r.code = 'ADMIN_DISTRICT')
    OR (u.username = 'analyst.staff' AND r.code = 'STAFF_ANALYST')
    OR (u.username = 'army.cmd.1' AND r.code = 'ARMY_COMMANDER')
    OR (u.username = 'brigade.cmd.1' AND r.code = 'FORMATION_COMMANDER')
    OR (u.username = 'unit.cmd.1' AND r.code = 'UNIT_COMMANDER')
    OR (u.username = 'company.cmd.1' AND r.code = 'COMPANY_COMMANDER')
    OR (u.username = 'platoon.cmd.1' AND r.code = 'PLATOON_COMMANDER')
    OR (u.username = 'squad.cmd.1' AND r.code = 'SQUAD_COMMANDER')
    OR (u.username = 'soldier.demo' AND r.code = 'SOLDIER')
)
ON CONFLICT DO NOTHING;

INSERT INTO command_assignments (soldier_id, object_type, object_id, is_primary)
VALUES
(1, 'DISTRICT', 1, TRUE),
(2, 'DISTRICT', 1, TRUE),
(3, 'ARMY', 2, TRUE),
(4, 'BRIGADE', 5, TRUE),
(5, 'MILITARY_UNIT', 1, TRUE),
(6, 'COMPANY', 10, TRUE),
(7, 'PLATOON', 11, TRUE),
(8, 'SQUAD', 12, TRUE),
(9, 'SELF', 9, TRUE);
