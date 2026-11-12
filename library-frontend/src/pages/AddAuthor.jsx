import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Layout from "../ccomponents/Layout";
import { addAuthor } from "../ccomponents/service/AuthorService";
import { toast } from "react-toastify";
import "./Forms.css";

const AddAuthor = () => {
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);

  const navigate = useNavigate();

  const saveAuthor = async (event) => {
    event.preventDefault();

    if (name.trim() === "") {
      toast.warning("Please Fill Name Field");
      return;
    }

    setSaving(true);

    try {
      await addAuthor({ name: name.trim() });
      toast.success("Author Added Successfully");
      navigate("/authors");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Add Author");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Add Author</h1>
      </div>

      <form className="form-container" onSubmit={saveAuthor}>
        <div className="form-group">
          <label htmlFor="author-name">Author Name</label>
          <input
            id="author-name"
            type="text"
            placeholder="Enter Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Saving..." : "Add Author"}
          </button>
          <Link to="/authors" className="form-cancel">
            Cancel
          </Link>
        </div>
      </form>
    </Layout>
  );
};

export default AddAuthor;
