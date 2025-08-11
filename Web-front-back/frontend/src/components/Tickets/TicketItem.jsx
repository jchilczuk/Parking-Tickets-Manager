import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api';
import './TicketItem.css';

const TicketItem = ({ ticket, onDelete }) => {
  const navigate = useNavigate();
  const [imageBase64, setImageBase64] = useState(null);
  const [loadingImage, setLoadingImage] = useState(false);
  const [showImage, setShowImage] = useState(false);

  const fetchImage = async () => {
    try {
      setLoadingImage(true);
      const token = localStorage.getItem('token');
      const response = await api.get(`/ticket/${ticket.id}/image`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setImageBase64(response.data.image_base64);
      setShowImage(true);
    } catch (err) {
      console.error('Błąd pobierania zdjęcia:', err);
      alert('Nie udało się załadować zdjęcia.');
    } finally {
      setLoadingImage(false);
    }
  };

  const handleToggleImage = () => {
    if (showImage) {
      setShowImage(false);
    } else {
      fetchImage();
    }
  };

  // POPRAWNA funkcja do formatowania daty i czasu - konwersja UTC->lokalny
  const formatDateTime = (dateString, timeString) => {
    if (!dateString || !timeString) {
      return 'Brak daty/czasu';
    }
    
    try {
      // Upewnij się, że timeString ma format HH:MM:SS
      const normalizedTime = timeString.length === 5 ? `${timeString}:00` : timeString;
      
      // Twórz datę UTC (dane z bazy są w UTC)
      const utcDateTime = new Date(`${dateString}T${normalizedTime}.000Z`);
      
      // Sprawdź czy data jest prawidłowa
      if (isNaN(utcDateTime.getTime())) {
        return `${dateString} ${timeString}`;
      }
      
      // DEBUG
      console.log(`[DEBUG] UTC from DB: ${dateString}T${normalizedTime}`);
      console.log(`[DEBUG] UTC Date object: ${utcDateTime.toISOString()}`);
      console.log(`[DEBUG] Local time: ${utcDateTime.toString()}`);
      
      // JavaScript automatycznie konwertuje UTC na lokalny czas
      const localDate = utcDateTime.toLocaleDateString('pl-PL');
      const localTime = utcDateTime.toLocaleTimeString('pl-PL', { 
        hour: '2-digit', 
        minute: '2-digit' 
      });
      
      console.log(`[DEBUG] Formatted local: ${localDate} ${localTime}`);
      
      return `${localDate} ${localTime}`;
      
    } catch (error) {
      console.error('Error in formatDateTime:', error);
      // Fallback - zwróć oryginalne wartości
      return `${dateString} ${timeString}`;
    }
  };

  const detectMimeType = (base64) => {
    if (!base64) return 'image/jpeg';
    const prefix = base64.substring(0, 5);
    if (prefix.startsWith('/9j/')) return 'image/jpeg';
    if (prefix.startsWith('iVBOR')) return 'image/png';
    return 'image/jpeg';
  };

  return (
    <div className="ticket-item-wrapper">
      <div className="ticket-item" id={`ticket-${ticket.id}`}>
        <div className="ticket-header" style={{ 
          display: 'flex', 
          justifyContent: 'space-between', 
          alignItems: 'flex-start',
          marginBottom: '15px' 
        }}>
          <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold' }}>{ticket.vehicle_number}</h3>
          <span className="ticket-date" style={{ 
            fontSize: '14px', 
            color: '#666',
            textAlign: 'right',
            lineHeight: '1.2',
            fontWeight: '500'
          }}>
            {formatDateTime(ticket.date, ticket.time)}
          </span>
        </div>

        <div className="ticket-location">
          <strong>Lokalizacja:</strong> {ticket.location}
        </div>

        <div className="ticket-image-section" style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: '10px', marginTop: '10px' }}>
          <button onClick={handleToggleImage}>
            {loadingImage ? 'Ładowanie...' : showImage ? 'Ukryj zdjęcie' : 'Pokaż zdjęcie'}
          </button>

          {showImage && imageBase64 && (
            <img
              src={`data:${detectMimeType(imageBase64)};base64,${imageBase64}`}
              alt="Zdjęcie biletu"
              className="ticket-image"
              style={{ maxWidth: '300px', width: '100%', height: 'auto', borderRadius: '8px' }}
            />
          )}
        </div>
      </div>

      <div className="ticket-actions">
        <button onClick={() => onDelete(ticket.id)} className="btn-delete">Usuń</button>
      </div>
    </div>
  );
};

export default TicketItem;