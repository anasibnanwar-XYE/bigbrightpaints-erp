-- Add GST input/output/payable account configuration to companies
ALTER TABLE companies
    ADD COLUMN gst_input_tax_account_id BIGINT,
    ADD COLUMN gst_output_tax_account_id BIGINT,
    ADD COLUMN gst_payable_account_id BIGINT;
