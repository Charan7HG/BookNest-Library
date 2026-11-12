import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import {
    forgotPassword,
    isAuthenticated,
} from "../ccomponents/service/AuthService";
import "./Login.css";

const ForgotPassword = () => {
    const [email, setEmail] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const navigate = useNavigate();

    if (isAuthenticated()) {
        return <Navigate to="/" replace />;
    }

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        if (!email.trim() || !newPassword || !confirmPassword) {
            setError("Please fill in all fields.");
            return;
        }

        if (newPassword !== confirmPassword) {
            setError("Passwords do not match.");
            return;
        }

        setSubmitting(true);

        try {
            await forgotPassword(email.trim(), newPassword);
            toast.success("Password updated successfully. Please login with your new password.");
            navigate("/login", { replace: true });
        } catch (resetError) {
            const message = resetError.response?.data?.message;

            if (message) {
                setError(message);
            } else if (resetError.request) {
                setError("Unable to connect to the server. Please try again.");
            } else {
                setError("Failed to reset password. Please try again.");
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
                    <h1>Reset Password</h1>
                    <p>Use your registered email to set a new password</p>
                </div>

                <form onSubmit={handleSubmit} noValidate>
                    <div className="auth-field">
                        <label htmlFor="reset-email">Registered Email</label>
                        <input
                            id="reset-email"
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            placeholder="Enter your registered email"
                            autoComplete="email"
                            autoFocus
                            required
                        />
                    </div>

                    <div className="auth-field">
                        <label htmlFor="reset-new-password">New Password</label>
                        <div className="password-field">
                            <input
                                id="reset-new-password"
                                type={showNewPassword ? "text" : "password"}
                                value={newPassword}
                                onChange={(event) => setNewPassword(event.target.value)}
                                placeholder="Enter a new password"
                                autoComplete="new-password"
                                required
                            />
                            <button
                                type="button"
                                className="toggle-password-btn"
                                onClick={() => setShowNewPassword((visible) => !visible)}
                                aria-label={showNewPassword ? "Hide password" : "Show password"}
                                aria-pressed={showNewPassword}
                                aria-controls="reset-new-password"
                                title={showNewPassword ? "Hide password" : "Show password"}
                            >
                                {showNewPassword ? "🙈" : "👁️"}
                            </button>
                        </div>
                    </div>

                    <div className="auth-field">
                        <label htmlFor="reset-confirm-password">Confirm Password</label>
                        <div className="password-field">
                            <input
                                id="reset-confirm-password"
                                type={showConfirmPassword ? "text" : "password"}
                                value={confirmPassword}
                                onChange={(event) => setConfirmPassword(event.target.value)}
                                placeholder="Re-enter the new password"
                                autoComplete="new-password"
                                required
                            />
                            <button
                                type="button"
                                className="toggle-password-btn"
                                onClick={() => setShowConfirmPassword((visible) => !visible)}
                                aria-label={showConfirmPassword ? "Hide password" : "Show password"}
                                aria-pressed={showConfirmPassword}
                                aria-controls="reset-confirm-password"
                                title={showConfirmPassword ? "Hide password" : "Show password"}
                            >
                                {showConfirmPassword ? "🙈" : "👁️"}
                            </button>
                        </div>
                    </div>

                    {error && (
                        <p className="auth-error" role="alert">
                            {error}
                        </p>
                    )}

                    <button type="submit" className="auth-submit" disabled={submitting}>
                        {submitting ? "Updating..." : "Reset Password"}
                    </button>
                </form>

                <div className="auth-links">
                    <p>
                        Remember your password? <Link to="/login">Go to Login</Link>
                    </p>
                </div>
            </div>
        </div>
    );
};

export default ForgotPassword;
