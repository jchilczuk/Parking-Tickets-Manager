import pytest
from unittest.mock import patch, MagicMock
from flask import Flask
from app import create_app, db
from app.models import Ticket, User
from datetime import datetime, timedelta
from app.notifications import check_expired_tickets

@pytest.fixture
def app_context():
    app = create_app(test_config={"TESTING": True, "FLASK_ENV": "testing", "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:"})
    with app.app_context():
        db.create_all()
        yield app
        db.drop_all()
        db.session.remove()

def create_ticket_and_user(expired=True, with_token=True):
    user = User(email="test@example.com", password="hashed", name="Test", surname="User")
    if with_token:
        user.fcm_token = "fake_token"
    db.session.add(user)
    db.session.commit()

    ticket = Ticket(
        user_id=user.id,
        uploaded_at=datetime.utcnow() - timedelta(days=2),
        date=datetime.utcnow().date() - timedelta(days=1) if expired else datetime.utcnow().date() + timedelta(days=1),
        time=(datetime.utcnow() - timedelta(hours=1)).time() if expired else (datetime.utcnow() + timedelta(hours=2)).time(),
        location="Test St.",
        vehicle_number="ABC123",
        image_base64=None,
        notified=False
    )
    db.session.add(ticket)
    db.session.commit()
    return ticket, user

@patch("app.notifications.send_ticket_email")
@patch("app.notifications.send_push_notification")
def test_expired_ticket_notification(mock_push, mock_email, app_context):
    ticket, user = create_ticket_and_user()

    check_expired_tickets(app_context)

    db.session.refresh(ticket)
    assert ticket.notified is True
    assert mock_email.called
    assert mock_push.called

@patch("app.notifications.send_ticket_email")
@patch("app.notifications.send_push_notification")
def test_no_fcm_token(mock_push, mock_email, app_context):
    ticket, user = create_ticket_and_user(with_token=False)

    check_expired_tickets(app_context)

    db.session.refresh(ticket)
    assert ticket.notified is True
    assert mock_email.called
    assert not mock_push.called

@patch("app.notifications.send_ticket_email")
@patch("app.notifications.send_push_notification")
def test_non_expired_ticket(mock_push, mock_email, app_context):
    ticket, user = create_ticket_and_user(expired=False)

    check_expired_tickets(app_context)

    db.session.refresh(ticket)
    assert ticket.notified is False
    assert not mock_email.called
    assert not mock_push.called

@patch("app.notifications.send_ticket_email")
@patch("app.notifications.send_push_notification")
def test_user_does_not_exist(mock_push, mock_email, app_context):
    # Dodajemy bilet ze złym user_id
    ticket = Ticket(
        user_id=999,
        uploaded_at=datetime.utcnow(),
        date=datetime.utcnow().date() - timedelta(days=1),
        time=(datetime.utcnow() - timedelta(hours=1)).time(),
        location="Test",
        vehicle_number="XYZ",
        notified=False
    )
    db.session.add(ticket)
    db.session.commit()

    check_expired_tickets(app_context)

    db.session.refresh(ticket)
    assert ticket.notified is True
    assert mock_email.called
    assert not mock_push.called
