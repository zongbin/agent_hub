-- =====================================================================
-- 初始化管理员账号
-- 密码: admin123（BCrypt 哈希，cost factor 10）
-- 幂等：重复执行不报错
-- =====================================================================
INSERT INTO users (username, password, real_name, email, role, department, status, created_at, updated_at, deleted)
VALUES (
    'admin',
    '$2a$10$rhzKBRv1rJMA4n4KsTqey.BI.zaM9.Ir9Kqp2wMtHuT35VfmpcgjS',
    '系统管理员',
    'admin@company.com',
    'ADMIN',
    '管理部',
    1,
    NOW(),
    NOW(),
    0
)
ON CONFLICT (username) DO NOTHING;
