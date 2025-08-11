import os
from dotenv import load_dotenv

# Load environment variables from a .env file if present
load_dotenv()

class Config:
    # Database connection URI; fallback to local PostgreSQL if not set in environment
    SQLALCHEMY_DATABASE_URI = os.environ.get("DATABASE_URL", "postgresql://user:password@localhost:5432/parkingdb")
    # Disable SQLAlchemy event system to save resources (recommended unless needed)
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    # Secret key for JWT token signing; should be kept secret in production
    JWT_SECRET_KEY = os.environ.get("JWT_SECRET_KEY", "secret-key")

    # Email server configuration for Flask-Mail
    MAIL_SERVER = os.environ.get("MAIL_SERVER", "smtp.poczta.onet.pl")  # SMTP server address
    MAIL_PORT = int(os.environ.get("MAIL_PORT", 587))                   # SMTP port (default 587 for TLS)
    MAIL_USE_TLS = os.environ.get("MAIL_USE_TLS", "True") == "True"     # Use TLS for secure email sending
    MAIL_USERNAME = os.environ.get("MAIL_USERNAME")                     # Email account username
    MAIL_PASSWORD = os.environ.get("MAIL_PASSWORD")                     # Email account password
    MAIL_DEFAULT_SENDER = os.environ.get("MAIL_DEFAULT_SENDER")         # Default sender address for outgoing emails
