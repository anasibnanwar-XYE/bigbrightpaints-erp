-- Flyway v2: accounting baseline
-- Generated from canonical schema snapshot and grouped by domain phase.

-- TABLE: accounting_events
CREATE TABLE public.accounting_events (
    id bigint NOT NULL,
    event_id uuid NOT NULL,
    company_id bigint NOT NULL,
    event_type character varying(50) NOT NULL,
    aggregate_id uuid NOT NULL,
    aggregate_type character varying(50) NOT NULL,
    sequence_number bigint NOT NULL,
    event_timestamp timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    effective_date date NOT NULL,
    account_id bigint,
    account_code character varying(50),
    journal_entry_id bigint,
    journal_reference character varying(100),
    debit_amount numeric(19,4),
    credit_amount numeric(19,4),
    balance_before numeric(19,4),
    balance_after numeric(19,4),
    description character varying(500),
    user_id character varying(100),
    correlation_id uuid,
    payload text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- SEQUENCE: accounting_events_id_seq
CREATE SEQUENCE public.accounting_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: accounting_events_id_seq
ALTER SEQUENCE public.accounting_events_id_seq OWNED BY public.accounting_events.id;

-- TABLE: accounting_period_snapshots
CREATE TABLE public.accounting_period_snapshots (
    id bigint NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    company_id bigint NOT NULL,
    accounting_period_id bigint NOT NULL,
    as_of_date date NOT NULL,
    created_at timestamp with time zone NOT NULL,
    created_by character varying(255),
    trial_balance_total_debit numeric(19,2) DEFAULT 0 NOT NULL,
    trial_balance_total_credit numeric(19,2) DEFAULT 0 NOT NULL,
    inventory_total_value numeric(19,2) DEFAULT 0 NOT NULL,
    inventory_low_stock bigint DEFAULT 0 NOT NULL,
    ar_subledger_total numeric(19,2) DEFAULT 0 NOT NULL,
    ap_subledger_total numeric(19,2) DEFAULT 0 NOT NULL
);

-- SEQUENCE: accounting_period_snapshots_id_seq
CREATE SEQUENCE public.accounting_period_snapshots_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: accounting_period_snapshots_id_seq
ALTER SEQUENCE public.accounting_period_snapshots_id_seq OWNED BY public.accounting_period_snapshots.id;

-- TABLE: accounting_period_trial_balance_lines
CREATE TABLE public.accounting_period_trial_balance_lines (
    id bigint NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    snapshot_id bigint NOT NULL,
    account_id bigint,
    account_code character varying(50),
    account_name character varying(255),
    account_type character varying(50),
    debit numeric(19,2) DEFAULT 0 NOT NULL,
    credit numeric(19,2) DEFAULT 0 NOT NULL
);

-- SEQUENCE: accounting_period_trial_balance_lines_id_seq
CREATE SEQUENCE public.accounting_period_trial_balance_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: accounting_period_trial_balance_lines_id_seq
ALTER SEQUENCE public.accounting_period_trial_balance_lines_id_seq OWNED BY public.accounting_period_trial_balance_lines.id;

-- TABLE: accounting_periods
CREATE TABLE public.accounting_periods (
    id bigint NOT NULL,
    public_id uuid NOT NULL,
    company_id bigint NOT NULL,
    year integer NOT NULL,
    month integer NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    bank_reconciled boolean DEFAULT false NOT NULL,
    bank_reconciled_at timestamp without time zone,
    bank_reconciled_by character varying(255),
    inventory_counted boolean DEFAULT false NOT NULL,
    inventory_counted_at timestamp without time zone,
    inventory_counted_by character varying(255),
    checklist_notes text,
    closed_at timestamp without time zone,
    closed_by character varying(255),
    version integer DEFAULT 0 NOT NULL,
    locked_at timestamp with time zone,
    locked_by character varying(128),
    lock_reason text,
    reopened_at timestamp with time zone,
    reopened_by character varying(128),
    reopen_reason text,
    closing_journal_entry_id bigint,
    CONSTRAINT accounting_periods_month_check CHECK (((month >= 1) AND (month <= 12)))
);

-- SEQUENCE: accounting_periods_id_seq
CREATE SEQUENCE public.accounting_periods_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: accounting_periods_id_seq
ALTER SEQUENCE public.accounting_periods_id_seq OWNED BY public.accounting_periods.id;

-- TABLE: accounts
CREATE TABLE public.accounts (
    id bigint NOT NULL,
    public_id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    code character varying(64) NOT NULL,
    name character varying(255) NOT NULL,
    type character varying(32) NOT NULL,
    balance numeric(18,2) DEFAULT 0 NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    active boolean DEFAULT true NOT NULL,
    parent_id bigint,
    hierarchy_level integer DEFAULT 1
);

-- SEQUENCE: accounts_id_seq
CREATE SEQUENCE public.accounts_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: accounts_id_seq
ALTER SEQUENCE public.accounts_id_seq OWNED BY public.accounts.id;

-- TABLE: dealer_ledger_entries
CREATE TABLE public.dealer_ledger_entries (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    dealer_id bigint NOT NULL,
    journal_entry_id bigint,
    entry_date date NOT NULL,
    reference_number character varying(64) NOT NULL,
    memo text,
    debit numeric(18,2) DEFAULT 0 NOT NULL,
    credit numeric(18,2) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    due_date date,
    paid_date date,
    invoice_number character varying(100),
    payment_status character varying(20) DEFAULT 'UNPAID'::character varying,
    amount_paid numeric(19,4) DEFAULT 0
);

-- SEQUENCE: dealer_ledger_entries_id_seq
CREATE SEQUENCE public.dealer_ledger_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: dealer_ledger_entries_id_seq
ALTER SEQUENCE public.dealer_ledger_entries_id_seq OWNED BY public.dealer_ledger_entries.id;

-- TABLE: journal_entries
CREATE TABLE public.journal_entries (
    id bigint NOT NULL,
    public_id uuid DEFAULT gen_random_uuid() NOT NULL,
    company_id bigint NOT NULL,
    reference_number character varying(64) NOT NULL,
    memo text,
    status character varying(32) DEFAULT 'DRAFT'::character varying NOT NULL,
    entry_date date NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    dealer_id bigint,
    supplier_id bigint,
    version bigint DEFAULT 0 NOT NULL,
    accounting_period_id bigint,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    posted_at timestamp with time zone,
    created_by character varying(255),
    posted_by character varying(255),
    last_modified_by character varying(255),
    void_reason text,
    voided_at timestamp with time zone,
    correction_type character varying(32),
    correction_reason text,
    reversal_of_id bigint,
    currency character varying(8) DEFAULT 'INR'::character varying NOT NULL,
    foreign_amount_total numeric(18,2),
    fx_rate numeric(19,6),
    CONSTRAINT chk_fx_rate_positive CHECK ((fx_rate > (0)::numeric))
);

-- SEQUENCE: journal_entries_id_seq
CREATE SEQUENCE public.journal_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: journal_entries_id_seq
ALTER SEQUENCE public.journal_entries_id_seq OWNED BY public.journal_entries.id;

-- TABLE: journal_lines
CREATE TABLE public.journal_lines (
    id bigint NOT NULL,
    journal_entry_id bigint NOT NULL,
    account_id bigint NOT NULL,
    description text,
    debit numeric(18,2) DEFAULT 0 NOT NULL,
    credit numeric(18,2) DEFAULT 0 NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);

-- SEQUENCE: journal_lines_id_seq
CREATE SEQUENCE public.journal_lines_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: journal_lines_id_seq
ALTER SEQUENCE public.journal_lines_id_seq OWNED BY public.journal_lines.id;

-- TABLE: journal_reference_mappings
CREATE TABLE public.journal_reference_mappings (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    legacy_reference character varying(64) NOT NULL,
    canonical_reference character varying(64) NOT NULL,
    entity_type character varying(64),
    entity_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

-- SEQUENCE: journal_reference_mappings_id_seq
CREATE SEQUENCE public.journal_reference_mappings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: journal_reference_mappings_id_seq
ALTER SEQUENCE public.journal_reference_mappings_id_seq OWNED BY public.journal_reference_mappings.id;

-- TABLE: partner_settlement_allocations
CREATE TABLE public.partner_settlement_allocations (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    partner_type character varying(16) NOT NULL,
    dealer_id bigint,
    supplier_id bigint,
    invoice_id bigint,
    purchase_id bigint,
    journal_entry_id bigint NOT NULL,
    settlement_date date NOT NULL,
    allocation_amount numeric(18,2) DEFAULT 0 NOT NULL,
    discount_amount numeric(18,2) DEFAULT 0 NOT NULL,
    write_off_amount numeric(18,2) DEFAULT 0 NOT NULL,
    fx_difference_amount numeric(18,2) DEFAULT 0 NOT NULL,
    currency character varying(16) DEFAULT 'INR'::character varying NOT NULL,
    memo text,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    idempotency_key character varying(128),
    CONSTRAINT chk_partner_settlement_partner CHECK (((((partner_type)::text = 'DEALER'::text) AND (dealer_id IS NOT NULL) AND (supplier_id IS NULL)) OR (((partner_type)::text = 'SUPPLIER'::text) AND (supplier_id IS NOT NULL) AND (dealer_id IS NULL))))
);

-- SEQUENCE: partner_settlement_allocations_id_seq
CREATE SEQUENCE public.partner_settlement_allocations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: partner_settlement_allocations_id_seq
ALTER SEQUENCE public.partner_settlement_allocations_id_seq OWNED BY public.partner_settlement_allocations.id;

-- TABLE: supplier_ledger_entries
CREATE TABLE public.supplier_ledger_entries (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    supplier_id bigint NOT NULL,
    journal_entry_id bigint,
    entry_date date NOT NULL,
    reference_number character varying(128) NOT NULL,
    memo text,
    debit numeric(18,2) DEFAULT 0 NOT NULL,
    credit numeric(18,2) DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    due_date date,
    paid_date date,
    invoice_number character varying(100),
    payment_status character varying(20) DEFAULT 'UNPAID'::character varying,
    amount_paid numeric(19,4) DEFAULT 0
);

-- SEQUENCE: supplier_ledger_entries_id_seq
CREATE SEQUENCE public.supplier_ledger_entries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- SEQUENCE OWNED BY: supplier_ledger_entries_id_seq
ALTER SEQUENCE public.supplier_ledger_entries_id_seq OWNED BY public.supplier_ledger_entries.id;

-- DEFAULT: accounting_events id
ALTER TABLE ONLY public.accounting_events ALTER COLUMN id SET DEFAULT nextval('public.accounting_events_id_seq'::regclass);

-- DEFAULT: accounting_period_snapshots id
ALTER TABLE ONLY public.accounting_period_snapshots ALTER COLUMN id SET DEFAULT nextval('public.accounting_period_snapshots_id_seq'::regclass);

-- DEFAULT: accounting_period_trial_balance_lines id
ALTER TABLE ONLY public.accounting_period_trial_balance_lines ALTER COLUMN id SET DEFAULT nextval('public.accounting_period_trial_balance_lines_id_seq'::regclass);

-- DEFAULT: accounting_periods id
ALTER TABLE ONLY public.accounting_periods ALTER COLUMN id SET DEFAULT nextval('public.accounting_periods_id_seq'::regclass);

-- DEFAULT: accounts id
ALTER TABLE ONLY public.accounts ALTER COLUMN id SET DEFAULT nextval('public.accounts_id_seq'::regclass);

-- DEFAULT: dealer_ledger_entries id
ALTER TABLE ONLY public.dealer_ledger_entries ALTER COLUMN id SET DEFAULT nextval('public.dealer_ledger_entries_id_seq'::regclass);

-- DEFAULT: journal_entries id
ALTER TABLE ONLY public.journal_entries ALTER COLUMN id SET DEFAULT nextval('public.journal_entries_id_seq'::regclass);

-- DEFAULT: journal_lines id
ALTER TABLE ONLY public.journal_lines ALTER COLUMN id SET DEFAULT nextval('public.journal_lines_id_seq'::regclass);

-- DEFAULT: journal_reference_mappings id
ALTER TABLE ONLY public.journal_reference_mappings ALTER COLUMN id SET DEFAULT nextval('public.journal_reference_mappings_id_seq'::regclass);

-- DEFAULT: partner_settlement_allocations id
ALTER TABLE ONLY public.partner_settlement_allocations ALTER COLUMN id SET DEFAULT nextval('public.partner_settlement_allocations_id_seq'::regclass);

-- DEFAULT: supplier_ledger_entries id
ALTER TABLE ONLY public.supplier_ledger_entries ALTER COLUMN id SET DEFAULT nextval('public.supplier_ledger_entries_id_seq'::regclass);

-- CONSTRAINT: accounting_events accounting_events_event_id_key
ALTER TABLE ONLY public.accounting_events
    ADD CONSTRAINT accounting_events_event_id_key UNIQUE (event_id);

-- CONSTRAINT: accounting_events accounting_events_pkey
ALTER TABLE ONLY public.accounting_events
    ADD CONSTRAINT accounting_events_pkey PRIMARY KEY (id);

-- CONSTRAINT: accounting_period_snapshots accounting_period_snapshots_pkey
ALTER TABLE ONLY public.accounting_period_snapshots
    ADD CONSTRAINT accounting_period_snapshots_pkey PRIMARY KEY (id);

-- CONSTRAINT: accounting_period_trial_balance_lines accounting_period_trial_balance_lines_pkey
ALTER TABLE ONLY public.accounting_period_trial_balance_lines
    ADD CONSTRAINT accounting_period_trial_balance_lines_pkey PRIMARY KEY (id);

-- CONSTRAINT: accounting_periods accounting_periods_pkey
ALTER TABLE ONLY public.accounting_periods
    ADD CONSTRAINT accounting_periods_pkey PRIMARY KEY (id);

-- CONSTRAINT: accounting_periods accounting_periods_public_id_key
ALTER TABLE ONLY public.accounting_periods
    ADD CONSTRAINT accounting_periods_public_id_key UNIQUE (public_id);

-- CONSTRAINT: accounts accounts_company_id_code_key
ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_company_id_code_key UNIQUE (company_id, code);

-- CONSTRAINT: accounts accounts_pkey
ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);

-- CONSTRAINT: dealer_ledger_entries dealer_ledger_entries_pkey
ALTER TABLE ONLY public.dealer_ledger_entries
    ADD CONSTRAINT dealer_ledger_entries_pkey PRIMARY KEY (id);

-- CONSTRAINT: journal_entries journal_entries_pkey
ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_pkey PRIMARY KEY (id);

-- CONSTRAINT: journal_lines journal_lines_pkey
ALTER TABLE ONLY public.journal_lines
    ADD CONSTRAINT journal_lines_pkey PRIMARY KEY (id);

-- CONSTRAINT: journal_reference_mappings journal_reference_mappings_pkey
ALTER TABLE ONLY public.journal_reference_mappings
    ADD CONSTRAINT journal_reference_mappings_pkey PRIMARY KEY (id);

-- CONSTRAINT: partner_settlement_allocations partner_settlement_allocations_pkey
ALTER TABLE ONLY public.partner_settlement_allocations
    ADD CONSTRAINT partner_settlement_allocations_pkey PRIMARY KEY (id);

-- CONSTRAINT: supplier_ledger_entries supplier_ledger_entries_pkey
ALTER TABLE ONLY public.supplier_ledger_entries
    ADD CONSTRAINT supplier_ledger_entries_pkey PRIMARY KEY (id);

-- CONSTRAINT: accounting_events uk_aggregate_sequence
ALTER TABLE ONLY public.accounting_events
    ADD CONSTRAINT uk_aggregate_sequence UNIQUE (aggregate_id, sequence_number);

-- CONSTRAINT: accounting_periods uk_company_year_month
ALTER TABLE ONLY public.accounting_periods
    ADD CONSTRAINT uk_company_year_month UNIQUE (company_id, year, month);

-- CONSTRAINT: journal_entries uk_journal_company_reference
ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT uk_journal_company_reference UNIQUE (company_id, reference_number);

-- CONSTRAINT: accounting_period_snapshots uq_accounting_period_snapshot
ALTER TABLE ONLY public.accounting_period_snapshots
    ADD CONSTRAINT uq_accounting_period_snapshot UNIQUE (company_id, accounting_period_id);

-- INDEX: idx_accounting_period_snapshots_company
CREATE INDEX idx_accounting_period_snapshots_company ON public.accounting_period_snapshots USING btree (company_id);

-- INDEX: idx_accounting_period_snapshots_period
CREATE INDEX idx_accounting_period_snapshots_period ON public.accounting_period_snapshots USING btree (accounting_period_id);

-- INDEX: idx_accounting_period_trial_balance_account
CREATE INDEX idx_accounting_period_trial_balance_account ON public.accounting_period_trial_balance_lines USING btree (account_id);

-- INDEX: idx_accounting_period_trial_balance_snapshot
CREATE INDEX idx_accounting_period_trial_balance_snapshot ON public.accounting_period_trial_balance_lines USING btree (snapshot_id);

-- INDEX: idx_accounting_periods_company
CREATE INDEX idx_accounting_periods_company ON public.accounting_periods USING btree (company_id);

-- INDEX: idx_accounting_periods_date_range
CREATE INDEX idx_accounting_periods_date_range ON public.accounting_periods USING btree (start_date, end_date);

-- INDEX: idx_accounting_periods_status
CREATE INDEX idx_accounting_periods_status ON public.accounting_periods USING btree (status);

-- INDEX: idx_accounts_active
CREATE INDEX idx_accounts_active ON public.accounts USING btree (active);

-- INDEX: idx_accounts_hierarchy
CREATE INDEX idx_accounts_hierarchy ON public.accounts USING btree (company_id, hierarchy_level, code);

-- INDEX: idx_accounts_parent
CREATE INDEX idx_accounts_parent ON public.accounts USING btree (parent_id);

-- INDEX: idx_acct_events_account
CREATE INDEX idx_acct_events_account ON public.accounting_events USING btree (account_id, event_timestamp);

-- INDEX: idx_acct_events_account_date
CREATE INDEX idx_acct_events_account_date ON public.accounting_events USING btree (account_id, effective_date);

-- INDEX: idx_acct_events_company_account_effective_ts
CREATE INDEX idx_acct_events_company_account_effective_ts ON public.accounting_events USING btree (company_id, account_id, effective_date, event_timestamp, sequence_number);

-- INDEX: idx_acct_events_company_ts
CREATE INDEX idx_acct_events_company_ts ON public.accounting_events USING btree (company_id, event_timestamp);

-- INDEX: idx_acct_events_correlation
CREATE INDEX idx_acct_events_correlation ON public.accounting_events USING btree (correlation_id);

-- INDEX: idx_acct_events_journal
CREATE INDEX idx_acct_events_journal ON public.accounting_events USING btree (journal_entry_id);

-- INDEX: idx_acct_events_type
CREATE INDEX idx_acct_events_type ON public.accounting_events USING btree (company_id, event_type);

-- INDEX: idx_dealer_ledger_company
CREATE INDEX idx_dealer_ledger_company ON public.dealer_ledger_entries USING btree (company_id);

-- INDEX: idx_dealer_ledger_company_dealer_date
CREATE INDEX idx_dealer_ledger_company_dealer_date ON public.dealer_ledger_entries USING btree (company_id, dealer_id, entry_date);

-- INDEX: idx_dealer_ledger_company_status_dealer_due
CREATE INDEX idx_dealer_ledger_company_status_dealer_due ON public.dealer_ledger_entries USING btree (company_id, payment_status, dealer_id, due_date);

-- INDEX: idx_dealer_ledger_dealer
CREATE INDEX idx_dealer_ledger_dealer ON public.dealer_ledger_entries USING btree (dealer_id);

-- INDEX: idx_dealer_ledger_due_date
CREATE INDEX idx_dealer_ledger_due_date ON public.dealer_ledger_entries USING btree (company_id, due_date);

-- INDEX: idx_dealer_ledger_entries_dealer
CREATE INDEX idx_dealer_ledger_entries_dealer ON public.dealer_ledger_entries USING btree (dealer_id);

-- INDEX: idx_dealer_ledger_invoice
CREATE INDEX idx_dealer_ledger_invoice ON public.dealer_ledger_entries USING btree (company_id, invoice_number);

-- INDEX: idx_dealer_ledger_journal_entry
CREATE INDEX idx_dealer_ledger_journal_entry ON public.dealer_ledger_entries USING btree (journal_entry_id);

-- INDEX: idx_dealer_ledger_status
CREATE INDEX idx_dealer_ledger_status ON public.dealer_ledger_entries USING btree (company_id, payment_status);

-- INDEX: idx_journal_company_date_status
CREATE INDEX idx_journal_company_date_status ON public.journal_entries USING btree (company_id, entry_date DESC, status);

-- INDEX: idx_journal_company_dealer_date
CREATE INDEX idx_journal_company_dealer_date ON public.journal_entries USING btree (company_id, dealer_id, entry_date DESC);

-- INDEX: idx_journal_entries_dealer
CREATE INDEX idx_journal_entries_dealer ON public.journal_entries USING btree (dealer_id);

-- INDEX: idx_journal_entries_period
CREATE INDEX idx_journal_entries_period ON public.journal_entries USING btree (accounting_period_id);

-- INDEX: idx_journal_entries_reversal
CREATE INDEX idx_journal_entries_reversal ON public.journal_entries USING btree (reversal_of_id);

-- INDEX: idx_journal_entries_supplier
CREATE INDEX idx_journal_entries_supplier ON public.journal_entries USING btree (supplier_id);

-- INDEX: idx_journal_lines_account_id
CREATE INDEX idx_journal_lines_account_id ON public.journal_lines USING btree (account_id);

-- INDEX: idx_journal_reference_mapping_canonical
CREATE INDEX idx_journal_reference_mapping_canonical ON public.journal_reference_mappings USING btree (company_id, canonical_reference);

-- INDEX: idx_partner_settlement_company
CREATE INDEX idx_partner_settlement_company ON public.partner_settlement_allocations USING btree (company_id);

-- INDEX: idx_partner_settlement_idempotency
CREATE INDEX idx_partner_settlement_idempotency ON public.partner_settlement_allocations USING btree (company_id, idempotency_key) WHERE (idempotency_key IS NOT NULL);

-- INDEX: idx_partner_settlement_invoice
CREATE INDEX idx_partner_settlement_invoice ON public.partner_settlement_allocations USING btree (company_id, invoice_id);

-- INDEX: idx_partner_settlement_partner
CREATE INDEX idx_partner_settlement_partner ON public.partner_settlement_allocations USING btree (company_id, partner_type, dealer_id, supplier_id);

-- INDEX: idx_partner_settlement_purchase
CREATE INDEX idx_partner_settlement_purchase ON public.partner_settlement_allocations USING btree (company_id, purchase_id);

-- INDEX: idx_supplier_ledger_company
CREATE INDEX idx_supplier_ledger_company ON public.supplier_ledger_entries USING btree (company_id);

-- INDEX: idx_supplier_ledger_company_supplier_date
CREATE INDEX idx_supplier_ledger_company_supplier_date ON public.supplier_ledger_entries USING btree (company_id, supplier_id, entry_date);

-- INDEX: idx_supplier_ledger_due_date
CREATE INDEX idx_supplier_ledger_due_date ON public.supplier_ledger_entries USING btree (company_id, due_date);

-- INDEX: idx_supplier_ledger_entries_supplier
CREATE INDEX idx_supplier_ledger_entries_supplier ON public.supplier_ledger_entries USING btree (supplier_id);

-- INDEX: idx_supplier_ledger_journal_entry
CREATE INDEX idx_supplier_ledger_journal_entry ON public.supplier_ledger_entries USING btree (journal_entry_id);

-- INDEX: idx_supplier_ledger_status
CREATE INDEX idx_supplier_ledger_status ON public.supplier_ledger_entries USING btree (company_id, payment_status);

-- INDEX: idx_supplier_ledger_supplier
CREATE INDEX idx_supplier_ledger_supplier ON public.supplier_ledger_entries USING btree (supplier_id);

-- INDEX: uk_partner_settlement_idem_invoice
CREATE UNIQUE INDEX uk_partner_settlement_idem_invoice ON public.partner_settlement_allocations USING btree (company_id, idempotency_key, invoice_id) WHERE ((idempotency_key IS NOT NULL) AND (invoice_id IS NOT NULL));

-- INDEX: uk_partner_settlement_idem_purchase
CREATE UNIQUE INDEX uk_partner_settlement_idem_purchase ON public.partner_settlement_allocations USING btree (company_id, idempotency_key, purchase_id) WHERE ((idempotency_key IS NOT NULL) AND (purchase_id IS NOT NULL));

-- INDEX: uq_journal_reference_mapping_legacy
CREATE UNIQUE INDEX uq_journal_reference_mapping_legacy ON public.journal_reference_mappings USING btree (company_id, legacy_reference);

-- FK CONSTRAINT: accounting_events accounting_events_company_id_fkey
ALTER TABLE ONLY public.accounting_events
    ADD CONSTRAINT accounting_events_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);

