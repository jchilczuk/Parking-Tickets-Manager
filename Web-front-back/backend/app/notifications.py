# Module for handling notifications about expired parking tickets

from datetime import datetime, date, time, timezone
from zoneinfo import ZoneInfo
from app.models import Ticket, db, User
from app.email_utils import send_ticket_email
from app.firebase_utils import send_push_notification

import logging
logger = logging.getLogger(__name__)

def check_expired_tickets(app): 
    logger.info("check_expired_tickets uruchomione")
    """
    Checks for expired parking tickets and notifies users via email and push notification.
    Marks tickets as notified after sending.
    Args:
        app: The Flask application instance (for app context).
    """
    with app.app_context():
        now = datetime.utcnow()
        current_date = now.date()
        current_time = now.time()
        
        logger.info(f"Sprawdzam bilety UTC: {current_date} {current_time}")
        
        # DODAJ DEBUG - sprawdź wszystkie bilety w bazie
        all_tickets = Ticket.query.all()
        logger.info(f"Wszystkich biletów w bazie: {len(all_tickets)}")
        
        for t in all_tickets:
            logger.debug(f"Bilet ID={t.id}, date={t.date}, time={t.time}, notified={t.notified}")
            
            # Sprawdź warunki dla każdego biletu
            date_expired = t.date < current_date
            time_expired = (t.date == current_date) and (t.time <= current_time)
            not_notified = t.notified == False
            
            logger.debug(f"ID={t.id}: date_expired={date_expired}, time_expired={time_expired}, not_notified={not_notified}")
        
        # Query for tickets that have expired and have not been notified yet
        expired_tickets = Ticket.query.filter(
            Ticket.notified == False,
            (
                (Ticket.date < current_date) | 
                ((Ticket.date == current_date) & (Ticket.time <= current_time))
            )
        ).all()

        logger.info(f"Found {len(expired_tickets)} expired tickets at {now}")

        for ticket in expired_tickets:
            logger.info(f"Processing ticket ID {ticket.id} for user ID {ticket.user_id}")
            logger.debug(f"Ticket date/time: {ticket.date} {ticket.time}")
            
            # Konwersja czasu UTC na lokalny (Europe/Warsaw) dla logów i powiadomień
            utc_dt = datetime.combine(ticket.date, ticket.time).replace(tzinfo=timezone.utc)
            local_dt = utc_dt.astimezone(ZoneInfo("Europe/Warsaw"))
            formatted_time = local_dt.strftime("%H:%M")
            formatted_date = local_dt.strftime("%Y-%m-%d")
            
            logger.info(f"Ticket expired at: {formatted_date} {formatted_time} (local time)")
            
            # Send email notification about the expired ticket
            try:
                send_ticket_email(ticket)
                logger.info("Email sent for ticket.")
            except Exception as e:
                logger.error(f"Email error: {e}")

            # Retrieve the user associated with the ticket
            user = User.query.get(ticket.user_id)
            if user:
                logger.info(f"User found: {user.email}")
                # If user has an FCM token, send push notification
                if user.fcm_token:
                    logger.debug(f"Sending push notification to token: {user.fcm_token[:20]}...")
                    try:
                        # Używaj lokalnego czasu w powiadomieniu push
                        send_push_notification(
                            token=user.fcm_token,
                            title="Przeterminowany bilet parkingowy",
                            body=f"Bilet dla pojazdu {ticket.vehicle_number} z lokalizacji {ticket.location} stracił ważność dnia {formatted_date} o {formatted_time}."
                        )
                        logger.info("Push notification sent successfully.")
                    except Exception as e:
                        logger.error(f"Push notification error: {e}")
                else:
                    logger.info("User has no FCM token stored.")
            else:
                logger.info("User not found.")

            # Mark the ticket as notified to avoid duplicate notifications
            ticket.notified = True
            logger.info(f"Marked ticket {ticket.id} as notified.")

        # Commit changes to the database if any tickets were updated
        if expired_tickets:
            db.session.commit()
            logger.info(f"Updated {len(expired_tickets)} tickets as notified.")
        else:
            logger.info("No expired tickets found.")