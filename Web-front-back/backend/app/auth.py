from flask import Blueprint, request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from app.models import db, User
import re

# Create a Blueprint for authentication-related routes, with URL prefix /auth
auth_bp = Blueprint('auth', __name__, url_prefix="/auth")

# Regular expression for validating email addresses
EMAIL_REGEX = r"^[\w\.-]+@[\w\.-]+\.\w+$"

@auth_bp.route("/register", methods=["POST"])
def register():
    """
    Register a new user.
    Expects JSON with 'email', 'password', 'name', and 'surname'.
    Validates input, checks for existing user, hashes password, and saves user to DB.
    """
    data = request.get_json()
    # Check if all required fields are present
    if not data or 'email' not in data or 'password' not in data or 'name' not in data or 'surname' not in data:
        return jsonify({"msg": "Niepoprawne dane"}), 400

    email = data['email'].strip()
    password = data['password'].strip()        

    # Validate email format
    if not re.match(EMAIL_REGEX, email):
        return jsonify({"msg": "Niepoprawny format adresu e-mail"}), 400

    # Validate password length
    if len(password) < 5:
        return jsonify({"msg": "Hasło musi zawierać co najmniej 5 znaków"}), 400

    # Check if a user with this email already exists
    if User.query.filter_by(email=email).first():
        return jsonify({"msg": "Użytkownik o podanym adresie e-mail już istnieje"}), 409

    # Hash the password for secure storage
    hashed_pw = generate_password_hash(password)
    # Create a new user instance
    new_user = User(email=email, password=hashed_pw, name=data['name'], surname=data['surname'])
    db.session.add(new_user)
    db.session.commit()

    return jsonify({"msg": "Zarejestrowano pomyślnie"}), 201

@auth_bp.route("/login", methods=["POST"])
def login():
    """
    Log in a user.
    Expects JSON with 'email' and 'password'.
    Checks credentials and returns JWT access token if valid.
    """
    data = request.get_json()
    # Check if required fields are present
    if not data or 'email' not in data or 'password' not in data:
        return jsonify({"msg": "Niepoprawne dane"}), 400

    # Find user by email
    user = User.query.filter_by(email=data['email']).first()
    # Check password hash
    if user and check_password_hash(user.password, data['password']):
        # Create JWT token with user id as identity
        token = create_access_token(identity=str(user.id))
        return jsonify(
            access_token=token,
            name=user.name,
            surname=user.surname
        )

    # Invalid credentials
    return jsonify({"msg": "Niepoprawne dane"}), 401

@auth_bp.route("/register_token", methods=["POST"])
@jwt_required()
def register_token():
    """
    Register or update a user's FCM token for push notifications.
    Requires JWT authentication.
    Expects JSON with 'fcm_token'.
    """
    # Get user id from JWT token
    user_id = int(get_jwt_identity())   
    data = request.get_json()
    token = data.get("fcm_token")
    
    # Check if token is provided
    if not token:
        print(f"[DEBUG] No token provided for user_id={user_id}")
        return jsonify({"msg": "Missing token"}), 400

    # Find user by id
    user = db.session.get(User, user_id)
    if user:
        # Update user's FCM token
        user.fcm_token = token
        db.session.commit()
        print(f"[DEBUG] Token updated for user {user.email}: {token}")
        return jsonify({"msg": "Token updated"}), 200

    # User not found
    print(f"[DEBUG] User not found for ID {user_id}")
    return jsonify({"msg": "User not found"}), 404