import pytest
from app import create_app, db
from app.models import User
from werkzeug.security import generate_password_hash
from flask_jwt_extended import create_access_token

@pytest.fixture
def client():
    app = create_app(test_config={
        "TESTING": True,
        "FLASK_ENV": "testing",
        "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
        "JWT_SECRET_KEY": "testowy_klucz_jwt",
        "SECRET_KEY": "tajny",
        "MAIL_SUPPRESS_SEND": True
    })
    with app.app_context():
        db.create_all()
        user = User(
            email="test@example.com",
            password=generate_password_hash("sekret"),
            name="Test",
            surname="User"
        )
        db.session.add(user)
        db.session.commit()
        token = create_access_token(identity=str(user.id))
        yield app.test_client(), token
        db.session.remove()
        db.drop_all()

# Rejestracja

def test_register_valid_user(client):
    """Rejestracja nowego użytkownika"""
    test_client, _ = client
    response = test_client.post("/auth/register", json={
        "email": "jan.kowalski@example.com",
        "password": "tajnehaslo",
        "name": "Jan",
        "surname": "Kowalski"
    })
    assert response.status_code == 201
    json_data = response.get_json()
    assert json_data["msg"] == "Zarejestrowano pomyślnie"

def test_register_duplicate_email(client):
    """Rejestracja z istniejącym adresem e-mail"""
    test_client, _ = client
    test_client.post("/auth/register", json={
        "email": "jan@example.com",
        "password": "haslo123",
        "name": "Jan",
        "surname": "Nowak"
    })
    response = test_client.post("/auth/register", json={
        "email": "jan@example.com",
        "password": "haslo456",
        "name": "Janek",
        "surname": "Kowalski"
    })
    assert response.status_code == 409
    json_data = response.get_json()
    assert "już istnieje" in json_data["msg"]

def test_register_short_password(client):
    """Rejestracja z hasłem krótszym niż 5 znaków"""
    test_client, _ = client
    response = test_client.post("/auth/register", json={
        "email": "krótkie@example.com",
        "password": "123",  # za krótkie
        "name": "Krótki",
        "surname": "Hasło"
    })
    assert response.status_code == 400
    json_data = response.get_json()
    assert "co najmniej 5 znaków" in json_data["msg"]

# Logowanie

def test_login_success(client):
    """Poprawne logowanie i otrzymanie tokenu"""
    test_client, _ = client
    response = test_client.post("/auth/login", json={
        "email": "test@example.com",
        "password": "sekret"
    })
    assert response.status_code == 200
    json_data = response.get_json()
    assert "access_token" in json_data
    assert json_data["name"] == "Test"
    assert json_data["surname"] == "User"

def test_login_wrong_password(client):
    """Logowanie z błędnym hasłem"""
    test_client, _ = client
    response = test_client.post("/auth/login", json={
        "email": "test@example.com",
        "password": "zlehaslo"
    })
    assert response.status_code == 401
    assert "msg" in response.get_json()

def test_login_nonexistent_user(client):
    """Logowanie nieistniejącego użytkownika"""
    test_client, _ = client
    response = test_client.post("/auth/login", json={
        "email": "nieistnieje@example.com",
        "password": "abc123"
    })
    assert response.status_code == 401
    assert "msg" in response.get_json()

# Rejestracja tokenu FCM

def test_register_token_success(client):
    """Zapisanie tokena FCM z poprawnym JWT"""
    test_client, token = client
    response = test_client.post(
        "/auth/register_token",
        json={"fcm_token": "abc123"},
        headers={"Authorization": f"Bearer {token}"}
    )
    assert response.status_code == 200
    json_data = response.get_json()
    assert json_data["msg"] == "Token updated"

def test_register_token_missing(client):
    """Brak tokena FCM w żądaniu"""
    test_client, token = client
    response = test_client.post(
        "/auth/register_token",
        json={},
        headers={"Authorization": f"Bearer {token}"}
    )
    assert response.status_code == 400
    json_data = response.get_json()
    assert json_data["msg"] == "Missing token"

def test_register_token_unauthorized(client):
    """Brak JWT w nagłówku"""
    test_client, _ = client
    response = test_client.post(
        "/auth/register_token",
        json={"fcm_token": "abc123"}
    )
    assert response.status_code == 401
