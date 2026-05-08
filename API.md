# API Specification - be-management-kebun

## Base URL
`/kebun`

## Data Model
`Kebun`
- `name` (string)
- `code` (string, immutable after creation)
- `luas` (number)
- `coordinates` (array of 4 points)

`Point`
- `x` (number)
- `y` (number)

## Endpoints

### 1. Create Kebun
- Method: `POST`
- Path: `/kebun`
- Request Body:
```json
{
  "name": "Kebun Sawit A",
  "code": "KBNA01",
  "luas": 100.0,
  "coordinates": [
    { "x": 0, "y": 0 },
    { "x": 0, "y": 2 },
    { "x": 2, "y": 2 },
    { "x": 2, "y": 0 }
  ]
}
```
- Success Response: `201 Created`

### 2. Get Kebun by Code
- Method: `GET`
- Path: `/kebun/{code}`
- Success Response: `200 OK`
- Not Found: `404 Not Found`

### 3. List/Filter Kebun by Name
- Method: `GET`
- Path: `/kebun?name={keyword}`
- Success Response: `200 OK`

### 4. Update Kebun
- Method: `PUT`
- Path: `/kebun/{code}`
- Request Body: same shape as create
- Success Response: `200 OK`
- Validation:
  - `code` in payload must remain the same as existing kebun code.

### 5. Delete Kebun
- Method: `DELETE`
- Path: `/kebun/{code}`
- Success Response: `204 No Content`
- Constraint:
  - delete is blocked if active mandor is still assigned.

## Error Mapping
Global exception mapping:
- `IllegalArgumentException` -> `400 Bad Request`
- `IllegalStateException` -> `409 Conflict`

Error body:
```json
{
  "message": "<error detail>"
}
```

## Domain Constraints Enforced
- Exactly 4 coordinates are required.
- Coordinates must form a square.
- Overlap validation is executed before create.
- Kebun code is immutable on update.
- Deletion blocked for active mandor dependencies.
- Unassigning mandor requires replacement mandor.

## Async Event
On successful mandor assignment, service publishes Kafka event:
- Topic: `app.kafka.topic.mandor-assigned` (default: `mandor-assigned`)
- Event: `MandorAssignedEvent`
```json
{
  "kebunCode": "KBNA01",
  "mandorId": "mandor-123"
}
```
