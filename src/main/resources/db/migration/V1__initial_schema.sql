CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TYPE user_role AS ENUM ('TECHNICIAN','SALON_OWNER','ADMIN');
CREATE TYPE payment_type AS ENUM ('W2','CONTRACT_1099','CASH_W2');
CREATE TYPE job_status AS ENUM ('DRAFT','ACTIVE','PAUSED','CLOSED','EXPIRED');
CREATE TYPE application_status AS ENUM ('APPLIED','VIEWED','CONTACTED','INTERVIEW','OFFER','HIRED','REJECTED','WITHDRAWN');

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(254) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role user_role NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE technician_profiles (
  user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  bio TEXT, years_experience INTEGER CHECK (years_experience >= 0),
  city VARCHAR(100), state VARCHAR(50), zip_code VARCHAR(12),
  location GEOGRAPHY(POINT,4326), desired_radius_miles INTEGER DEFAULT 25 CHECK (desired_radius_miles BETWEEN 1 AND 100),
  open_to_work BOOLEAN NOT NULL DEFAULT TRUE, recruiter_visible BOOLEAN NOT NULL DEFAULT TRUE,
  license_number VARCHAR(80), license_verified BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE skills (id BIGSERIAL PRIMARY KEY, name VARCHAR(80) NOT NULL UNIQUE);
CREATE TABLE technician_skills (technician_id UUID REFERENCES technician_profiles(user_id) ON DELETE CASCADE, skill_id BIGINT REFERENCES skills(id), PRIMARY KEY (technician_id,skill_id));

CREATE TABLE salons (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), owner_id UUID NOT NULL REFERENCES users(id),
  name VARCHAR(180) NOT NULL, description TEXT, address_line1 VARCHAR(180) NOT NULL,
  city VARCHAR(100) NOT NULL, state VARCHAR(50) NOT NULL, zip_code VARCHAR(12) NOT NULL,
  location GEOGRAPHY(POINT,4326) NOT NULL, phone VARCHAR(30), website VARCHAR(300), verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_salons_location ON salons USING GIST(location);

CREATE TABLE jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), salon_id UUID NOT NULL REFERENCES salons(id), created_by UUID NOT NULL REFERENCES users(id),
  title VARCHAR(180) NOT NULL, description TEXT NOT NULL, employment_type VARCHAR(40) NOT NULL,
  payment_type payment_type NOT NULL, compensation_min NUMERIC(10,2), compensation_max NUMERIC(10,2), compensation_unit VARCHAR(30), commission_percent NUMERIC(5,2),
  schedule VARCHAR(250), minimum_experience_years INTEGER DEFAULT 0 CHECK (minimum_experience_years >= 0), license_required BOOLEAN NOT NULL DEFAULT FALSE,
  status job_status NOT NULL DEFAULT 'DRAFT', expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE job_skills (job_id UUID REFERENCES jobs(id) ON DELETE CASCADE, skill_id BIGINT REFERENCES skills(id), PRIMARY KEY(job_id,skill_id));
CREATE INDEX idx_jobs_active_created ON jobs(status,created_at DESC);

CREATE TABLE applications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(), job_id UUID NOT NULL REFERENCES jobs(id), technician_id UUID NOT NULL REFERENCES users(id),
  message TEXT, status application_status NOT NULL DEFAULT 'APPLIED', created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(job_id,technician_id)
);
CREATE INDEX idx_applications_job ON applications(job_id,created_at DESC);

INSERT INTO skills(name) VALUES ('Acrylic'),('Gel X'),('Builder Gel'),('Dip Powder'),('Manicure'),('Pedicure'),('Nail Art') ON CONFLICT DO NOTHING;
