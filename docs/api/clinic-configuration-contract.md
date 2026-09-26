# NCL-09-CN-002 — Clinic configuration contract

## Scope

This contract defines the shared operational configuration for one clinic. It
does not create a second representation of examination rooms.

The canonical examination-room resource remains `rooms`, managed through the
existing `/rooms` endpoints. A clinic-configuration write never creates,
renames, activates, deactivates, or deletes a room.

## Resource

The system has exactly one clinic configuration. Its persisted fields are:

| Field | Type | Required | Rules |
| --- | --- | --- | --- |
| `clinicName` | string | Yes | Trimmed, non-blank, maximum 150 characters. |
| `openingTime` | `HH:mm:ss` | Yes | Local clinic time. |
| `closingTime` | `HH:mm:ss` | Yes | Must be later than `openingTime`. |
| `address` | string | No | Trimmed, maximum 500 characters. |
| `phone` | string | No | Trimmed, maximum 30 characters. |

`rooms`, `roomIds`, and `examinationRooms` are not fields of this resource and
must not be persisted as strings or JSON in clinic configuration.

## API

Base path: `/api/v1`

### `GET /system/clinic`

Returns the singleton configuration. When it has not been configured, return
`200 OK` with all fields set to `null`; do not return `404`.

Only `ADMIN` may call this endpoint.

```json
{
  "clinicName": "Phòng khám An Tâm",
  "address": "Quận 1, TP.HCM",
  "phone": "0901234567",
  "openingTime": "08:00:00",
  "closingTime": "17:00:00"
}
```

### `PUT /system/clinic`

Creates the singleton configuration if absent, otherwise replaces its fields.
Only `ADMIN` may call this endpoint.

```json
{
  "clinicName": "Phòng khám An Tâm",
  "address": "Quận 1, TP.HCM",
  "phone": "0901234567",
  "openingTime": "08:00:00",
  "closingTime": "17:00:00"
}
```

The successful response is `200 OK` and has the same representation as
`GET /system/clinic`.

## Validation and authorization

| Condition | HTTP result |
| --- | --- |
| Missing or blank `clinicName` | `400 Bad Request` |
| Missing/invalid time, or closing time not later than opening time | `400 Bad Request` |
| No authenticated user | `401 Unauthorized` |
| Authenticated user without `ADMIN` role | `403 Forbidden` |

This enforces QTN-01 and the acceptance criteria NCL-09-CN-002-TC-01 through
NCL-09-CN-002-TC-03.

## Room boundary

Clients obtain and administer examination rooms through `/rooms`. Any screen
that needs both clinic metadata and rooms must call `GET /system/clinic` and
`GET /rooms` separately. The clinic endpoint does not return or mutate rooms.

## Implementation acceptance checks

1. An administrator can create and subsequently read the singleton
   configuration.
2. A write without `clinicName` is rejected with `400`.
3. A non-admin request is rejected with `403`.
4. No schema column or payload field stores a room list for this resource.
