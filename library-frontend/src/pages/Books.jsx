import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import {
  getAllBooks,
  deleteBook,
  searchBookByName,
  getBookById,
} from "../ccomponents/service/BookService";
import { hasAnyRole } from "../ccomponents/service/AuthService";
import "./Books.css";
import { useNavigate } from "react-router-dom";

import Spinner from "../ccomponents/Spinner";
import { toast } from "react-toastify";

const Books = () => {
  const [books, setBooks] = useState([]);
  const [searchId, setSearchId] = useState("");
  const [searchName, setSearchName] = useState("");

  const [loading, setLoading] = useState(true);

  const navigate = useNavigate();
  const canManage = hasAnyRole("ADMIN", "LIBRARIAN");

  useEffect(() => {
    let active = true;

    getAllBooks()
      .then((response) => {
        if (active) {
          setBooks(response.data.data);
        }
      })
      .catch((error) => {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Books");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const fetchBooks = async () => {
    setLoading(true);
    try {
      const response = await getAllBooks();
      setBooks(response.data.data);
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Load Books");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    const confirmDelete = window.confirm(
      "Are you sure you want to delete this book?"
    );

    if (!confirmDelete) {
      return;
    }

    try {
      await deleteBook(id);
      toast.success("Book Deleted Successfully");
      fetchBooks();
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Delete Book");
    }
  };

  const handleSearch = async (event) => {
    event.preventDefault();

    try {
      if (searchId !== "") {
        const response = await getBookById(searchId);
        const book = response.data.data;

        if (book) {
          setBooks([book]);
        } else {
          toast.error("Book Not Found");
          setBooks([]);
        }
      } else if (searchName !== "") {
        const response = await searchBookByName(searchName);
        setBooks(response.data.data);
      } else {
        toast.warning("Please enter Book ID or Name");
      }
    } catch (error) {
      toast.error(error.response?.data?.message || "Book Not Found");
      setBooks([]);
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
        <h1>Books</h1>

        {canManage && (
          <button type="button" onClick={() => navigate("/add-book")}>
            Add Book
          </button>
        )}
      </div>

      <form className="search-container" onSubmit={handleSearch}>
        <input
          type="number"
          placeholder="Search By ID"
          aria-label="Search books by ID"
          value={searchId}
          onChange={(e) => setSearchId(e.target.value)}
        />

        <input
          type="text"
          placeholder="Search By Name"
          aria-label="Search books by name"
          value={searchName}
          onChange={(e) => setSearchName(e.target.value)}
        />

        <button type="submit">Search</button>

        <button
          type="button"
          onClick={() => {
            setSearchId("");
            setSearchName("");
            fetchBooks();
          }}
        >
          Show All
        </button>
      </form>

      <div className="table-scroll">
        <table className="book-table book-table--books">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Price</th>
              <th>Author</th>
              <th>User</th>
              <th>Categories</th>
              {canManage && <th>Actions</th>}
            </tr>
          </thead>

          <tbody>
            {books.length === 0 ? (
              <tr>
                <td colSpan={canManage ? 7 : 6}>No Books Found</td>
              </tr>
            ) : (
              books.map((book) => (
                <tr key={book.id}>
                  <td>{book.id}</td>
                  <td>{book.name}</td>
                  <td className="nowrap">{book.price}</td>
                  <td>{book.author?.name}</td>
                  <td>{book.users?.name || "Not Assigned"}</td>
                  <td>
                    <div className="category-list">
                      {book.category?.length > 0
                        ? book.category.map((category) => category.name).join(", ")
                        : "No Category"}
                    </div>
                  </td>

                  {canManage && (
                    <td className="nowrap">
                      <div className="action-buttons">
                        <button
                          type="button"
                          onClick={() => navigate(`/edit-book/${book.id}`)}
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          onClick={() => handleDelete(book.id)}
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

export default Books;