-- FK CONSTRAINT: accounting_period_snapshots accounting_period_snapshots_accounting_period_id_fkey
ALTER TABLE ONLY public.accounting_period_snapshots
    ADD CONSTRAINT accounting_period_snapshots_accounting_period_id_fkey FOREIGN KEY (accounting_period_id) REFERENCES public.accounting_periods(id);

-- FK CONSTRAINT: accounting_period_snapshots accounting_period_snapshots_company_id_fkey
ALTER TABLE ONLY public.accounting_period_snapshots
    ADD CONSTRAINT accounting_period_snapshots_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);

-- FK CONSTRAINT: accounting_period_trial_balance_lines accounting_period_trial_balance_lines_snapshot_id_fkey
ALTER TABLE ONLY public.accounting_period_trial_balance_lines
    ADD CONSTRAINT accounting_period_trial_balance_lines_snapshot_id_fkey FOREIGN KEY (snapshot_id) REFERENCES public.accounting_period_snapshots(id) ON DELETE CASCADE;

-- FK CONSTRAINT: accounting_periods accounting_periods_closing_journal_entry_id_fkey
ALTER TABLE ONLY public.accounting_periods
    ADD CONSTRAINT accounting_periods_closing_journal_entry_id_fkey FOREIGN KEY (closing_journal_entry_id) REFERENCES public.journal_entries(id);

