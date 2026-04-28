CREATE TABLE IF NOT EXISTS tenant_default_seed_runs (
  id bigserial PRIMARY KEY,
  company_id bigint NOT NULL,
  run_id varchar(64) NOT NULL,
  category varchar(64) NOT NULL,
  status varchar(32) NOT NULL,
  operation varchar(32) NOT NULL,
  required boolean NOT NULL DEFAULT true,
  completed_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  version bigint NOT NULL DEFAULT 0,
  CONSTRAINT fk_tenant_default_seed_runs_company
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
  CONSTRAINT uk_tenant_default_seed_runs_company_category
    UNIQUE (company_id, category),
  CONSTRAINT uk_tenant_default_seed_runs_run_id
    UNIQUE (run_id),
  CONSTRAINT ck_tenant_default_seed_runs_status
    CHECK (status IN ('COMPLETE', 'REPAIR_REQUIRED')),
  CONSTRAINT ck_tenant_default_seed_runs_operation
    CHECK (operation IN ('SEEDED', 'REPAIRED', 'NOOP', 'PENDING_REPAIR'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_default_seed_runs_company
  ON tenant_default_seed_runs(company_id);

CREATE INDEX IF NOT EXISTS idx_tenant_default_seed_runs_status
  ON tenant_default_seed_runs(company_id, status);
