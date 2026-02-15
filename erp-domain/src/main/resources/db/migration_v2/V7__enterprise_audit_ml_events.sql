-- v2 baseline extension: enterprise audit action events + personalization consent.

ALTER TABLE public.app_users
    ADD COLUMN IF NOT EXISTS ai_personalization_opt_in boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS ai_personalization_updated_at timestamp with time zone;

CREATE TABLE IF NOT EXISTS public.audit_action_events (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    occurred_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source character varying(32) NOT NULL,
    module character varying(64) NOT NULL,
    action character varying(128) NOT NULL,
    entity_type character varying(128),
    entity_id character varying(128),
    reference_number character varying(128),
    status character varying(16) NOT NULL,
    failure_reason character varying(512),
    amount numeric(19,4),
    currency character varying(16),
    correlation_id uuid,
    request_id character varying(128),
    trace_id character varying(128),
    ip_address character varying(64),
    user_agent text,
    actor_user_id bigint,
    actor_identifier character varying(255) NOT NULL,
    actor_anonymized boolean NOT NULL DEFAULT false,
    ml_eligible boolean NOT NULL DEFAULT false,
    training_subject_key character varying(128),
    training_payload text,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0
);

CREATE SEQUENCE IF NOT EXISTS public.audit_action_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.audit_action_events_id_seq OWNED BY public.audit_action_events.id;
ALTER TABLE ONLY public.audit_action_events ALTER COLUMN id SET DEFAULT nextval('public.audit_action_events_id_seq'::regclass);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.audit_action_events'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE ONLY public.audit_action_events
            ADD CONSTRAINT audit_action_events_pkey PRIMARY KEY (id);
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS public.audit_action_event_metadata (
    event_id bigint NOT NULL,
    metadata_key character varying(128) NOT NULL,
    metadata_value text,
    PRIMARY KEY (event_id, metadata_key)
);

ALTER TABLE ONLY public.audit_action_events
    ADD CONSTRAINT fk_audit_action_events_company
    FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.audit_action_events
    ADD CONSTRAINT fk_audit_action_events_actor_user
    FOREIGN KEY (actor_user_id) REFERENCES public.app_users(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.audit_action_event_metadata
    ADD CONSTRAINT fk_audit_action_event_metadata_event
    FOREIGN KEY (event_id) REFERENCES public.audit_action_events(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_audit_action_events_company_occurred
    ON public.audit_action_events(company_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_events_company_module_action
    ON public.audit_action_events(company_id, module, action, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_events_company_status
    ON public.audit_action_events(company_id, status, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_events_company_reference
    ON public.audit_action_events(company_id, reference_number, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_events_company_actor_user
    ON public.audit_action_events(company_id, actor_user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_events_company_actor_identifier
    ON public.audit_action_events(company_id, actor_identifier, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_events_company_trace
    ON public.audit_action_events(company_id, trace_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS public.ml_interaction_events (
    id bigint NOT NULL,
    company_id bigint NOT NULL,
    occurred_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    module character varying(64) NOT NULL,
    action character varying(128) NOT NULL,
    interaction_type character varying(32),
    screen character varying(128),
    target_id character varying(256),
    status character varying(16) NOT NULL,
    failure_reason character varying(512),
    correlation_id uuid,
    request_id character varying(128),
    trace_id character varying(128),
    ip_address character varying(64),
    user_agent text,
    actor_user_id bigint,
    actor_identifier character varying(255) NOT NULL,
    actor_anonymized boolean NOT NULL DEFAULT false,
    consent_opt_in boolean NOT NULL DEFAULT false,
    training_subject_key character varying(128),
    payload text,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0
);

CREATE SEQUENCE IF NOT EXISTS public.ml_interaction_events_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.ml_interaction_events_id_seq OWNED BY public.ml_interaction_events.id;
ALTER TABLE ONLY public.ml_interaction_events ALTER COLUMN id SET DEFAULT nextval('public.ml_interaction_events_id_seq'::regclass);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.ml_interaction_events'::regclass
          AND contype = 'p'
    ) THEN
        ALTER TABLE ONLY public.ml_interaction_events
            ADD CONSTRAINT ml_interaction_events_pkey PRIMARY KEY (id);
    END IF;
END
$$;

CREATE TABLE IF NOT EXISTS public.ml_interaction_event_metadata (
    event_id bigint NOT NULL,
    metadata_key character varying(128) NOT NULL,
    metadata_value text,
    PRIMARY KEY (event_id, metadata_key)
);

ALTER TABLE ONLY public.ml_interaction_events
    ADD CONSTRAINT fk_ml_interaction_events_company
    FOREIGN KEY (company_id) REFERENCES public.companies(id) ON DELETE RESTRICT;

ALTER TABLE ONLY public.ml_interaction_events
    ADD CONSTRAINT fk_ml_interaction_events_actor_user
    FOREIGN KEY (actor_user_id) REFERENCES public.app_users(id) ON DELETE SET NULL;

ALTER TABLE ONLY public.ml_interaction_event_metadata
    ADD CONSTRAINT fk_ml_interaction_event_metadata_event
    FOREIGN KEY (event_id) REFERENCES public.ml_interaction_events(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_ml_interaction_events_company_occurred
    ON public.ml_interaction_events(company_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ml_interaction_events_company_actor_user
    ON public.ml_interaction_events(company_id, actor_user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ml_interaction_events_company_actor_identifier
    ON public.ml_interaction_events(company_id, actor_identifier, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ml_interaction_events_company_module_action
    ON public.ml_interaction_events(company_id, module, action, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_ml_interaction_events_company_trace
    ON public.ml_interaction_events(company_id, trace_id, occurred_at DESC);
