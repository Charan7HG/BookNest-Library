import { useState } from "react";
import { Link, Navigate } from "react-router-dom";
import { toast } from "react-toastify";
import { isAuthenticated } from "../ccomponents/service/AuthService";
import { submitPasswordResetRequest } from "../ccomponents/service/PasswordResetService";
import "./Login.css";

const GENERIC_MESSAGE =
    "If an eligible account exists, your password reset request has been submitted. " +
    "Please contact your Librarian if you need assistance.";

const ForgotPassword = () => {
    const [email, setEmail] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);
    const [submitted, setSubmitted] = useState(false);

    if (isAuthenticated()) {
        return <Navigate to="/" replace />;
    }

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError("");

        if (!email.trim()) {
            setError("Please enter your registered email.");
            return;
        }

        setSubmitting(true);

        try {
            await submitPasswordResetRequest(email.trim());
            setSubmitted(true);
            toast.success("Request submitted.");
        } catch (requestError) {
            if (requestError.request) {
                setError("Unable to connect to the server. Please try again.");
            } else {
                setError("Unable to submit the request. Please try again.");
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
                    <h1>Forgot Password</h1>
                    <p>Ask library staff to help you regain access</p>
                </div>

                {submitted ? (
                    <div className="auth-links">
                        <p role="status">{GENERIC_MESSAGE}</p>
                        <p>
                            <Link to="/login">Back to Login</Link>
                        </p>
                    </div>
                ) : (
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

                        {error && (
                            <p className="auth-error" role="alert">
                                {error}
                            </p>
                        )}

                        <button type="submit" className="auth-submit" disabled={submitting}>
                            {submitting ? "Submitting..." : "Submit Request"}
                        </button>

                        <div className="auth-links">
                            <p>
                                Remember your password? <Link to="/login">Back to Login</Link>
                            </p>
                        </div>
                    </form>
                )}
            </div>
        </div>
    );
};

export default ForgotPassword;
