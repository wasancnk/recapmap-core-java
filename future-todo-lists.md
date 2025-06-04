## Deferred Phases (Planned for Future)
- **Phase 5: Downstream Integration & Human Review**
  - RAG/search backend integration for extracted/merged results.
  - Human-in-the-loop review UI for admins to review, correct, and approve extraction results and errors.
- **Phase 6: Extensibility & Advanced Features**
  - Add OCR/hybrid extraction support for image-heavy or scanned documents.
  - Advanced analytics, reporting, and visualization of extraction quality and usage metrics.

---
# Future TODO List for IP Copilot

## AI Extraction & Prompting
- Support user-initiated re-prompting for data extraction (allow users to re-run or customize prompts per page/extraction).
- Allow custom prompt templates per user/session, editable without code changes (admin UI for prompt management).
- Support for both plain text/Markdown and JSON prompt/response formats, with admin-editable templates.

## Data Versioning & History
- Implement versioning/history for extractions (keep previous versions when a page is re-processed or re-annotated).

## Access Control
- Use access control values: `SHARED_GLOBAL` (all users/tenants) and `PRIVATE_TO_ENTITY` (specific user/entity only).
- Review and improve access control logic in backend and UI.

## Full-Text Search & Indexing
- Implement full-text search and semantic search in MongoDB Atlas for extracted text, keywords, and topics.
- Add support for advanced search and filtering in the UI.

## Data Privacy & Retention
- Add data privacy and retention controls for images and extracted text (configurable retention, deletion, audit logging).

## UI/UX
- Add support for history/version review in the extraction review UI.
- Allow users to add custom comments or corrections to extractions, with audit trail.

## Database
- Add PostgreSQL support for future phases (migration or hybrid storage).

## Glue Tasks (Extraction Trigger)
- Support two options for triggering extraction:
  1. Manual: Button in the UI to trigger extraction for a file or batch.
  2. Automatic: Extraction runs immediately after PDF-to-image conversion completes.

---
This file is a living document. Update as new ideas and requirements arise.
