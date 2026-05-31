import requests
import json

payload = {
    "firstName": "test",
    "lastName": "test",
    "userName": "testuser3",
    "email": "test3@example.com",
    "password": "password123"
}

try:
    response = requests.post("http://localhost:8080/api/v1/registration", json=payload)
    print(f"Status: {response.status_code}")
    print(f"Response: {response.text}")
except Exception as e:
    print(e)
