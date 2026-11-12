import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import {
  getAllAuthors,
  deleteAuthor,
  getAuthorById,
  searchAuthorByName,
} from "../ccomponents/service/AuthorService";
import { hasAnyRole } from "../ccomponents/service/AuthService";
import { useNavigate } from "react-router-dom";
import Spinner from "../ccomponents/Spinner";
import { toast } from "react-toastify";
import "./Books.css";

const Authors = () => {
  const [authors, setAuthors] = useState([]);
  const [searchId, setSearchId] = useState("");
  const [searchName, setSearchName] = useState("");
  const [loading, setLoading] = useState(true);

  const navigate = useNavigate();
  const canManage = hasAnyRole("ADMIN", "LIBRARIAN");

  useEffect(() => {
    let active = true;

    getAllAuthors()
      .then((response) => {
        if (active) {
          setAuthors(response.data.data);
        }
      })
      .catch((error) => {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Authors");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const fetchAuthors = async () => {
    setLoading(true);
    try {
      const response = await getAllAuthors();
      setAuthors(response.data.data);
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Load Authors");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    const confirmRemove = window.confirm(
      "Are you sure you want to remove this Author?"
    );

    if (!confirmRemove) {
      return;
    }

    try {
      await deleteAuthor(id);
      toast.success("Author Removed Successfully");
      fetchAuthors();
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Delete Author");
    }
  };

  const handleSearch = async (event) => {
    event.preventDefault();

    try {
      if (searchId !== "") {
        const response = await getAuthorById(searchId);
        const author = response.data.data;

        if (author) {
          setAuthors([author]);
        } else {
          toast.error("Author Not Found");
          setAuthors([]);
        }
      } else if (searchName !== "") {
        const response = await searchAuthorByName(searchName);
        setAuthors(response.data.data);
      } else {
        toast.warning("Please enter Author ID or Name");
      }
    } catch (error) {
      toast.error(error.response?.data?.message || "Author Not Found");
      setAuthors([]);
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
        <h1>Authors</h1>

        {canManage && (
          <button type="button" onClick={() => navigate("/add-author")}>
            Add Author
          </button>
        )}
      </div>

      <form className="search-container" onSubmit={handleSearch}>
        <input
          type="number"
          placeholder="Search By ID"
          aria-label="Search authors by ID"
          value={searchId}
          onChange={(e) => setSearchId(e.target.value)}
        />

        <input
          type="text"
          placeholder="Search By Name"
          aria-label="Search authors by name"
          value={searchName}
          onChange={(e) => setSearchName(e.target.value)}
        />

        <button type="submit">Search</button>

        <button
          type="button"
          onClick={() => {
            setSearchId("");
            setSearchName("");
            fetchAuthors();
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
              <th>Author Name</th>
              {canManage && <th>Actions</th>}
            </tr>
          </thead>

          <tbody>
            {authors.length === 0 ? (
              <tr>
                <td colSpan={canManage ? 3 : 2}>No Authors Found</td>
              </tr>
            ) : (
              authors.map((author) => (
                <tr key={author.id}>
                  <td>{author.id}</td>
                  <td>{author.name}</td>

                  {canManage && (
                    <td>
                      <div className="action-buttons">
                        <button
                          type="button"
                          onClick={() => navigate(`/edit-author/${author.id}`)}
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          onClick={() => handleDelete(author.id)}
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

export default Authors;
