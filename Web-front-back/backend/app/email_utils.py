from flask_mail import Message
from flask import current_app
from app.models import User
from app.extensions import mail
import base64
from datetime import datetime, timezone
from zoneinfo import ZoneInfo

def send_ticket_email(ticket):
    """
    Sends an email to the user about an expired parking ticket.
    Attaches the ticket image if available.
    """
    # Retrieve the user associated with the ticket
    user = User.query.get(ticket.user_id)
    if not user or not user.email:
        print("Użytkownik nie istnieje")
        return

    # Get the sender email address from app config
    sender_email = current_app.config.get("MAIL_DEFAULT_SENDER")
    print("SENDER =", sender_email)

    # Raise an error if sender email is not configured
    if not sender_email:
        raise RuntimeError("Nie wczytuje maila nadawcy.")
    
    # Konwersja czasu UTC na lokalny (Europe/Warsaw)
    utc_dt = datetime.combine(ticket.date, ticket.time).replace(tzinfo=timezone.utc)
    local_dt = utc_dt.astimezone(ZoneInfo("Europe/Warsaw"))
    formatted_time = local_dt.strftime("%H:%M:%S")
    formatted_date = local_dt.strftime("%Y-%m-%d")

    # Create the email message
    msg = Message(
        subject="Przeterminowany bilet parkingowy",
        sender=sender_email,
        recipients=[user.email],
        body=f"""
Cześć {user.name}!

Bilet dodany dnia {ticket.uploaded_at.date()} stracił ważność dnia {formatted_date} o godzinie {formatted_time}.

Lokalizacja: {ticket.location}

Numer pojazdu: {ticket.vehicle_number}

Jeśli masz pytania, skontaktuj się z nami.

Pozdrawiamy,
Zespół Parking App
"""
    )

    # Attach the ticket image if it exists and is valid base64
    if ticket.image_base64:
        try:
            img_data = base64.b64decode(ticket.image_base64)
            msg.attach("bilet.jpg", "image/jpeg", img_data)
        except Exception as e:
            print("Błąd załącznika:", e)

    # Send the email within the Flask app context
    with current_app.app_context():
        try:
            mail.send(msg)
            print("Mail wysłany do:", user.email) 
        except Exception as e:
            print("Błąd wysyłania maila:", e)


