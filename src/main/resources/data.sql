-- Initial roles (use INSERT IGNORE to avoid duplicates if table exists)
INSERT IGNORE INTO role (role_name) VALUES ('RENTER'), ('PROVIDER'), ('ADMIN');

-- Create default admin user
-- Username: Admin, Password: Password
INSERT IGNORE INTO user (user_id, username, password, first_name, last_name, phone_number, city, state) 
VALUES (1, 'Admin', '$2a$10$CotBcdDIGzn8sXDmYfZXZ.MkUaXrC2BTdlM1RJaadr28/.S8DeCUG', 'Admin', 'System', '000-0000', 'San Jose', 'CA');

-- Create MockRenter user
INSERT IGNORE INTO user (user_id, username, password, first_name, last_name, phone_number, city, state) 
VALUES (2, 'MockRenter', '$2a$10$CotBcdDIGzn8sXDmYfZXZ.MkUaXrC2BTdlM1RJaadr28/.S8DeCUG', 'Mock', 'Renter', '111-1111', 'San Jose', 'CA');

-- Create MockProvider user
INSERT IGNORE INTO user (user_id, username, password, first_name, last_name, phone_number, city, state) 
VALUES (3, 'MockProvider', '$2a$10$CotBcdDIGzn8sXDmYfZXZ.MkUaXrC2BTdlM1RJaadr28/.S8DeCUG', 'Mock', 'Provider', '222-2222', 'San Francisco', 'CA');

-- Assign roles
INSERT IGNORE INTO user_role (user_id, role_id) VALUES (1, (SELECT role_id FROM role WHERE role_name = 'ADMIN'));
INSERT IGNORE INTO user_role (user_id, role_id) VALUES (1, (SELECT role_id FROM role WHERE role_name = 'RENTER'));
INSERT IGNORE INTO user_role (user_id, role_id) VALUES (1, (SELECT role_id FROM role WHERE role_name = 'PROVIDER'));

INSERT IGNORE INTO user_role (user_id, role_id) VALUES (2, (SELECT role_id FROM role WHERE role_name = 'RENTER'));

INSERT IGNORE INTO user_role (user_id, role_id) VALUES (3, (SELECT role_id FROM role WHERE role_name = 'PROVIDER'));

-- Register in "is-a" tables
INSERT IGNORE INTO renter (user_id) VALUES (1);
INSERT IGNORE INTO provider (user_id) VALUES (1);
INSERT IGNORE INTO renter (user_id) VALUES (2);
INSERT IGNORE INTO provider (user_id) VALUES (3);

-- Create some sample equipment for the marketplace (Owned by Admin/Provider)
INSERT IGNORE INTO equipment (equipment_id, provider_id, equipment_name, description, hourly_rate, is_deactivated, is_reported) 
VALUES (1, 1, 'Sony A7 III Camera', 'Full-frame mirrorless camera, great for video and stills.', 25.00, false, false);

INSERT IGNORE INTO equipment (equipment_id, provider_id, equipment_name, description, hourly_rate, is_deactivated, is_reported) 
VALUES (2, 1, 'Epson 4K Projector', '3000 lumens, perfect for outdoor movie nights.', 15.00, false, false);

-- Equipment owned by MockProvider
INSERT IGNORE INTO equipment (equipment_id, provider_id, equipment_name, description, hourly_rate, is_deactivated, is_reported) 
VALUES (3, 3, 'DJI Mavic Air 2', '4K drone with 34 mins flight time. Registration required.', 40.00, false, false);

-- A reported listing for Admin to see
INSERT IGNORE INTO equipment (equipment_id, provider_id, equipment_name, description, hourly_rate, is_deactivated, is_reported) 
VALUES (4, 3, 'Broken Drill', 'Barely works, making weird noises.', 5.00, false, true);

-- A deactivated listing
INSERT IGNORE INTO equipment (equipment_id, provider_id, equipment_name, description, hourly_rate, is_deactivated, is_reported) 
VALUES (5, 3, 'Old Laptop', 'Vintage collection piece.', 10.00, true, false);
