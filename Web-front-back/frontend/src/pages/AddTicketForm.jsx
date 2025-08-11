import React, { useState } from 'react';
import { uploadTicket } from '../services/tickets';
import { fetchAndStoreFcmToken } from '../services/pushNotifications';

const AddTicketForm = () => {
  const [vehicleNumber, setVehicleNumber] = useState('');
  const [location, setLocation] = useState('');
  const [date, setDate] = useState('');
  const [time, setTime] = useState('');
  const [imageFile, setImageFile] = useState(null);
  const [error, setError] = useState('');

  // Pobierz obecną datę i czas w formacie lokalnym dla defaultowych wartości
  const getCurrentDateTime = () => {
    const now = new Date();
    const today = now.toISOString().split('T')[0]; // YYYY-MM-DD
    const currentTime = now.toTimeString().slice(0, 5); // HH:MM
    return { today, currentTime };
  };

  // Walidacja daty
  const validateDate = (selectedDate) => {
    if (!selectedDate) {
      return 'Data jest wymagana';
    }

    const { today } = getCurrentDateTime();
    const currentYear = new Date().getFullYear();
    const selectedYear = new Date(selectedDate).getFullYear();
    
    // Sprawdź czy rok ma 4 cyfry i jest poprawny
    if (isNaN(selectedYear) || selectedYear < 1000 || selectedYear > 9999) {
      return 'Rok musi składać się z 4 cyfr';
    }
    
    // Sprawdź czy rok jest >= aktualny rok
    if (selectedYear < currentYear) {
      return `Rok nie może być wcześniejszy niż ${currentYear}`;
    }
    
    // Sprawdź czy rok nie jest za daleko w przyszłości
    if (selectedYear > currentYear + 10) {
      return `Rok nie może być późniejszy niż ${currentYear + 10}`;
    }
    
    // Sprawdź czy data nie jest z przeszłości
    if (selectedDate < today) {
      return 'Nie można dodawać biletów z przeszłą datą';
    }
    
    return null;
  };

  // Walidacja czasu
  const validateTime = (selectedDate, selectedTime) => {
    if (!selectedTime) {
      return 'Godzina jest wymagana';
    }

    // Sprawdź format czasu
    const timeRegex = /^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$/;
    if (!timeRegex.test(selectedTime)) {
      return 'Niepoprawny format godziny (wymagany HH:MM)';
    }

    const { today, currentTime } = getCurrentDateTime();
    
    // Jeśli to dzisiejsza data, sprawdź czy czas nie jest z przeszłości
    if (selectedDate === today && selectedTime <= currentTime) {
      return `Dla dzisiejszej daty godzina musi być późniejsza niż ${currentTime}`;
    }
    
    return null;
  };

  // Walidacja nazwy pojazdu
  const validateVehicleNumber = (vehicleNumber) => {
    if (!vehicleNumber || vehicleNumber.trim().length === 0) {
      return 'Numer rejestracyjny jest wymagany';
    }
    
    if (vehicleNumber.trim().length < 2) {
      return 'Numer rejestracyjny musi mieć co najmniej 2 znaki';
    }
    
    if (vehicleNumber.trim().length > 20) {
      return 'Numer rejestracyjny nie może mieć więcej niż 20 znaków';
    }
    
    return null;
  };

  // Walidacja lokalizacji
  const validateLocation = (location) => {
    if (!location || location.trim().length === 0) {
      return 'Lokalizacja jest wymagana';
    }
    
    if (location.trim().length < 3) {
      return 'Lokalizacja musi mieć co najmniej 3 znaki';
    }
    
    if (location.trim().length > 100) {
      return 'Lokalizacja nie może mieć więcej niż 100 znaków';
    }
    
    return null;
  };

  // Walidacja pliku zdjęcia
  const validateImageFile = (imageFile) => {
    if (!imageFile) {
      return null; // Zdjęcie jest opcjonalne
    }
    
    // Sprawdź typ pliku
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
    if (!allowedTypes.includes(imageFile.type)) {
      return 'Dozwolone są tylko pliki: JPG, JPEG, PNG, GIF';
    }
    
    // Sprawdź rozmiar pliku (max 5MB)
    const maxSizeInBytes = 5 * 1024 * 1024; // 5MB
    if (imageFile.size > maxSizeInBytes) {
      return 'Plik nie może być większy niż 5MB';
    }
    
    return null;
  };

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
    setError('');

    // Walidacja wszystkich pól
    const vehicleError = validateVehicleNumber(vehicleNumber);
    if (vehicleError) {
      setError(vehicleError);
      return;
    }

    const locationError = validateLocation(location);
    if (locationError) {
      setError(locationError);
      return;
    }

    const dateError = validateDate(date);
    if (dateError) {
      setError(dateError);
      return;
    }

    const timeError = validateTime(date, time);
    if (timeError) {
      setError(timeError);
      return;
    }

    const imageError = validateImageFile(imageFile);
    if (imageError) {
      setError(imageError);
      return;
    }

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
      setError('');
    } catch (err) {
      console.error('[ERROR] Błąd podczas dodawania biletu:', err);

      if (err.response?.status === 401) {
        setError('Nie jesteś zalogowany. Zaloguj się ponownie.');
      } else if (err.response?.status === 400) {
        const backendMsg = err.response?.data?.msg || 'Błąd walidacji danych.';
        setError(backendMsg);
      } else if (!err.response) {
        setError('Brak połączenia z serwerem.');
      } else {
        setError('Wystąpił nieznany błąd podczas dodawania biletu.');
      }
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
    error: {
      backgroundColor: '#fee',
      border: '1px solid #fcc',
      color: '#c33',
      padding: '12px',
      borderRadius: '8px',
      marginBottom: '20px',
      fontSize: '14px',
      fontWeight: '500'
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
    },
    validationHelper: {
      fontSize: '12px',
      color: '#4A90E2',
      marginTop: '4px',
      fontWeight: '500'
    }
  };

  // Ustaw minimalną datę na dzisiaj i maksymalny rok
  const { today } = getCurrentDateTime();
  const currentYear = new Date().getFullYear();
  const maxDate = `${currentYear + 10}-12-31`;

  return (
    <div style={formStyles.container}>
      <h2 style={formStyles.title}>Dodaj nowy bilet</h2>
      
      {error && (
        <div style={formStyles.error}>
          {error}
        </div>
      )}
      
      <form onSubmit={handleSubmit}>
        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Numer rejestracyjny:</label>
          <input 
            type="text" 
            value={vehicleNumber}
            onChange={(e) => setVehicleNumber(e.target.value)}
            placeholder="np. ABC123, WA1234AB"
            maxLength="20"
            style={formStyles.input}
            onFocus={(e) => Object.assign(e.target.style, formStyles.inputFocus)}
            onBlur={(e) => Object.assign(e.target.style, formStyles.input)}
            required 
          />
          <div style={formStyles.validationHelper}>
            Wymagane: 2-20 znaków
          </div>
        </div>

        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Lokalizacja:</label>
          <input 
            type="text" 
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            placeholder="np. Centrum handlowe, ul. Główna 15"
            maxLength="100"
            style={formStyles.input}
            onFocus={(e) => Object.assign(e.target.style, formStyles.inputFocus)}
            onBlur={(e) => Object.assign(e.target.style, formStyles.input)}
            required 
          />
          <div style={formStyles.validationHelper}>
            Wymagane: 3-100 znaków
          </div>
        </div>

        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Data wygaśnięcia:</label>
          <input 
            type="date" 
            value={date}
            onChange={(e) => setDate(e.target.value)}
            min={today}
            max={maxDate}
            style={formStyles.input}
            onFocus={(e) => Object.assign(e.target.style, formStyles.inputFocus)}
            onBlur={(e) => Object.assign(e.target.style, formStyles.input)}
            required 
          />
          <div style={formStyles.validationHelper}>
            Minimalna data: dzisiaj, maksymalny rok: {currentYear + 10}
          </div>
        </div>

        <div style={formStyles.fieldGroup}>
          <label style={formStyles.label}>Godzina wygaśnięcia:</label>
          <input 
            type="time" 
            value={time}
            onChange={(e) => setTime(e.target.value)}
            style={formStyles.input}
            onFocus={(e) => Object.assign(e.target.style, formStyles.inputFocus)}
            onBlur={(e) => Object.assign(e.target.style, formStyles.input)}
            required 
          />
          {date === today && (
            <div style={formStyles.validationHelper}>
              Dla dzisiejszej daty: minimalna godzina to aktualna godzina
            </div>
          )}
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
          <div style={formStyles.validationHelper}>
            Dozwolone: JPG, JPEG, PNG, GIF (max 5MB)
          </div>
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