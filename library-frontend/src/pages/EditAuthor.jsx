import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getAuthorById, updateAuthor } from "../ccomponents/service/AuthorService";
import { toast } from "react-toastify";
import "./Forms.css";

const EditAuthor = () => {
  const { id } = useParams();
  return <EditAuthorForm key={id} id={id} />;
};

const EditAuthorForm = ({ id }) => {
  const [name, setName] = useState("");
  const [saving, setSaving] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    let active = true;

    const fetchAuthor = async () => {
      try {
        const response = await getAuthorById(id);
        if (active) {
          setName(response.data.data.name);
        }
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "Author Not Found");
        }
      }
    };

    fetchAuthor();

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
      await updateAuthor(id, { id: Number(id), name: name.trim() });
      toast.success("Author Updated Successfully");
      navigate("/authors");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed To Update Author");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Edit Author</h1>
      </div>

      <form className="form-container" onSubmit={handleUpdate}>
        <div className="form-group">
          <label htmlFor="edit-author-name">Author Name</label>
          <input
            id="edit-author-name"
            type="text"
            placeholder="Enter Author Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Updating..." : "Update Author"}
          </button>
          <Link to="/authors" className="form-cancel">
            Cancel
          </Link>
        </div>
      </form>
    </Layout>
  );
};

export default EditAuthor;
