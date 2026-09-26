# Exception conventions

Use `ValidationException` for invalid input or a local domain invariant when the
client only needs to show a message and correct the request. It always produces
`400` with the stable `VALIDATION_FAILED` code.

Create a specific domain exception only when at least one of these is true:

- The client must take a distinct action based on its code or `details`.
- The failure describes a business-state transition, access rule, missing
  resource, concurrency conflict, or external operation that must be observed
  separately.
- The exception carries structured data needed by the client, such as stock
  shortages or interaction warnings.

Specific exceptions must be registered in `DomainErrorCode`. Do not create one
solely to restate a required field or an invalid value; use `ValidationException`
instead.

Domain exceptions must not import HTTP or Spring types. REST maps a
`DomainErrorCode` to an HTTP status in `DomainExceptionHttpStatusMapper`.

When a module has multiple business exceptions, they must extend that module's
abstract exception base (for example `AuthException`, `QueueException`, or
`ClinicalException`). A module with only one concrete exception does not need a
base class.

Frontend code must first call `normalizeApiError()` for every HTTP failure. UI
behaviour and translations may branch on `code` (or HTTP `status` for generic
transport/authentication failures), never by parsing `message`. The message is
display-only; validation field errors are read from `details.fields`.

`*NotFoundException` is reserved for a missing resource and maps to `404`. A
failed business precondition must be named for that condition and normally maps
to `409`; authentication failures map to `401` without exposing whether an
account exists.

Infrastructure exceptions (for example PDF rendering or Cloudinary storage)
remain `500 INTERNAL_SERVER_ERROR` responses with no cause details exposed to
clients. Log them where the operation still has context: include the operation,
the applicable entity/storage ID, and the exception as the cause.
