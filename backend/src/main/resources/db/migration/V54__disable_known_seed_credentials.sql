UPDATE users
SET active = 0
WHERE (username = 'hcm' AND password_hash = '$2a$10$Se9DjTVUDtzAHo2W/mf0JuG1bSMGHGhU8cvLnoUyW2PQqUc89oOa.')
   OR (username = 'admin' AND password_hash = '$2a$10$.23hPl3rkhCSFxCZ68G4I.ik80KwbH/KBGwACiSCofQbRgBp4S55i')
   OR (username = 'saidul' AND password_hash = '$2a$10$jCMBC3UjTWKzXGKm6A9exuF1B6vZuWeZ3uXULSyTvf4Yz6HWHHQxS')
   OR (username = 'jtsecy' AND password_hash = '$2a$10$rtaeYL1BT/S77IOU1Kx1puUHTyUbC5EdeEROyHK2agTtTNe4WluW.')
   OR (username = 'cmo' AND password_hash = '$2a$10$WYLxJIenXfAAIQdOe/UkIOFMyN79aqMew0h/BmkYKcP0yxpSvnUuK')
   OR (username = 'deo1' AND password_hash = '$2a$10$HkySIP9NjInkhzLE82XnTeVhMlTtz7b/LFypv8zVI6mkKHD0D9jK2');
