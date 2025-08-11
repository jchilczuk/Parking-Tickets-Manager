import TicketList from '../components/Tickets/TicketList';
import './TicketsPage.css';
import { useEffect } from 'react';
import { registerForPushNotifications } from '../utils/notifications';

const TicketsPage = () => {
  useEffect(() => {
    registerForPushNotifications();
  }, []);

  return (
    <div className="tickets-page">
      <h1>Twoje bilety parkingowe</h1>
      <TicketList />
    </div>
  );
};

export default TicketsPage;



