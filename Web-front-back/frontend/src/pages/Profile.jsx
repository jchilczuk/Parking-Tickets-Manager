import React, { useState, useEffect } from 'react';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import { useNavigate } from 'react-router-dom';
import './Profile.css';

const Profile = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const storedUser = JSON.parse(localStorage.getItem('user'));
    if (storedUser) {
      setUser(storedUser);
    }
  }, []);

  const handleLogout = () => {
    setLoading(true);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  if (!user) {
    return <p>Brak danych użytkownika.</p>;
  }

  return (
    <div className="profile">
      <h1>Twój profil</h1>
      <div className="profile-info">
        <div className="profile-field">
          <label>Imię:</label>
          <p>{user.name}</p>
        </div>
        <div className="profile-field">
          <label>Nazwisko:</label>
          <p>{user.surname}</p>
        </div>
        <div className="profile-field">
          <label>Email:</label>
          <p>{user.email}</p>
        </div>
      </div>
      <button
        onClick={handleLogout}
        disabled={loading}
        className="logout-button"
      >
        {loading ? <LoadingSpinner small /> : 'Wyloguj się'}
      </button>
    </div>
  );
};

export default Profile;
