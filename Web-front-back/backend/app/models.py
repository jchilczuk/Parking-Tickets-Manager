from flask_sqlalchemy import SQLAlchemy
from datetime import datetime

# Initialize SQLAlchemy instance for ORM
db = SQLAlchemy()

class User(db.Model):
    """
    User model for storing user account details.
    """
    id = db.Column(db.Integer, primary_key=True)                  # Unique user ID (primary key)
    email = db.Column(db.String(120), unique=True, nullable=False) # User's email address (must be unique)
    password = db.Column(db.String(512), nullable=False)           # Hashed password
    name = db.Column(db.String(30), nullable=False)                # User's first name
    surname = db.Column(db.String(30), nullable=False)             # User's last name
    fcm_token = db.Column(db.String(256), nullable=True)           # FCM token for push notifications (optional)

class Ticket(db.Model):
    """
    Ticket model for storing parking ticket details.
    """
    id = db.Column(db.Integer, primary_key=True)                   # Unique ticket ID (primary key)
    user_id = db.Column(db.Integer, db.ForeignKey('user.id'), nullable=False) # Reference to the user who owns the ticket
    vehicle_number = db.Column(db.String(20))                      # Vehicle registration number
    location = db.Column(db.String(100))                           # Parking location
    date = db.Column(db.Date)                                      # Expiration date of the ticket
    time = db.Column(db.Time)                                      # Expiration time of the ticket
    image_base64 = db.Column(db.Text)                              # Ticket image encoded as base64 string
    uploaded_at = db.Column(db.DateTime, default=datetime.utcnow)  # Timestamp when the ticket was uploaded
    notified = db.Column(db.Boolean, default=False)                # Whether the user has been notified about this ticket
