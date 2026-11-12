import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import {
  getAllCategories,
  deleteCategory,
  getCategoryById,
  searchCategoryByName,
} from "../ccomponents/service/CategoryService";
import { hasAnyRole } from "../ccomponents/service/AuthService";
import { useNavigate } from "react-router-dom";
import Spinner from "../ccomponents/Spinner";
import { toast } from "react-toastify";
import "./Books.css";

const Categories = () => {
  const [categories, setCategories] = useState([]);
  const [searchId, setSearchId] = useState("");
  const [searchName, setSearchName] = useState("");
  const [loading, setLoading] = useState(true);

  const navigate = useNavigate();
  const canManage = hasAnyRole("ADMIN", "LIBRARIAN");

  useEffect(() => {
    let active = true;

    getAllCategories()
      .then((response) => {
        if (active) {
          setCategories(response.data.data);
        }
      })
      .catch((error) => {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Categories");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const fetchCategories = async () => {
    setLoading(true);
    try {
      const response = await getAllCategories();
      setCategories(response.data.data);
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Load Categories");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    const confirmRemove = window.confirm(
      "Are you sure you want to remove this Category?"
    );

    if (!confirmRemove) {
      return;
    }

    try {
      await deleteCategory(id);
      toast.success("Category Removed Successfully");
      fetchCategories();
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Delete Category");
    }
  };

  const handleSearch = async (event) => {
    event.preventDefault();

    try {
      if (searchId !== "") {
        const response = await getCategoryById(searchId);
        const category = response.data.data;

        if (category) {
          setCategories([category]);
        } else {
          toast.error("Category Not Found");
          setCategories([]);
        }
      } else if (searchName !== "") {
        const response = await searchCategoryByName(searchName);
        setCategories(response.data.data);
      } else {
        toast.warning("Please enter Category ID or Name");
      }
    } catch (error) {
      toast.error(error.response?.data?.message || "Category Not Found");
      setCategories([]);
    }
  };

  if (loading) {
    return (
      <Layout>
        <Spinner />
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="books-header">
        <h1>Categories</h1>

        {canManage && (
          <button type="button" onClick={() => navigate("/add-category")}>
            Add Category
          </button>
        )}
      </div>

      <form className="search-container" onSubmit={handleSearch}>
        <input
          type="number"
          placeholder="Search By ID"
          aria-label="Search categories by ID"
          value={searchId}
          onChange={(e) => setSearchId(e.target.value)}
        />

        <input
          type="text"
          placeholder="Search By Name"
          aria-label="Search categories by name"
          value={searchName}
          onChange={(e) => setSearchName(e.target.value)}
        />

        <button type="submit">Search</button>

        <button
          type="button"
          onClick={() => {
            setSearchId("");
            setSearchName("");
            fetchCategories();
          }}
        >
          Show All
        </button>
      </form>

      <div className="table-scroll">
        <table className="book-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Category Name</th>
              {canManage && <th>Actions</th>}
            </tr>
          </thead>

          <tbody>
            {categories.length === 0 ? (
              <tr>
                <td colSpan={canManage ? 3 : 2}>No Categories Found</td>
              </tr>
            ) : (
              categories.map((category) => (
                <tr key={category.id}>
                  <td>{category.id}</td>
                  <td>{category.name}</td>

                  {canManage && (
                    <td>
                      <div className="action-buttons">
                        <button
                          type="button"
                          onClick={() => navigate(`/edit-category/${category.id}`)}
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          onClick={() => handleDelete(category.id)}
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </Layout>
  );
};

export default Categories;
