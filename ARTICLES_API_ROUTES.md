# Articles API Routes (Cam nang)

## Public routes

### GET /api/articles

- Chuc nang: list bai viet da publish cho customer/client.
- Query:
  - `category` (String, optional): loc theo danh muc.
- Output: `List<ArticleResponse>`.

### GET /api/articles/{slug}

- Chuc nang: chi tiet bai viet da publish theo slug.
- Ghi chu: moi lan doc chi tiet se tang `viewCount`.
- Output: `ArticleResponse`.

## Admin routes

Tat ca route ben duoi can `Authorization: Bearer <admin_token>`.

### GET /api/admin/articles

- Chuc nang: list tat ca bai viet, bao gom draft.
- Output: `List<ArticleResponse>`.

### POST /api/admin/articles

- Chuc nang: tao bai viet.
- Input:

```json
{
  "title": "Cach bao duong xe sau khi rua",
  "content": "<p>Noi dung bai viet...</p>",
  "thumbnailUrl": "https://example.com/thumb.jpg",
  "category": "Bao duong",
  "published": false
}
```

- Output: `ArticleResponse`.

### PUT /api/admin/articles/{articleId}

- Chuc nang: sua bai viet.
- Input: giong `POST /api/admin/articles`.
- Output: `ArticleResponse`.

### DELETE /api/admin/articles/{articleId}

- Chuc nang: xoa bai viet.
- Output: HTTP 204 No Content.

### PATCH /api/admin/articles/{articleId}/publish

- Chuc nang: publish/unpublish bai viet.
- Input optional:

```json
{
  "published": true
}
```

- Ghi chu: neu khong gui body, API se toggle trang thai hien tai.
- Output: `ArticleResponse`.

## ArticleResponse

```json
{
  "articleId": "uuid",
  "title": "Cach bao duong xe sau khi rua",
  "slug": "cach-bao-duong-xe-sau-khi-rua",
  "content": "<p>Noi dung bai viet...</p>",
  "excerpt": "Noi dung ngan de hien thi card...",
  "thumbnailUrl": "https://example.com/thumb.jpg",
  "category": "Bao duong",
  "published": true,
  "viewCount": 12,
  "createdAt": "2026-06-21T10:00:00",
  "updatedAt": "2026-06-21T10:00:00"
}
```
