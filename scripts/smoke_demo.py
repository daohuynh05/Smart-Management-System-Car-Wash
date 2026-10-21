"""Check a freshly seeded demo through its public frontend/proxy URL."""
import json
import os
import sys
from urllib.error import HTTPError
from urllib.request import Request, urlopen

base = sys.argv[1].rstrip("/")


def request(path, data=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = Request(base + path, data=json.dumps(data).encode() if data else None, headers=headers)
    try:
        with urlopen(req, timeout=30) as response:
            body = response.read()
            return response.status, json.loads(body) if body else None
    except HTTPError as error:
        return error.code, None


for role, email, password in (
    ("CUSTOMER", "customer@example.invalid", os.getenv("DEMO_CUSTOMER_PASSWORD", "CustomerDemo!2026")),
    ("ADMIN", "admin@example.invalid", os.getenv("DEMO_ADMIN_PASSWORD", "AdminDemo!2026")),
):
    status, auth = request("/api/auth/login", {"emailOrPhone": email, "password": password})
    assert status == 200 and auth["role"] == role, (role, status)
    token = auth["accessToken"]
    assert request("/api/auth/me", token=token)[0] == 200
    expected = 200 if role == "ADMIN" else 403
    assert request("/api/admin/dashboard/stats", token=token)[0] == expected
    print(role + ": login, profile and admin authorization OK")
assert request("/api/auth/login", {"emailOrPhone": "customer@example.invalid", "password": "wrong"})[0] >= 400
