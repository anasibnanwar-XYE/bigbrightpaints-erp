# Pagination and Filters

Last reviewed: 2026-03-31

## List Query Patterns

All list endpoints follow a consistent pagination pattern:

```
GET /api/v1/{module}?page=0&size=20&sort=field,asc
```

### Query Parameters

| Parameter | Type | Default | Description |
|---|---|---|---|
| `page` | integer | 0 | Zero-based page number |
| `size` | integer | 20 | Number of items per page (max 100) |
| `sort` | string | varies | Sort field and direction, e.g., `createdAt,desc` |

### Response Structure

```json
{
  "success": true,
  "data": {
    "content": [
      { /* first item */ },
      { /* second item */ }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "message": "List retrieved",
  "timestamp": "2026-03-31T10:00:00Z"
}
```

## Cursor vs Offset

The ERP uses **offset-based pagination** for list endpoints. Cursor-based pagination is not currently supported.

### When to Use Offset Pagination

- Fixed-size page navigation (e.g., "page 1 of 8")
- User-initiated page navigation
- Known total counts are needed

### Limitations

- Large offsets (e.g., page 1000+) may be slow.
- For very large datasets, consider using filter-based narrowing instead of deep pagination.

## Filter Syntax

Filters are passed as query parameters using the pattern:

```
GET /api/v1/{module}?filter.field=value&filter.anotherField=value
```

### Supported Operators

| Operator | Syntax | Example |
|---|---|---|
| Equals | `filter.field=value` | `filter.status=ACTIVE` |
| In | `filter.field.in=value1,value2` | `filter.status.in=ACTIVE,PENDING` |
| Like | `filter.field.like=%value%` | `filter.name.like=%admin%` |
| Greater Than | `filter.field.gt=value` | `filter.amount.gt=1000` |
| Less Than | `filter.field.lt=value` | `filter.createdAt.lt=2026-03-31` |
| Between | `filter.field.gte=value1&filter.field.lte=value2` | `filter.amount.gte=100&filter.amount.lte=500` |

### Filter Examples

**Filter by status:**

```
GET /api/v1/admin/users?filter.status=ACTIVE
```

**Filter by multiple values:**

```
GET /api/v1/invoices?filter.status.in=PENDING,APPROVED
```

**Filter by date range:**

```
GET /api/v1/journal-entries?filter.entryDate.gte=2026-01-01&filter.entryDate.lte=2026-03-31
```

**Filter with like (search):**

```
GET /api/v1/dealers?filter.name.like=%Acme%
```

## Sorting

### Sort Parameter

```
sort=fieldName,asc | sort=fieldName,desc
```

### Multiple Sort Fields

```
sort=field1,asc&sort=field2,desc
```

### Common Sort Fields

| Endpoint | Default Sort | Available Sort Fields |
|---|---|---|
| `/api/v1/admin/users` | `createdAt,desc` | `email`, `displayName`, `createdAt`, `status` |
| `/api/v1/invoices` | `createdAt,desc` | `invoiceNumber`, `createdAt`, `dueDate`, `totalAmount` |
| `/api/v1/journal-entries` | `entryDate,desc` | `entryDate`, `referenceNumber`, `createdAt` |
| `/api/v1/dealers` | `name,asc` | `name`, `code`, `createdAt`, `status` |

## Combining Filters and Pagination

All parameters can be combined:

```
GET /api/v1/invoices?filter.status=PENDING&filter.dueDate.lt=2026-04-30&sort=dueDate,asc&page=0&size=10
```

### Response:

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "invoiceNumber": "INV-2026-0042",
        "dueDate": "2026-04-15",
        "totalAmount": 1500.00,
        "status": "PENDING"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "first": true,
    "last": false
  }
}
```

## Client-Side Handling

### Pagination UI

- Show page numbers with navigation (first, previous, next, last).
- Display "Showing 1-20 of 156" style feedback.
- Disable navigation buttons at boundaries.

### Filter UI

- Build filter forms with appropriate input types (select, date picker, text).
- Show active filters as removable chips.
- Provide "Clear all filters" action.

### Performance Considerations

- Debounce filter inputs (300-500ms).
- Show loading state during fetch.
- Cache page results if re-fetching is likely (e.g., back button).

## Links

- See individual portal `api-contracts.md` files in [`docs/frontend-portals/`](../frontend-portals/) for module-specific pagination behavior.
