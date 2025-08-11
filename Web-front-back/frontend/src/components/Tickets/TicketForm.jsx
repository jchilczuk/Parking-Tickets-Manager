import React, { useState } from 'react';
import { uploadTicket } from '../services/tickets';
import { fetchAndStoreFcmToken } from '../services/pushNotifications';

const AddTicketForm = () => {
  const [vehicleNumber, setVehicleNumber] = useState('');
  const [location, setLocation] = useState('');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [imageFile, setImageFile] = useState(null);

  // Pobierz obecną datę i czas w formacie lokalnym dla defaultowych wartości
  const getCurrentDateTime = () => {
    const now = new Date();
    const today = now.toISOString().split('T')[0]; // YYYY-MM-DD
    const currentTime = now.toTimeString().slice(0, 5); // HH:MM
    return { today, currentTime };
  };

  const { today, currentTime } = getCurrentDateTime();

  // Funkcja do konwersji pliku na base64
  const convertToBase64 = (file) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => {
        // Usuń prefix "data:image/jpeg;base64," i zostaw tylko base64
        const base64String = reader.result.split(',')[1];
        resolve(base64String);
      };
      reader.onerror = (error) => reject(error);
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      // Konwertuj zdjęcie na base64 jeśli zostało wybrane
      let imageBase64 = null;
      if (imageFile) {
        console.log('Konwertuję zdjęcie na base64...');
        imageBase64 = await convertToBase64(imageFile);
        console.log('Zdjęcie skonwertowane, rozmiar base64:', imageBase64.length);
      }

      // POPRAWNA konwersja lokalnego czasu na UTC
      const localDateTime = new Date(`${date}T${time}:00`);
      
      // DEBUG - sprawdź strefy czasowe
      console.log('Lokalny czas wprowadzony:', localDateTime.toString());
      console.log('Offset strefy czasowej (minuty):', localDateTime.getTimezoneOffset());
      console.log('Lokalny czas ISO:', localDateTime.toISOString());
      
      // JavaScript automatycznie konwertuje na UTC
      const utcDate = localDateTime.toISOString().split('T')[0]; // YYYY-MM-DD w UTC
      const utcTime = localDateTime.toISOString().split('T')[1].substring(0, 5); // HH:MM w UTC
      
      const payload = {
        vehicle_number: vehicleNumber,
        location: location,
        date: utcDate,
        time: utcTime,
        image_base64: imageBase64,
      };

      console.log('Wysyłam payload (POPRAWNA konwersja PL->UTC):', {
        'czas_lokalny': `${date} ${time}`,
        'czas_utc': `${payload.date} ${payload.time}`,
        'offset_minut': localDateTime.getTimezoneOffset(),
        image_base64: !!payload.image_base64
      });
      
      await uploadTicket(payload);
      await fetchAndStoreFcmToken();

      alert('Bilet dodany pomyślnie!');
      setVehicleNumber('');
      setLocation('');
      setDate('');
      setTime('');
      setImageFile(null);
    } catch (err) {
      console.error('[ERROR] Błąd podczas dodawania biletu:', err);
      alert('Wystąpił błąd podczas dodawania biletu.');
    }
  };

  const formStyles = {
    container: {
      maxWidth: '500px',
      margin: '20px auto',
      padding: '30px',
      backgroundColor: '#ffffff',
      borderRadius: '12px',
      boxShadow: '0 4px 6px rgba(0, 0, 0, 0.1)',
      fontFamily: 'Arial, sans-serif'
    },
    title: {
      textAlign: 'center',
      marginBottom: '30px',
      color: '#333',
      fontSize: '28px',
      fontWeight: 'bold'
    },
    fieldGroup: {
      marginBottom: '20px',
      position: 'relative'
    },
    label: {
      display: 'block',
      marginBottom: '8px',
      fontWeight: '600',
      color: '#555',
      fontSize: '14px'
    },
    input: {
      width: '100%',
      padding: '12px 16px',
      border: '2px solid #e1e5e9',
      borderRadius: '8px',
      fontSize: '16px',
      transition: 'border-color 0.3s ease, box-shadow 0.3s ease',
      boxSizing: 'border-box',
      backgroundColor: '#fafafa'
    },
    inputFocus: {
      borderColor: '#4A90E2',
      boxShadow: '0 0 0 3px rgba(74, 144, 226, 0.1)',
      backgroundColor: '#ffffff',
      outline: 'none'
    },
    fileInput: {
      width: '100%',
      padding: '12px 16px',
      border: '2px dashed #ccc',
      borderRadius: '8px',
      fontSize: '14px',
      backgroundColor: '#f9f9f9',
      cursor: 'pointer',
      transition: 'border-color 0.3s ease',
      boxSizing: 'border-box'
    },
    fileInputHover: {
      borderColor: '#4A90E2',
      backgroundColor: '#f0f8ff'
    },
    button: {
      width: '100%',
      padding: '14px',
      backgroundColor: '#4A90E2',
      color: 'white',
      border: 'none',
      borderRadius: '8px',
      fontSize: '16px',
      fontWeight: '600',
      cursor: 'pointer',
      transition: 'background-color 0.3s ease, transform 0.1s ease',
      marginTop: '10px'
    },
    buttonHover: {
      backgroundColor: '#357abd',
      transform: 'translateY(-1px)'
    },
    helper: {
      fontSize: '12px',
      color: '#666',
      marginTop: '4px',
      fontStyle: 'italic'
    }
  };

  return (
    <div style={formStyles.container}>
      <h2 style={formStyles.title}>Dodaj nowy bilet</h2>
      
      <form onSubmit={handleSubmit}>
        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Numer rejestracyjny:</label>
          <input 
            type="text" 
            value={vehicleNumber}
            onChange={(e) => setVehicleNumber(e.target.value)}
            placeholder="Numer rejestracyjny"
            style={formStyles.input}
            onFocus={(e) => Object.assign(e.target.style, formStyles.inputFocus)}
            onBlur={(e) => Object.assign(e.target.style, formStyles.input)}
            required 
          />
        </div>

        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Lokalizacja:</label>
          <input 
            type="text" 
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            placeholder="Lokalizacja"
            style={formStyles.input}
            onFocus={(e) => Object.assign(e.target.style, formStyles.inputFocus)}
            onBlur={(e) => Object.assign(e.target.style, formStyles.input)}
            required 
          />
        </div>

        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Data:</label>
          <input 
            type="date" 
            value={date}
            onChange={(e) => setDate(e.target.value)}
            style={formStyles.input}
            onFocus={(e) => Object.assign(e.target.style, formStyles.inputFocus)}
            onBlur={(e) => Object.assign(e.target.style, formStyles.input)}
            required 
          />
        </div>

        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Godzina:</label>
          <input 
            type="time" 
            value={time}
            onChange={(e) => setTime(e.target.value)}
            style={formStyles.input}
            onFocus={(e) => Object.assign(e.target.style, formStyles.inputFocus)}
            onBlur={(e) => Object.assign(e.target.style, formStyles.input)}
            required 
          />
        </div>

        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Zdjęcie (opcjonalne):</label>
          <input 
            type="file" 
            accept="image/*"
            onChange={(e) => setImageFile(e.target.files[0])}
            style={formStyles.fileInput}
            onMouseOver={(e) => Object.assign(e.target.style, formStyles.fileInputHover)}
            onMouseOut={(e) => Object.assign(e.target.style, formStyles.fileInput)}
          />
          {imageFile && (
            <div style={{ ...formStyles.helper, color: '#4A90E2', fontWeight: '600' }}>
              ✓ Wybrano: {imageFile.name} ({Math.round(imageFile.size / 1024)} KB)
            </div>
          )}
        </div>

        <button 
          type="submit"
          style={formStyles.button}
          onMouseOver={(e) => Object.assign(e.target.style, {...formStyles.button, ...formStyles.buttonHover})}
          onMouseOut={(e) => Object.assign(e.target.style, formStyles.button)}
        >
          Dodaj bilet
        </button>
      </form>
    </div>
  );
};

export default AddTicketForm;