-- FK CONSTRAINT: accounting_periods accounting_periods_company_id_fkey
ALTER TABLE ONLY public.accounting_periods
    ADD CONSTRAINT accounting_periods_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id);

-- FK CONSTRAINT: accounts accounts_company_id_fkey
ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;

-- FK CONSTRAINT: accounts accounts_parent_id_fkey
ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.accounts(id);

-- FK CONSTRAINT: dealer_ledger_entries dealer_ledger_entries_company_id_fkey
ALTER TABLE ONLY public.dealer_ledger_entries
    ADD CONSTRAINT dealer_ledger_entries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;

-- FK CONSTRAINT: dealer_ledger_entries dealer_ledger_entries_journal_entry_id_fkey
ALTER TABLE ONLY public.dealer_ledger_entries
    ADD CONSTRAINT dealer_ledger_entries_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id) ON DELETE SET NULL;

-- FK CONSTRAINT: companies fk_company_payroll_cash_account
ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fk_company_payroll_cash_account FOREIGN KEY (payroll_cash_account_id) REFERENCES public.accounts(id);

-- FK CONSTRAINT: companies fk_company_payroll_expense_account
ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fk_company_payroll_expense_account FOREIGN KEY (payroll_expense_account_id) REFERENCES public.accounts(id);

-- FK CONSTRAINT: journal_entries journal_entries_accounting_period_id_fkey
ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_accounting_period_id_fkey FOREIGN KEY (accounting_period_id) REFERENCES public.accounting_periods(id);

