import { useEffect, useState } from 'react';
import { toast } from 'react-toastify';
import { getDashboard } from '../ccomponents/service/DashboardService';
import './Dashboard.css';

const Dashboard = () => {
  const [dashboard, setDashboard] = useState({});

  useEffect(() => {
    const fetchDashboard = async () => {
      try {
        const response = await getDashboard();
        setDashboard(response.data);
      } catch {
        toast.error('Failed to Load Dashboard');
      }
    };

    fetchDashboard();
  }, []);

  return (
    <div className="cards">
      <div className="card">
        <h2>{dashboard.users}</h2>
        <p>Users</p>
      </div>

      <div className="card">
        <h2>{dashboard.books}</h2>
        <p>Books</p>
      </div>

      <div className="card">
        <h2>{dashboard.authors}</h2>
        <p>Authors</p>
      </div>

      <div className="card">
        <h2>{dashboard.categories}</h2>
        <p>Categories</p>
      </div>
    </div>
  );
};

export default Dashboard;
