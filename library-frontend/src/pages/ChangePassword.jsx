import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Layout from "../ccomponents/Layout";
import { toast } from "react-toastify";
import {
  changePassword,
  clearPasswordChangeRequired,
  isPasswordChangeRequired,
} from "../ccomponents/service/AuthService";
import "./Forms.css";

const MIN_PASSWORD_LENGTH = 8;

const ChangePassword = () => {
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  // Read once for the notice shown to temporary-password users.
  const [forcedChange] = useState(() => isPasswordChangeRequired());

  const validate = () => {
    if (!currentPassword || !newPassword || !confirmPassword) {
      return "Please fill in all three password fields.";
    }

    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      return `New password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
    }

    if (newPassword !== confirmPassword) {
      return "New password and confirmation do not match.";
    }

    if (newPassword === currentPassword) {
      return "New password must be different from the current password.";
    }

    return "";
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const validationError = validate();

    if (validationError) {
      setFormError(validationError);
      toast.warning(validationError);
      return;
    }

    setFormError("");
    setSaving(true);

    try {
      await changePassword(currentPassword, newPassword);

      // The backend already cleared passwordChangeRequired. Update the stored
      // session so the router stops forcing this page, then continue normally.
      clearPasswordChangeRequired();

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");

      toast.success("Password changed successfully.");
      navigate("/", { replace: true });
    } catch (error) {
      const message =
        error.response?.data?.message || "Failed to change password.";
      setFormError(message);
      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Change Password</h1>
      </div>

      <form className="form-container" onSubmit={handleSubmit} noValidate>
        {forcedChange && (
          <p className="form-hint" role="status">
            You are signed in with a temporary password. Please choose a new
            password before using the rest of the application.
          </p>
        )}

        {formError && (
          <p className="form-error" role="alert">
            {formError}
          </p>
        )}

        <div className="form-group">
          <label htmlFor="current-password">Current Password</label>
          <input
            id="current-password"
            type="password"
            value={currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
            autoComplete="current-password"
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="new-password">New Password</label>
          <input
            id="new-password"
            type="password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            autoComplete="new-password"
            minLength={MIN_PASSWORD_LENGTH}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="confirm-password">Confirm New Password</label>
          <input
            id="confirm-password"
            type="password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            autoComplete="new-password"
            minLength={MIN_PASSWORD_LENGTH}
            required
          />
        </div>

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Saving..." : "Change Password"}
          </button>
        </div>
      </form>
    </Layout>
  );
};

export default ChangePassword;
