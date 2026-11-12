import { Link, Navigate, useLocation } from "react-router-dom";
import Layout from "../ccomponents/Layout";
import { getStoredUser, isAuthenticated } from "../ccomponents/service/AuthService";

const AccessDenied = () => (
    <Layout>
        <div className="page-message">
            <h1>Access Denied</h1>
            <p>Your account does not have permission to open this page.</p>
            <Link to="/">Back to Dashboard</Link>
        </div>
    </Layout>
);

const ProtectedRoute = ({ children, allowedRoles }) => {
    const location = useLocation();

    if (!isAuthenticated()) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }

    if (allowedRoles && allowedRoles.length > 0) {
        const role = getStoredUser()?.role;

        if (!allowedRoles.includes(role)) {
            return <AccessDenied />;
        }
    }

    return children;
};

export default ProtectedRoute;
