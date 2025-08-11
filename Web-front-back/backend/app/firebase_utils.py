# Utility functions for Firebase push notifications

import firebase_admin
from firebase_admin import credentials, messaging
import os
import logging
from dotenv import load_dotenv

# Konfiguracja loggera
logger = logging.getLogger(__name__)

# Load environment variables from .env file
load_dotenv()

logger.info(f"FIREBASE_KEY_PATH = {os.getcwd()+os.getenv('FIREBASE_KEY_PATH')}")

# Load Firebase credentials from the path specified in environment variable
if os.getenv("FLASK_ENV") != "testing":
    cred_path = os.getenv("FIREBASE_KEY_PATH")
    if cred_path:
        cred = credentials.Certificate(os.getcwd()+os.getenv("FIREBASE_KEY_PATH"))
        firebase_admin.initialize_app(cred)


def send_push_notification(token, title, body):
    """
    Sends a push notification to a device using Firebase Cloud Messaging (FCM).
    Args:
        token (str): FCM device token.
        title (str): Notification title.
        body (str): Notification body.
    """
    logger.debug(f"Wysyłam FCM do tokena: {token[:50]}...")
    logger.debug(f"Title: {title}, Body: {body}")
    
    message = messaging.Message(
        notification=messaging.Notification(
            title=title,
            body=body,
        ),
        token=token,
    )
    try:
        # Send the message via Firebase
        response = messaging.send(message)
        logger.info(f"FCM SUCCESS: {response}")
    except Exception as e:
        # Log error if sending fails
        logger.error(f"FCM ERROR: {e}")
        logger.error(f"Error type: {type(e)}")
        logger.error(f"Token używany: {token[:50]}...")

# Debug - sprawdź project_id z klucza
def debug_firebase_config():
    """Debug function to check Firebase configuration"""
    try:
        import json
        key_path = os.getcwd() + os.getenv("FIREBASE_KEY_PATH")
        with open(key_path, 'r') as f:
            key_data = json.load(f)
            logger.info(f"Firebase project_id z klucza: {key_data.get('project_id')}")
    except Exception as e:
        logger.error(f"Błąd czytania klucza Firebase: {e}")

# Wywołaj debug przy importie
debug_firebase_config()
