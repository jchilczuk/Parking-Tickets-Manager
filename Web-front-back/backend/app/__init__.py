from flask import Flask
from flask_cors import CORS
from flask_apscheduler import APScheduler
from app.models import db
from flask_jwt_extended import JWTManager
from app.auth import auth_bp
from app.tickets import tickets_bp
from app.extensions import mail
from app.notifications import check_expired_tickets 

import logging
logging.basicConfig(level=logging.DEBUG)

# Initialize scheduler and JWT manager (used globally)
scheduler = APScheduler()
jwt = JWTManager()

def create_app(test_config=None):
    # Create Flask app instance
    app = Flask(__name__)
    if test_config:
        # Use test configuration if provided
        app.config.update(test_config)
    else:
        # Load default configuration
        app.config.from_object("app.config.Config")

    # Initialize Flask extensions with app context
    db.init_app(app)
    mail.init_app(app)
    jwt.init_app(app)
    CORS(app)  # Enable Cross-Origin Resource Sharing

    # Register authentication and ticket blueprints
    app.register_blueprint(auth_bp)
    app.register_blueprint(tickets_bp)

    # Ensure all responses have a Content-Type header
    @app.after_request
    def set_default_headers(response):
        if not response.content_type:
            response.headers['Content-Type'] = 'application/json'
        return response

    # Only start scheduler if not in testing mode
    if not app.config.get("TESTING"): 
        scheduler.init_app(app)
        scheduler.start()

        # Schedule periodic job to check for expired tickets every 3 minutes
        scheduler.add_job(
            id='check_expired_tickets_job',
            func=lambda: check_expired_tickets(app),
            trigger='interval',
            minutes=3,
            misfire_grace_time=30,  # Tolerate short delays in job execution
            coalesce=True           # Prevent multiple runs if jobs were missed
        )

    logging.info("[DEBUG] Scheduler wystartował")  # Log scheduler start

    return app
