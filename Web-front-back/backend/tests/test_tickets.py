import pytest
from flask import Flask
from flask_jwt_extended import create_access_token, JWTManager
from app.models import db, Ticket
from app.tickets import tickets_bp  # zakładamy że tickets.py jest w app/


# Konfiguracja aplikacji testowej
@pytest.fixture
def app():
    app = Flask(__name__)
    app.config["JWT_SECRET_KEY"] = "test-secret"
    app.config["TESTING"] = True
    app.config["FLASK_ENV"] = "testing"
    app.config["SQLALCHEMY_DATABASE_URI"] = "sqlite:///:memory:"
    JWTManager(app)

    db.init_app(app)
    app.register_blueprint(tickets_bp)

    with app.app_context():
        db.create_all()
        yield app
        db.session.remove()
        db.drop_all()

@pytest.fixture
def client(app):
    return app.test_client()

@pytest.fixture
def access_token():
    return create_access_token(identity="1")

@pytest.fixture
def auth_headers(access_token):
    return {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }

# Testy: /ticket (POST)

def test_upload_ticket_success(client, auth_headers):
    data = {
        "vehicle_number": "ABC123",
        "location": "Warsaw",
        "date": "2024-12-31",
        "time": "12:30"
    }

    response = client.post("/ticket", json=data, headers=auth_headers)
    assert response.status_code == 201, response.get_json()
    assert "id" in response.get_json()

def test_upload_ticket_missing_fields(client, auth_headers):
    data = {"vehicle_number": "XYZ"}
    response = client.post("/ticket", json=data, headers=auth_headers)
    assert response.status_code == 400
    assert "Brakuje wymaganych pól" in response.get_json()["msg"]

def test_upload_ticket_invalid_date(client, auth_headers):
    data = {
        "vehicle_number": "ABC123",
        "location": "Warsaw",
        "date": "31-12-2024",  # błędny format
        "time": "12:30"
    }
    response = client.post("/ticket", json=data, headers=auth_headers)
    assert response.status_code == 400
    assert "Nieprawidłowy format daty" in response.get_json()["msg"]

# Testy: /tickets (GET)

def test_search_tickets_empty(client, auth_headers):
    response = client.get("/tickets", headers=auth_headers)
    assert response.status_code == 200
    assert response.get_json() == []

def test_search_tickets_with_filter(client, auth_headers):
    client.post("/ticket", json={
        "vehicle_number": "XYZ123",
        "location": "Krakow",
        "date": "2024-12-30",
        "time": "14:15"
    }, headers=auth_headers)

    response = client.get("/tickets?location=Krakow", headers=auth_headers)
    assert response.status_code == 200
    tickets = response.get_json()
    assert len(tickets) == 1
    assert tickets[0]["location"] == "Krakow"

# Testy: /ticket/<id> (GET)

def test_get_ticket_by_id(client, auth_headers):
    post_response = client.post("/ticket", json={
        "vehicle_number": "LMN789",
        "location": "Lodz",
        "date": "2024-12-01",
        "time": "09:45"
    }, headers=auth_headers)

    assert post_response.status_code == 201, post_response.get_json()
    ticket_id = post_response.get_json()["id"]

    get_response = client.get(f"/ticket/{ticket_id}", headers=auth_headers)
    assert get_response.status_code == 200
    assert get_response.get_json()["location"] == "Lodz"

# Testy: /ticket/<id> (DELETE)

def test_delete_ticket_success(client, auth_headers):
    post_response = client.post("/ticket", json={
        "vehicle_number": "JKL123",
        "location": "Poznan",
        "date": "2024-10-20",
        "time": "10:00"
    }, headers=auth_headers)

    assert post_response.status_code == 201, post_response.get_json()
    ticket_id = post_response.get_json()["id"]

    delete_response = client.delete(f"/ticket/{ticket_id}", headers=auth_headers)
    assert delete_response.status_code == 200
    assert f"Bilet {ticket_id} został usunięty" in delete_response.get_json()["msg"]
