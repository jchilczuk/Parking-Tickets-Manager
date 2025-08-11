import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { register } from '../../services/auth';
import LoadingSpinner from '../common/LoadingSpinner';
import './Auth.css';

const Register = () => {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    name: '',
    surname: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      await register(formData);
      navigate('/login');
    } catch (err) {
      setError(err.message || 'Nie udało się zarejestrować (plik register)');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <h2>Rejestracja</h2>
      {error && <div className="error-message">{error}</div>}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor='email'>Email</label>
          <input
            type="email"
            name="email"
            id="email"
            value={formData.email}
            onChange={handleChange}
            required
            placeholder='Adres e-mail'
          />
        </div>
        <div className="form-group">
          <label htmlFor='password'>Hasło</label>
          <input
            type="password"
            name="password"
            id="password"
            value={formData.password}
            onChange={handleChange}
            required
            placeholder='Hasło'
          />
        </div>
        <div className="form-group">
          <label htmlFor='name'>Imię</label>
          <input
            type="text"
            name="name"
            id='name'
            value={formData.name}
            onChange={handleChange}
            required
            placeholder='Imię'
          />
        </div>
        <div className="form-group">
          <label htmlFor='surname'>Nazwisko</label>
          <input
            type="text"
            name="surname"
            id='surname'
            value={formData.surname}
            onChange={handleChange}
            required
            placeholder='Nazwisko'
          />
        </div>
        <button type="submit" disabled={loading}>
          {loading ? <LoadingSpinner small /> : 'Zarejestruj się'}
        </button>
      </form>
      <p className="auth-switch">
        Masz już konto? <a href="/login">Zaloguj się</a>
      </p>
    </div>
  );
};

export default Register;