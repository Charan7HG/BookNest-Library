import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  getCategoryById,
  updateCategory,
} from "../ccomponents/service/CategoryService";
import { toast } from "react-toastify";
import "./Forms.css";

const EditCategory = () => {
  const { id } = useParams();
  return <EditCategoryForm key={id} id={id} />;
};

const EditCategoryForm = ({ id }) => {
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    let active = true;

    const fetchCategory = async () => {
      try {
        const response = await getCategoryById(id);
        if (active) {
          setName(response.data.data.name);
        }
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "Category Not Found");
        }
      }
    };

    fetchCategory();

    return () => {
      active = false;
    };
  }, [id]);

  const handleUpdate = async (event) => {
    event.preventDefault();

    if (name.trim() === "") {
      toast.warning("Fill The Name Field");
      return;
    }

    setSaving(true);

    try {
      await updateCategory(id, { id: Number(id), name: name.trim() });
      toast.success("Category Updated Successfully");
      navigate("/categories");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed To Update Category");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Edit Category</h1>
      </div>

      <form className="form-container" onSubmit={handleUpdate}>
        <div className="form-group">
          <label htmlFor="edit-category-name">Category Name</label>
          <input
            id="edit-category-name"
            type="text"
            placeholder="Enter Category Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Updating..." : "Update Category"}
          </button>
          <Link to="/categories" className="form-cancel">
            Cancel
          </Link>
        </div>
      </form>
    </Layout>
  );
};

export default EditCategory;
