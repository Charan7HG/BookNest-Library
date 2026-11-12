import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Layout from "../ccomponents/Layout";
import { addUser } from "../ccomponents/service/UserService";
import { toast } from "react-toastify";
import "./Forms.css";

const AddUser = () => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("MEMBER");
  const [showPassword, setShowPassword] = useState(false);
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();

  const saveUser = async (event) => {
    event.preventDefault();

    if (
      name.trim() === "" ||
      email.trim() === "" ||
      phone === "" ||
      password === ""
    ) {
      toast.warning("Please Fill All Fields");
      return;
    }

    setSaving(true);

    try {
      const user = {
        name: name.trim(),
        password: password,
        role: role,
        profile: {
          email: email.trim(),
          phone: Number(phone),
        },
      };

      await addUser(user);

      toast.success("User Added Successfully");
      navigate("/users");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Add User");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Add User</h1>
      </div>

      <form className="form-container" onSubmit={saveUser}>
        <div className="form-group">
          <label htmlFor="user-name">Name</label>
          <input
            id="user-name"
            type="text"
            placeholder="Enter Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="user-email">Email</label>
          <input
            id="user-email"
            type="email"
            placeholder="Enter Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="user-phone">Phone</label>
          <input
            id="user-phone"
            type="number"
            placeholder="Enter Phone"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="user-role">Role</label>
          <select
            id="user-role"
            value={role}
            onChange={(e) => setRole(e.target.value)}
          >
            <option value="MEMBER">MEMBER</option>
            <option value="LIBRARIAN">LIBRARIAN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>

        <div className="form-group">
          <label htmlFor="user-password">Password</label>
          <div className="form-password-field">
            <input
              id="user-password"
              type={showPassword ? "text" : "password"}
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
              required
            />
            <button
              type="button"
              className="form-toggle-password"
              onClick={() => setShowPassword((visible) => !visible)}
              aria-label={showPassword ? "Hide password" : "Show password"}
              aria-pressed={showPassword}
              aria-controls="user-password"
              title={showPassword ? "Hide password" : "Show password"}
            >
              {showPassword ? "🙈" : "👁️"}
            </button>
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Saving..." : "Add User"}
          </button>
          <Link to="/users" className="form-cancel">
            Cancel
          </Link>
        </div>
      </form>
    </Layout>
  );
};

export default AddUser;
