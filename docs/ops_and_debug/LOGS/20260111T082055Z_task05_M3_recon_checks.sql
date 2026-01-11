-- Task 05 M3 reconciliation evidence checks
\echo '1) Orphan journals (no lines)'
select je.id, je.reference_number, je.entry_date
from journal_entries je
left join journal_lines jl on jl.journal_entry_id = je.id
where jl.id is null
order by je.id;

\echo '2) Orphan raw material movements (purchase/return)'
select count(*) as missing_journal
from raw_material_movements m
join raw_materials r on r.id = m.raw_material_id
where r.company_id = :company_id
  and m.reference_type in ('RAW_MATERIAL_PURCHASE', 'PURCHASE_RETURN')
  and m.journal_entry_id is null;

\echo '3) Orphan inventory movements (adjustments)'
select count(*) as missing_journal
from inventory_movements m
join finished_goods f on f.id = m.finished_good_id
where f.company_id = :company_id
  and m.reference_type = 'ADJUSTMENT'
  and m.journal_entry_id is null;

\echo '4) AR control vs dealer ledger tie-out (as-of)'
with ar_control as (
  select coalesce(sum(balance), 0) as balance
  from accounts
  where company_id = :company_id
    and type = 'ASSET'
    and (upper(code) like '%AR%' or upper(code) like '%RECEIVABLE%')
),
ar_ledger as (
  select coalesce(sum(debit - credit), 0) as balance
  from dealer_ledger_entries
  where company_id = :company_id
    and entry_date <= :'as_of'::date
)
select ar_control.balance as ar_control,
       ar_ledger.balance as dealer_ledger,
       (ar_control.balance - ar_ledger.balance) as variance
from ar_control, ar_ledger;

\echo '5) AP control vs supplier ledger tie-out (as-of)'
with ap_control as (
  select coalesce(sum(balance), 0) as balance
  from accounts
  where company_id = :company_id
    and type = 'LIABILITY'
    and (upper(code) like '%AP%' or upper(code) like '%PAYABLE%')
),
ap_ledger as (
  select coalesce(sum(credit - debit), 0) as balance
  from supplier_ledger_entries
  where company_id = :company_id
    and entry_date <= :'as_of'::date
)
select ap_control.balance as ap_control,
       ap_ledger.balance as supplier_ledger,
       (ap_control.balance - ap_ledger.balance) as variance
from ap_control, ap_ledger;

\echo '6) Inventory control account balance'
select c.code as company_code, a.id as inventory_account_id, a.balance as ledger_balance
from companies c
join accounts a on a.id = c.default_inventory_account_id
where c.id = :company_id;
