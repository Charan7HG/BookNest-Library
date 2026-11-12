import "./Components.css";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { clearAuth, getStoredUser } from "./service/AuthService";

const Navbar = () => {
    const user = getStoredUser();
    const navigate = useNavigate();

    const handleLogout = () => {
        clearAuth();
        toast.info("You have been logged out.");
        navigate("/login", { replace: true });
    };

    return (
        <header className="navbar">
            <Link to="/" className="navbar-brand">
                <span aria-hidden="true">📚</span>
                <h2>Library Management</h2>
            </Link>

            <div className="navbar-actions">
                <div className="profile">
                    <span>Welcome,</span>
                    <b>{user?.name || "User"}</b>
                    {user?.role && <span className="role-badge">{user.role}</span>}
                </div>

                <button type="button" onClick={handleLogout} className="logout-btn">
                    Logout
                </button>
            </div>
        </header>
    );
};

export default Navbar;
