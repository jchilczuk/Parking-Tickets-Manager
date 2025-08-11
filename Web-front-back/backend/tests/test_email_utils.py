import pytest
from unittest.mock import patch, MagicMock
from flask import Flask
from app.email_utils import send_ticket_email
from app.models import User
from app.extensions import mail
from flask_mail import Message
from app import db, create_app
import base64
from datetime import datetime, date, time

@pytest.fixture
def app_with_user():
    test_config = {
        "TESTING": True,
        "FLASK_ENV": "testing",
        "SQLALCHEMY_DATABASE_URI": "sqlite:///:memory:",
        "MAIL_DEFAULT_SENDER": "noreply@example.com"
    }

    app = create_app(test_config=test_config)

    with app.app_context():
        db.create_all()

        user = User(
            email="test@example.com",
            password="hashed",
            name="Test",
            surname="User"
        )
        db.session.add(user)
        db.session.commit()

        yield app, user

        db.drop_all()
        db.session.remove()

def test_send_ticket_email_success(app_with_user):
    app, user = app_with_user

    mock_ticket = MagicMock()
    mock_ticket.user_id = user.id
    mock_ticket.uploaded_at = datetime(2024, 6, 1)
    mock_ticket.date = date(2024, 6, 1)
    mock_ticket.time = time(12, 0)
    mock_ticket.location = "Testowa 123"
    mock_ticket.vehicle_number = "ABC1234"
    mock_ticket.image_base64 = base64.b64encode(b"fake image data").decode("utf-8")

    with app.app_context(), patch.object(mail, "send") as mock_send:
        send_ticket_email(mock_ticket)
        assert mock_send.called
        sent_msg: Message = mock_send.call_args[0][0]
        assert user.email in sent_msg.recipients
        assert "Przeterminowany bilet" in sent_msg.subject
        assert "Test" in sent_msg.body
        assert sent_msg.attachments  # czy załącznik dodany

def test_send_ticket_email_no_user(app_with_user):
    app, _ = app_with_user

    mock_ticket = MagicMock()
    mock_ticket.user_id = 999  # nie istnieje
    mock_ticket.image_base64 = None

    with app.app_context(), patch.object(mail, "send") as mock_send:
        send_ticket_email(mock_ticket)
        mock_send.assert_not_called()

def test_send_ticket_email_no_sender_config(app_with_user):
    app, user = app_with_user
    app.config["MAIL_DEFAULT_SENDER"] = None

    mock_ticket = MagicMock()
    mock_ticket.user_id = user.id
    mock_ticket.uploaded_at = datetime(2024, 6, 1)
    mock_ticket.date = date(2024, 6, 1)
    mock_ticket.time = time(12, 0)
    mock_ticket.location = "Testowa 123"
    mock_ticket.vehicle_number = "ABC1234"
    mock_ticket.image_base64 = None

    with app.app_context(), pytest.raises(RuntimeError):
        send_ticket_email(mock_ticket)
