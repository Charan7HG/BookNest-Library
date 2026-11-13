import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { isAuthenticated, isPasswordChangeRequired, login, saveAuth } from "../ccomponents/service/AuthService";
import "./Login.css";

const Login = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();

    const requestedPath = location.state?.from?.pathname;
    const redirectTo =
        requestedPath && requestedPath !== "/login" ? requestedPath : "/";

    if (isAuthenticated()) {
        // A temporary-password user is allowed to sign in but goes straight to
        // the Change Password page, matching the backend filter.
        return <Navigate to={isPasswordChangeRequired() ? "/change-password" : redirectTo} replace />;
    }

    const handleLogin = async (event) => {
        event.preventDefault();
        setError("");

        if (!email.trim() || !password) {
            setError("Please enter both email and password.");
            return;
        }

        setSubmitting(true);

        try {
            const response = await login(email.trim(), password);
            const data = response.data?.data;

            if (!data?.token || !data?.user) {
                setError("The server returned an invalid login response.");
                return;
            }

            saveAuth(data.token, data.user);

            const mustChangePassword =
                data.passwordChangeRequired === true ||
                data.user.passwordChangeRequired === true;

            if (mustChangePassword) {
                toast.info("Please change your temporary password before continuing.");
                navigate("/change-password", { replace: true });
                return;
            }

            toast.success("Login Successful");
            navigate(redirectTo, { replace: true });
        } catch (loginError) {
            const message = loginError.response?.data?.message;

            if (message) {
                setError(message);
            } else if (loginError.request) {
                setError("Unable to connect to the server. Please try again.");
            } else {
                setError("Login failed. Please try again.");
            }
        } finally {
            setSubmitting(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <div className="auth-brand">
                    <div className="auth-logo" aria-hidden="true">📚</div>
                    <h1>Library Management</h1>
                    <p>Sign in to manage your library</p>
                </div>

                <form onSubmit={handleLogin} noValidate>
                    <div className="auth-field">
                        <label htmlFor="login-email">Email</label>
                        <input
                            id="login-email"
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            placeholder="Enter your email"
                            autoComplete="email"
                            autoFocus
                            required
                        />
                    </div>

                    <div className="auth-field">
                        <label htmlFor="login-password">Password</label>
                        <div className="password-field">
                            <input
                                id="login-password"
                                type={showPassword ? "text" : "password"}
                                value={password}
                                onChange={(event) => setPassword(event.target.value)}
                                placeholder="Enter your password"
                                autoComplete="current-password"
                                required
                            />
                            <button
                                type="button"
                                className="toggle-password-btn"
                                onClick={() => setShowPassword((visible) => !visible)}
                                aria-label={showPassword ? "Hide password" : "Show password"}
                                aria-pressed={showPassword}
                                aria-controls="login-password"
                                title={showPassword ? "Hide password" : "Show password"}
                            >
                                {showPassword ? "🙈" : "👁️"}
                            </button>
                        </div>
                    </div>

                    {error && (
                        <p className="auth-error" role="alert">
                            {error}
                        </p>
                    )}

                    <button type="submit" className="auth-submit" disabled={submitting}>
                        {submitting ? "Signing in..." : "Login"}
                    </button>
                </form>

                <div className="auth-links">
                    <Link to="/forgot-password">Forgot Password?</Link>
                </div>
            </div>
        </div>
    );
};

export default Login;
