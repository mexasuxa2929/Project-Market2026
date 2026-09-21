-- superadmin2 test user: faqat ROLE_SUPER_ADMIN (ROLE_USER olib tashlanadi)
DELETE FROM user_roles
WHERE user_id = (SELECT id FROM users WHERE username = 'superadmin2');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
         CROSS JOIN roles r
WHERE u.username = 'superadmin2'
  AND r.name = 'ROLE_SUPER_ADMIN'
ON CONFLICT DO NOTHING;
