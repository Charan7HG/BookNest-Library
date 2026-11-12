import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getUserById, updateUser } from "../ccomponents/service/UserService";
import { toast } from "react-toastify";
import "./Forms.css";

const EditUser = () => {
  const { id } = useParams();
  return <EditUserForm key={id} id={id} />;
};

const EditUserForm = ({ id }) => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [role, setRole] = useState("MEMBER");
  const [saving, setSaving] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    let active = true;

    const fetchUser = async () => {
      try {
        const response = await getUserById(id);
        const user = response.data.data;

        if (active) {
          setName(user.name);
          setEmail(user.profile.email);
          setPhone(user.profile.phone);
          setRole(user.role);
        }
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "User Not Found");
        }
      }
    };

    fetchUser();

    return () => {
      active = false;
    };
  }, [id]);

  const handleUpdate = async (event) => {
    event.preventDefault();

    if (name.trim() === "" || email.trim() === "" || phone === "") {
      toast.warning("Fill All Fields");
      return;
    }

    setSaving(true);

    try {
      const user = {
        id: Number(id),
        name: name.trim(),
        role: role,
        profile: {
          email: email.trim(),
          phone: Number(phone),
        },
      };

      await updateUser(id, user);

      toast.success("User Updated Successfully");
      navigate("/users");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed To Update User");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Edit User</h1>
      </div>

      <form className="form-container" onSubmit={handleUpdate}>
        <div className="form-group">
          <label htmlFor="edit-user-name">Name</label>
          <input
            id="edit-user-name"
            type="text"
            placeholder="Enter Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="edit-user-email">Email</label>
          <input
            id="edit-user-email"
            type="email"
            placeholder="Enter Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="edit-user-phone">Phone</label>
          <input
            id="edit-user-phone"
            type="number"
            placeholder="Enter Phone"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="edit-user-role">Role</label>
          <select
            id="edit-user-role"
            value={role}
            onChange={(e) => setRole(e.target.value)}
          >
            <option value="MEMBER">MEMBER</option>
            <option value="LIBRARIAN">LIBRARIAN</option>
            <option value="ADMIN">ADMIN</option>
          </select>
        </div>

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Updating..." : "Update User"}
          </button>
          <Link to="/users" className="form-cancel">
            Cancel
          </Link>
        </div>
      </form>
    </Layout>
  );
};

export default EditUser;
