# Integration Contract Addendum (Kebun)

## Kebun Ownership
Kebun service is the source of truth for:
- kebun identity and metadata
- kebun code
- kebun coordinates
- Mandor-Kebun assignment

## Integration Key
Current canonical integration key is `kebunCode`.
- Reason: kebun currently uses `code` as primary identifier.
- If a stable `kebunId` is introduced later, responses may include it as an additional field.

## Required Read API for Sibling Services
Kebun must expose read-only endpoint for Mandor assignment context:
- `GET /internal/mandors/{mandorId}/kebun`

This endpoint is required by Hasil Panen validation flow.

## Boundaries
Kebun must not own Buruh-Mandor assignment logic (owned by Auth).
