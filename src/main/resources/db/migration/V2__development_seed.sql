-- Development-only representative data. Remove this migration before production if preferred.
INSERT INTO users(id,email,password_hash,role,display_name,email_verified) VALUES
('11111111-1111-1111-1111-111111111111','owner@nailconnect.test','$2a$10$w8q1vBgt1JQXoYh2b2YgIu3hP.EUQnF9IoumbuiVFdStxfDIdX3eW','SALON_OWNER','Blush Atelier Owner',TRUE)
ON CONFLICT DO NOTHING;
INSERT INTO salons(id,owner_id,name,description,address_line1,city,state,zip_code,location,verified) VALUES
('22222222-2222-2222-2222-222222222222','11111111-1111-1111-1111-111111111111','Blush Nail Atelier','Modern appointment-first nail studio.','100 Wilson Blvd','Arlington','VA','22201',ST_SetSRID(ST_MakePoint(-77.1068,38.8799),4326)::geography,TRUE)
ON CONFLICT DO NOTHING;
