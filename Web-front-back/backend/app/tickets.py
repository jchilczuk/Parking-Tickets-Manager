from flask import Blueprint, request, jsonify, make_response
from flask_jwt_extended import jwt_required, get_jwt_identity
from app.models import db, Ticket
from datetime import datetime
from sqlalchemy import extract
import re
from datetime import time

# Create a Blueprint for ticket-related routes
tickets_bp = Blueprint('tickets', __name__)

@tickets_bp.route("/ticket", methods=["POST"])
@jwt_required()
def upload_ticket():
    """
    Upload a new parking ticket for the authenticated user.
    Expects JSON with 'vehicle_number', 'location', 'date', 'time', and optionally 'image_base64'.
    Validates required fields and date/time format.
    """
    data = request.get_json()    

    # Check for missing required fields
    required_fields = ["vehicle_number", "location", "date", "time"]
    missing = [field for field in required_fields if not data.get(field)]
    if missing:
        return jsonify({"msg": f"Brakuje wymaganych pól: {', '.join(missing)}"}), 400
    
    try:
        # Parse and create a new Ticket object
        ticket = Ticket(
            user_id=int(get_jwt_identity()),
            vehicle_number=data['vehicle_number'],
            location=data['location'],
            date=datetime.strptime(data['date'], "%Y-%m-%d").date(),
            time=datetime.strptime(data['time'], "%H:%M").time(),
            image_base64 = data.get("image_base64")
        )
    except ValueError:
        # Handle invalid date or time format
        return jsonify({"msg": "Nieprawidłowy format daty lub godziny."}), 400

    # Save the ticket to the database
    db.session.add(ticket)
    db.session.commit()

    return jsonify({
        "msg": "Ticket uploaded",
        "id": ticket.id
    }), 201

@tickets_bp.route("/tickets", methods=["GET"])
@jwt_required()
def search_tickets():
    """
    Search for tickets belonging to the authenticated user.
    Supports filtering by date, location, vehicle_number, and time (including partial time).
    Returns a list of matching tickets.
    """
    try:
        user_id = int(get_jwt_identity())
        filters = {k: v for k, v in request.args.items()}
        query = Ticket.query.filter_by(user_id=user_id)

        # Filter by date if provided
        if "date" in filters and filters["date"]:
            query = query.filter(Ticket.date == filters["date"])
        # Filter by location (case-insensitive, partial match)
        if "location" in filters and filters["location"]:
            query = query.filter(Ticket.location.ilike(f"%{filters['location']}%"))
        # Filter by vehicle number (case-insensitive, partial match)
        if "vehicle_number" in filters and filters["vehicle_number"]:
            query = query.filter(Ticket.vehicle_number.ilike(f"%{filters['vehicle_number']}%"))
        # Flexible filtering by time (exact or by hour)
        if "time" in filters and filters["time"]:
            time_str = filters["time"]

            if re.match(r"^\d{2}:\d{2}$", time_str):  # Exact time HH:MM
                query = query.filter(Ticket.time == time_str)

            elif re.match(r"^\d{2}:(--)?$", time_str):  # Hour only, e.g. 11:-- or 11:
                hour = int(time_str[:2])
                query = query.filter(extract('hour', Ticket.time) == hour)

            elif re.match(r"^\d{2}$", time_str):  # Only hour, e.g. 11
                hour = int(time_str)
                query = query.filter(extract('hour', Ticket.time) == hour)

        # Execute the query and return results
        results = query.all()

        return jsonify([{
            "id": t.id,
            "vehicle_number": t.vehicle_number,
            "location": t.location,
            "date": str(t.date),
            "time": str(t.time),
            "image_base64": t.image_base64
        } for t in results]), 200
    
    except Exception as e:
        # Log and handle unexpected errors
        import traceback
        print("Error in /tickets:", traceback.format_exc())
        return jsonify({"msg": "Server error fetching tickets"}), 500

@tickets_bp.route("/ticket/<int:ticket_id>", methods=["GET"])
@jwt_required()
def get_ticket_by_id(ticket_id):
    """
    Retrieve a single ticket by its ID for the authenticated user.
    Returns ticket details if found, otherwise 404.
    """
    user_id = int(get_jwt_identity())
    ticket = Ticket.query.filter_by(id=ticket_id, user_id=user_id).first()

    if not ticket:
        return jsonify({"msg": "Ticket not found"}), 404

    ticket_data = {
        "id": ticket.id,
        "vehicle_number": ticket.vehicle_number,
        "location": ticket.location,
        "date": str(ticket.date),
        "time": str(ticket.time)
    }

    response = make_response(jsonify(ticket_data), 200)
    response.headers["Content-Type"] = "application/json; charset=utf-8"
    return response

@tickets_bp.route("/ticket/<int:ticket_id>/image", methods=["GET"])
@jwt_required()
def get_ticket_image(ticket_id):
    """
    Retrieve the base64-encoded image for a specific ticket.
    Returns 404 if ticket or image is not found.
    """
    user_id = int(get_jwt_identity())
    ticket = Ticket.query.filter_by(id=ticket_id, user_id=user_id).first()

    if not ticket or not ticket.image_base64:
        return jsonify({"msg": "Image not found"}), 404
    
    return jsonify({"image_base64": ticket.image_base64}), 200

@tickets_bp.route("/ticket/<int:ticket_id>", methods=["DELETE"])
@jwt_required()
def delete_ticket(ticket_id):
    """
    Delete a ticket by its ID for the authenticated user.
    Returns a success message if deleted, or 404 if not found or not owned by user.
    """
    user_id = int(get_jwt_identity())
    ticket = Ticket.query.filter_by(id=ticket_id, user_id=user_id).first()

    if not ticket:
        return jsonify({"msg": "Nie znaleziono biletu lub brak dostępu."}), 404

    db.session.delete(ticket)
    db.session.commit()
    return jsonify({"msg": f"Bilet {ticket_id} został usunięty."}), 200
