import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { searchTickets, deleteTicket } from '../../services/tickets';
import TicketItem from './TicketItem';
import LoadingSpinner from '../common/LoadingSpinner';
import ErrorModal from '../common/ErrorModal';

const TicketList = () => {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchParams, setSearchParams] = useState({
    vehicle_number: '',
    location: '',
    date: '',
    time: '' 
  });

  useEffect(() => {
    fetchTickets();
  }, []);

  const fetchTickets = async (params = {}) => {
    setLoading(true);
    try {
      const data = await searchTickets(params);
      setTickets(data);
    } catch (err) {
      setError(err.response?.data?.msg || 'Nie udało się pobrać biletów');
    } finally {
      setLoading(false);
    }
  };

  const handleSearchChange = (e) => {
    const { name, value } = e.target;
    setSearchParams(prev => ({ ...prev, [name]: value }));
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchTickets(searchParams);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Czy na pewno chcesz usunąć ten bilet?')) {
      try {
        await deleteTicket(id);
        setTickets(prev => prev.filter(ticket => ticket.id !== id));
      } catch (err) {
        setError('Nie udało się usunąć biletu');
      }
    }
  };

  const clearFilters = () => {
    setSearchParams({
      vehicle_number: '',
      location: '',
      date: '',
      time: ''
    });
    fetchTickets();
  };

  return (
    <div className="ticket-list">
      <div className="ticket-actions">
        <Link to="/tickets/new" className="btn-primary">
          Dodaj nowy bilet
        </Link>
      </div>

      <div className="search-box">
        <h3>Wyszukaj bilety</h3>
        <form onSubmit={handleSearchSubmit}>
          <div className="search-fields">
            <input
              type="text"
              name="vehicle_number"
              placeholder="Numer pojazdu"
              value={searchParams.vehicle_number}
              onChange={handleSearchChange}
            />
            <input
              type="text"
              name="location"
              placeholder="Lokalizacja"
              value={searchParams.location}
              onChange={handleSearchChange}
            />
            <input
              type="date"
              name="date"
              value={searchParams.date}
              onChange={handleSearchChange}
            />
            <input
              type="time"
              name="time"
              value={searchParams.time}
              onChange={handleSearchChange}
            />
          </div>
          <div className="search-buttons">
            <button type="submit" className="btn-primary">
              Szukaj
            </button>
            <button
              type="button"
              onClick={clearFilters}
              className="btn-secondary"
            >
              Wyczyść filtry
            </button>
          </div>
        </form>
      </div>

      {loading ? (
        <LoadingSpinner />
      ) : error ? (
        <ErrorModal message={error} onClose={() => setError(null)} />
      ) : tickets.length === 0 ? (
        <div className="no-results">
          <p>Nie znaleziono biletów spełniających kryteria</p>
        </div>
      ) : (
        <div className="tickets-container">
          {tickets.map(ticket => (
            <TicketItem
              key={ticket.id}
              ticket={ticket}
              onDelete={handleDelete}
            />
          ))}
        </div>
      )}
    </div>
  );
};

export default TicketList;