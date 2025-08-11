import React from 'react';
import './ErrorModal';

const ErrorModal = ({ message, onClose }) => {
  if (!message) return null;

  return (
    <div className="error-modal-overlay">
      <div className="error-modal">
        <div className="error-modal-header">
          <h3>Błąd</h3>
          <button onClick={onClose} className="close-button">
            &times;
          </button>
        </div>
        <div className="error-modal-body">
          <p>{message}</p>
        </div>
        <div className="error-modal-footer">
          <button onClick={onClose} className="confirm-button">
            OK
          </button>
        </div>
      </div>
    </div>
  );
};

export default ErrorModal;