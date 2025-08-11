import React from 'react';

const TicketCard = ({ ticket }) => {
  return (
    <div className="ticket-card">
      <p><strong>{ticket.vehicle_number}</strong> – {ticket.location} ({ticket.date} {ticket.time})</p>

      {/* Podgląd obrazu*/}
      {ticket.image_base64 && (
        <img
          src={`data:image/jpeg;base64,${ticket.image_base64}`}
          alt="Podgląd biletu"
          style={{ width: '150px', marginTop: '10px' }}
        />
      )}
    </div>
  );
};

export default TicketCard;
