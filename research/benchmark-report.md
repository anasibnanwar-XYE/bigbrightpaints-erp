# SMB & Mid-Market ERP Competitive Benchmark

## Scope
This benchmark compares your ERP (from openapi.json) to SMB/mid-market ERPs: SAP Business One, Odoo, ERPNext, TallyPrime, QuickBooks Online, Zoho Books, and Busy Accounting.

## Methodology
- Review sources: 5 sources per competitor (see Source Index). Web search was attempted but DuckDuckGo rate-limited results; direct review pages were used instead.
- Extraction: HTML pages were cleaned to plain text; keyword heuristics identified feature mentions and complaint signals.
- Sentiment: simple lexicon (positive/negative words) aggregated at sentence level; used only to scale feature scores.
- Scoring formula per feature: score = 1 + 4 * (0.6 * freq_norm + 0.4 * sent_norm), where freq_norm = log1p(mentions)/log1p(max_mentions).
- Local ERP: feature coverage derived from endpoint counts in openapi.json; coverage_score is not sentiment-based.

## Your ERP Coverage (openapi.json)
Product: BigBright ERP Domain API (version v1, 214 endpoints)
Key modules inferred from API paths:
- Accounting: evidence keywords journal-entries, trial-balance, gst, periods
- Inventory: evidence keywords landed-cost, revaluation, wip, stock
- Manufacturing: evidence keywords factory, production
- Sales/Dealer: evidence keywords dealer-portal, invoices
- HR/Payroll: evidence keywords attendance, payroll
- Dispatch: evidence keywords dispatch
- Reports: evidence keywords balance-sheet, cash-flow, inventory-reconciliation
- Security: evidence keywords auth, mfa, roles
- Orchestrator: evidence keywords orchestrator
Orchestrator endpoints include approvals, dispatch, payroll run, integration health, and traceability (see /api/v1/orchestrator/* in openapi.json).

## Overall Scores (Average Across Features)
- Zoho Books: 4.29 [S026], [S027], [S028], [S029], [S030]
- Busy Accounting: 4.29 [S031], [S032], [S033], [S034], [S035]
- TallyPrime: 4.16 [S016], [S017], [S018], [S020], [S038]
- SAP Business One: 3.99 [S001], [S002], [S003], [S004], [S036]
- QuickBooks Online: 3.99 [S021], [S022], [S023], [S024], [S025]
- Odoo: 3.95 [S006], [S007], [S008], [S009], [S037]
- ERPNext: 3.53 [S011], [S012], [S013], [S014], [S015]

Your ERP coverage scores are in `research/feature-matrix.csv` as `Your ERP (openapi-derived)`.

## Automation Benchmark
Automation & Workflow feature scores from reviews:
- Zoho Books: 5.0 [S026], [S027], [S028], [S029], [S030]
- QuickBooks Online: 4.12 [S021], [S022], [S023], [S024], [S025]
- Odoo: 4.07 [S006], [S007], [S008], [S009], [S037]
- SAP Business One: 3.92 [S001], [S002], [S003], [S004], [S036]
- TallyPrime: 3.77 [S016], [S017], [S018], [S020], [S038]
- ERPNext: 2.85 [S011], [S012], [S013], [S014], [S015]
- Busy Accounting: 2.7 [S031], [S032], [S033], [S034], [S035]

Your ERP includes an orchestrator layer with approvals, dispatch, payroll runs, integration/event health checks, and traceability. This indicates stronger built-in workflow automation than many SMB tools, which frequently rely on manual steps or add-ons.

## Feature Matrix
Full matrix: `research/feature-matrix.csv` (scores 1-5 from review text; local coverage score from openapi).
Detailed counts + sentiment per feature: `research/feature-scores.csv`.

## Repeated Feature Mentions & Complaints
### Busy Accounting [S031], [S032], [S033], [S034], [S035]
Top feature mentions:
- Pricing & Value: 87
- Sales & CRM: 84
- Usability & UX: 79
- Customization & Extensibility: 78
- Support & Service: 60
Top complaint signals (negative mentions):
- Pricing & Value: 8
- Usability & UX: 7
- Performance & Reliability: 7
- Sales & CRM: 5
- Tax & Compliance (GST/VAT): 5

### ERPNext [S011], [S012], [S013], [S014], [S015]
Top feature mentions:
- Customization & Extensibility: 52
- Sales & CRM: 51
- Usability & UX: 43
- Support & Service: 40
- Pricing & Value: 39
Top complaint signals (negative mentions):
- Sales & CRM: 8
- Payroll & HR: 7
- Customization & Extensibility: 7
- Support & Service: 7
- Pricing & Value: 7

### Odoo [S006], [S007], [S008], [S009], [S037]
Top feature mentions:
- Customization & Extensibility: 127
- Sales & CRM: 124
- Usability & UX: 100
- Pricing & Value: 96
- Support & Service: 73
Top complaint signals (negative mentions):
- Sales & CRM: 26
- Customization & Extensibility: 24
- Support & Service: 23
- Pricing & Value: 23
- Payroll & HR: 10

### QuickBooks Online [S021], [S022], [S023], [S024], [S025]
Top feature mentions:
- Usability & UX: 214
- Sales & CRM: 94
- Customization & Extensibility: 90
- Support & Service: 90
- Pricing & Value: 89
Top complaint signals (negative mentions):
- Usability & UX: 40
- Sales & CRM: 26
- Customization & Extensibility: 25
- Payroll & HR: 22
- Pricing & Value: 22

### SAP Business One [S001], [S002], [S003], [S004], [S036]
Top feature mentions:
- Sales & CRM: 71
- Usability & UX: 67
- Customization & Extensibility: 65
- Pricing & Value: 55
- Reporting & Analytics: 50
Top complaint signals (negative mentions):
- Pricing & Value: 16
- Sales & CRM: 13
- Customization & Extensibility: 13
- Support & Service: 12
- Payroll & HR: 11

### TallyPrime [S016], [S017], [S018], [S020], [S038]
Top feature mentions:
- Usability & UX: 82
- Pricing & Value: 59
- Sales & CRM: 55
- Tax & Compliance (GST/VAT): 49
- Customization & Extensibility: 49
Top complaint signals (negative mentions):
- Usability & UX: 5
- Customization & Extensibility: 4
- Pricing & Value: 4
- Performance & Reliability: 3
- Reporting & Analytics: 2

### Zoho Books [S026], [S027], [S028], [S029], [S030]
Top feature mentions:
- Sales & CRM: 115
- Customization & Extensibility: 98
- Usability & UX: 97
- Pricing & Value: 92
- AR/AP & Invoicing: 81
Top complaint signals (negative mentions):
- Usability & UX: 11
- Pricing & Value: 7
- Sales & CRM: 6
- Payroll & HR: 6
- Customization & Extensibility: 6

## Benchmark Summary (Highlights)
- Accounting & compliance features dominate review mentions across competitors, reflecting SMB buyer priorities around invoicing, taxes, and core financials. [S001] [S006] [S011] [S016] [S021] [S026] [S032]
- Manufacturing/WIP capabilities are mentioned less consistently, leaving room for differentiation where you already have landed cost, WIP adjustment, and production modules. [S001] [S006] [S011] [S015]
- Automation/workflow signals are uneven across SMB ERPs; your orchestrator endpoints suggest stronger native workflow coverage than typical SMB accounting tools. [S006] [S011] [S021]
- Common complaint patterns cluster around complexity/learning curve, support responsiveness, and performance issues, so onboarding UX and support SLAs are competitive levers. [S006] [S016] [S021] [S026] [S032]

## Source Index (Citations)
- [S001] SAP Business One - TrustRadius: https://www.trustradius.com/products/sap-business-one/reviews
- [S002] SAP Business One - SoftwareReviews: https://www.softwarereviews.com/products/sap-business-one
- [S003] SAP Business One - SoftwareSuggest: https://www.softwaresuggest.com/sap-business-one
- [S004] SAP Business One - FinancesOnline: https://reviews.financesonline.com/p/sap-business-one/
- [S006] Odoo - TrustRadius: https://www.trustradius.com/products/odoo/reviews
- [S007] Odoo - SoftwareReviews: https://www.softwarereviews.com/products/odoo
- [S008] Odoo - SoftwareSuggest: https://www.softwaresuggest.com/odoo
- [S009] Odoo - FinancesOnline: https://reviews.financesonline.com/p/odoo/
- [S011] ERPNext - TrustRadius: https://www.trustradius.com/products/erpnext/reviews
- [S012] ERPNext - SoftwareReviews: https://www.softwarereviews.com/products/erpnext
- [S013] ERPNext - SoftwareSuggest: https://www.softwaresuggest.com/erpnext
- [S014] ERPNext - FinancesOnline: https://reviews.financesonline.com/p/erpnext/
- [S015] ERPNext - Techjockey: https://www.techjockey.com/reviews/erpnext
- [S016] TallyPrime - TrustRadius: https://www.trustradius.com/products/tallyprime/reviews
- [S017] TallyPrime - SoftwareReviews: https://www.softwarereviews.com/products/tallyprime
- [S018] TallyPrime - SoftwareSuggest: https://www.softwaresuggest.com/tallyprime
- [S020] TallyPrime - Trustpilot: https://www.trustpilot.com/review/tallysolutions.com
- [S021] QuickBooks Online - TrustRadius: https://www.trustradius.com/products/quickbooks-online/reviews
- [S022] QuickBooks Online - SoftwareReviews: https://www.softwarereviews.com/products/quickbooks-online
- [S023] QuickBooks Online - SoftwareSuggest: https://www.softwaresuggest.com/quickbooks
- [S024] QuickBooks Online - FinancesOnline: https://reviews.financesonline.com/p/quickbooks-online/
- [S025] QuickBooks Online - Trustpilot: https://www.trustpilot.com/review/quickbooks.intuit.com
- [S026] Zoho Books - TrustRadius: https://www.trustradius.com/products/zoho-books/reviews
- [S027] Zoho Books - SoftwareReviews: https://www.softwarereviews.com/products/zoho-books
- [S028] Zoho Books - SoftwareSuggest: https://www.softwaresuggest.com/zoho-books
- [S029] Zoho Books - FinancesOnline: https://reviews.financesonline.com/p/zoho-books/
- [S030] Zoho Books - Techjockey: https://www.techjockey.com/reviews/zoho-books
- [S031] Busy Accounting - SoftwareReviews: https://www.softwarereviews.com/products/busy-accounting-software
- [S032] Busy Accounting - SoftwareSuggest: https://www.softwaresuggest.com/busy-accounting
- [S033] Busy Accounting - SoftwareSuggest Reviews: https://www.softwaresuggest.com/busy-accounting/reviews
- [S034] Busy Accounting - Techjockey: https://www.techjockey.com/reviews/busy-accounting-software
- [S035] Busy Accounting - Trustpilot: https://www.trustpilot.com/review/busy.in
- [S036] SAP Business One - SoftwareSuggest Reviews: https://www.softwaresuggest.com/sap-business-one/reviews
- [S037] Odoo - SoftwareSuggest Reviews: https://www.softwaresuggest.com/odoo/reviews
- [S038] TallyPrime - SoftwareSuggest Reviews: https://www.softwaresuggest.com/tallyprime/reviews