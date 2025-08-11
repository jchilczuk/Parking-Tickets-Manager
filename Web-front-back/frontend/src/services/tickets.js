import api from './api';

// Dodawanie biletu
export const uploadTicket = async (payload) => {
  try {
    const response = await api.post('/ticket', payload);
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.msg || 'Nie udało się dodać biletu');
  }
};

// Wyszukiwanie biletów
export const searchTickets = async (params = {}) => {
  try {
    const response = await api.get('/tickets', { params });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.msg || 'Nie udało się wyszukać biletu');
  }
};

// Usuwanie biletu
export const deleteTicket = async (id) => {
  try {
    const response = await api.delete(`/ticket/${id}`);
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.msg || 'Nie udało się usunąć biletu');
  }
};

// Pobieranie wszystkich biletów
export const getOldTickets = async () => {
  try {
    const response = await api.get('/tickets');
    console.log('Odpowiedź z /tickets:', response.data);
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.msg || 'Nie udało się pobrać przeterminowanych biletów');
  }
};

// Aktualizacja biletu (opcjonalnie z obrazem)
export const updateTicket = async (ticketId, ticketData, imageFile) => {
  let payload = { ...ticketData };

  if (imageFile === null) {
    payload.remove_image = true;
  } else if (imageFile) {
    const base64Image = await convertImageToBase64(imageFile);
    payload.image_base64 = base64Image;
  }

  try {
    const response = await api.put(`/ticket/${ticketId}`, payload);
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.msg || 'Nie udało się zaktualizować biletu');
  }
};

// Konwersja obrazu na base64
const convertImageToBase64 = (imageFile) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => resolve(reader.result.split(',')[1]);
    reader.onerror = reject;
    reader.readAsDataURL(imageFile);
  });
};
