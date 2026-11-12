import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Layout from "../ccomponents/Layout";
import { getDashboard } from "../ccomponents/service/DashboardService";
import { getStoredUser } from "../ccomponents/service/AuthService";
import Spinner from "../ccomponents/Spinner";
import "./Home.css";

const Home = () => {
  const [dashboard, setDashboard] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const user = getStoredUser();
  const isAdmin = user?.role === "ADMIN";

  useEffect(() => {
    let active = true;

    const fetchDashboard = async () => {
      try {
        const response = await getDashboard();
        if (active) {
          setDashboard(response.data);
        }
      } catch (dashboardError) {
        if (active) {
          setError(
            dashboardError.response?.data?.message ||
              "Failed to load the dashboard summary."
          );
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchDashboard();

    return () => {
      active = false;
    };
  }, []);

  if (loading) {
    return (
      <Layout>
        <Spinner />
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="dashboard">
        <h1>Library Management System</h1>
        <p className="dashboard-welcome">
          Welcome back, <strong>{user?.name || "User"}</strong>
          {user?.role ? ` (${user.role})` : ""}
        </p>

        {error && (
          <p className="dashboard-error" role="alert">
            {error}
          </p>
        )}

        <div className="dashboard-cards">
          <Link to="/books" className="dashboard-card books-card">
            <h2>📚 Books</h2>
            <p className="dashboard-value">{dashboard.books ?? 0}</p>
            <p>Total Books</p>
          </Link>

          <Link to="/authors" className="dashboard-card authors-card">
            <h2>👤 Authors</h2>
            <p className="dashboard-value">{dashboard.authors ?? 0}</p>
            <p>Total Authors</p>
          </Link>

          <Link to="/categories" className="dashboard-card categories-card">
            <h2>🏷 Categories</h2>
            <p className="dashboard-value">{dashboard.categories ?? 0}</p>
            <p>Total Categories</p>
          </Link>

          {isAdmin && (
            <Link to="/users" className="dashboard-card users-card">
              <h2>👥 Users</h2>
              <p className="dashboard-value">{dashboard.users ?? 0}</p>
              <p>Total Users</p>
            </Link>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Home;