-- FK CONSTRAINT: journal_entries journal_entries_company_id_fkey
ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;

-- FK CONSTRAINT: journal_entries journal_entries_reversal_of_id_fkey
ALTER TABLE ONLY public.journal_entries
    ADD CONSTRAINT journal_entries_reversal_of_id_fkey FOREIGN KEY (reversal_of_id) REFERENCES public.journal_entries(id) ON DELETE SET NULL;

-- FK CONSTRAINT: journal_lines journal_lines_account_id_fkey
ALTER TABLE ONLY public.journal_lines
    ADD CONSTRAINT journal_lines_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id) ON DELETE RESTRICT;

-- FK CONSTRAINT: journal_lines journal_lines_journal_entry_id_fkey
ALTER TABLE ONLY public.journal_lines
    ADD CONSTRAINT journal_lines_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id) ON DELETE CASCADE;

-- FK CONSTRAINT: journal_reference_mappings journal_reference_mappings_company_id_fkey
ALTER TABLE ONLY public.journal_reference_mappings
    ADD CONSTRAINT journal_reference_mappings_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;

-- FK CONSTRAINT: partner_settlement_allocations partner_settlement_allocations_company_id_fkey
ALTER TABLE ONLY public.partner_settlement_allocations
    ADD CONSTRAINT partner_settlement_allocations_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;

-- FK CONSTRAINT: partner_settlement_allocations partner_settlement_allocations_journal_entry_id_fkey
ALTER TABLE ONLY public.partner_settlement_allocations
    ADD CONSTRAINT partner_settlement_allocations_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id) ON DELETE CASCADE;

-- FK CONSTRAINT: supplier_ledger_entries supplier_ledger_entries_company_id_fkey
ALTER TABLE ONLY public.supplier_ledger_entries
    ADD CONSTRAINT supplier_ledger_entries_company_id_fkey FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE CASCADE;

-- FK CONSTRAINT: supplier_ledger_entries supplier_ledger_entries_journal_entry_id_fkey
ALTER TABLE ONLY public.supplier_ledger_entries
    ADD CONSTRAINT supplier_ledger_entries_journal_entry_id_fkey FOREIGN KEY (journal_entry_id) REFERENCES public.journal_entries(id) ON DELETE SET NULL;
