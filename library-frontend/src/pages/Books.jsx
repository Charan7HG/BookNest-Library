import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import {
  getAllBooks,
  deleteBook,
  searchBookByName,
  getBookById,
} from "../ccomponents/service/BookService";
import {
  createBorrowRequest,
  getMyBorrowRequests,
} from "../ccomponents/service/BorrowingService";
import { getErrorMessage } from "../ccomponents/borrowUtils";
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
  const [requestingBookId, setRequestingBookId] = useState(null);
  const [pendingBookIds, setPendingBookIds] = useState(() => new Set());

  const navigate = useNavigate();
  const canManage = hasAnyRole("ADMIN", "LIBRARIAN");
  const isMember = hasAnyRole("MEMBER");
  const showActions = canManage || isMember;

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
          toast.error(
            getErrorMessage(error, "Failed to Load Books")
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!isMember) {
      return undefined;
    }

    let active = true;

    getMyBorrowRequests()
      .then((response) => {
        if (!active) {
          return;
        }

        const pendingIds = (response.data.data || [])
          .filter((request) => request.status === "PENDING")
          .map((request) => request.bookId);

        setPendingBookIds(new Set(pendingIds));
      })
      .catch((error) => {
        if (active) {
          toast.error(
            getErrorMessage(error, "Failed to load your borrow requests")
          );
        }
      });

    return () => {
      active = false;
    };
  }, [isMember]);

  const fetchBooks = async () => {
    setLoading(true);
    try {
      const response = await getAllBooks();
      setBooks(response.data.data);
    } catch (error) {
      toast.error(getErrorMessage(error, "Failed to Load Books"));
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
      toast.error(getErrorMessage(error, "Failed to Delete Book"));
    }
  };

  const handleBorrowRequest = async (book) => {
    if (requestingBookId !== null) {
      return;
    }

    setRequestingBookId(book.id);

    try {
      const response = await createBorrowRequest(book.id);
      toast.success(
        response.data?.message || "Borrow request submitted successfully."
      );

      setPendingBookIds((current) => {
        const updated = new Set(current);
        updated.add(book.id);
        return updated;
      });
    } catch (error) {
      toast.error(
        getErrorMessage(error, "Failed to submit the borrow request.")
      );
    } finally {
      setRequestingBookId(null);
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
      toast.error(getErrorMessage(error, "Book Not Found"));
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

      {isMember && (
        <p className="borrow-hint">
          Copy availability is confirmed by library staff when your request is
          approved. Track the result under <strong>My Borrow Requests</strong>.
        </p>
      )}

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
              {showActions && <th>Actions</th>}
            </tr>
          </thead>

          <tbody>
            {books.length === 0 ? (
              <tr>
                <td colSpan={showActions ? 7 : 6}>No Books Found</td>
              </tr>
            ) : (
              books.map((book) => (
                <tr key={book.id}>
                  <td>{book.id}</td>
                  <td>
                    {book.name}
                    {isMember && (
                      <span className="availability-note">
                        Availability checked at approval
                      </span>
                    )}
                  </td>
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

                  {showActions && (
                    <td className="nowrap">
                      {canManage ? (
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
                      ) : (
                        <div className="action-buttons">
                          <button
                            type="button"
                            className="request-btn"
                            onClick={() => handleBorrowRequest(book)}
                            disabled={
                              pendingBookIds.has(book.id) ||
                              requestingBookId !== null
                            }
                          >
                            {pendingBookIds.has(book.id)
                              ? "Request Pending"
                              : requestingBookId === book.id
                                ? "Requesting..."
                                : "Request to Borrow"}
                          </button>
                        </div>
                      )}
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
