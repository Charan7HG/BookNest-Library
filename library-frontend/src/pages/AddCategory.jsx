import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import Layout from "../ccomponents/Layout";
import { addCategory } from "../ccomponents/service/CategoryService";
import { toast } from "react-toastify";
import "./Forms.css";

const AddCategory = () => {
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);

  const navigate = useNavigate();

  const saveCategory = async (event) => {
    event.preventDefault();

    if (name.trim() === "") {
      toast.warning("Please Fill Name Field");
      return;
    }

    setSaving(true);

    try {
      await addCategory({ name: name.trim() });
      toast.success("Category Added Successfully");
      navigate("/categories");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Add Category");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Add Category</h1>
      </div>

      <form className="form-container" onSubmit={saveCategory}>
        <div className="form-group">
          <label htmlFor="category-name">Category Name</label>
          <input
            id="category-name"
            type="text"
            placeholder="Enter Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Saving..." : "Add Category"}
          </button>
          <Link to="/categories" className="form-cancel">
            Cancel
          </Link>
        </div>
      </form>
    </Layout>
  );
};

export default AddCategory;
