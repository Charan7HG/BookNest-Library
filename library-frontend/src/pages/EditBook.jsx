import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getBookById, updateBook } from "../ccomponents/service/BookService";
import { toast } from "react-toastify";
import { getAllUsers } from "../ccomponents/service/UserService";
import { getAllAuthors } from "../ccomponents/service/AuthorService";
import { getAllCategories } from "../ccomponents/service/CategoryService";
import { hasAnyRole } from "../ccomponents/service/AuthService";
import Spinner from "../ccomponents/Spinner";
import "./Forms.css";

const EditBook = () => {
  const { id } = useParams();
  return <EditBookForm key={id} id={id} />;
};

const EditBookForm = ({ id }) => {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [price, setPrice] = useState("");
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState("");
  const [assignedUserName, setAssignedUserName] = useState("");
  const [selectedAuthor, setSelectedAuthor] = useState("");
  const [authors, setAuthors] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const canAssignUser = hasAnyRole("ADMIN");

  useEffect(() => {
    let active = true;

    const fetchUsers = async () => {
      if (!canAssignUser) {
        return;
      }

      try {
        const response = await getAllUsers();
        if (active) setUsers(response.data.data);
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Users");
        }
      }
    };

    const fetchAuthors = async () => {
      try {
        const response = await getAllAuthors();
        if (active) setAuthors(response.data.data);
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Authors");
        }
      }
    };

    const fetchCategories = async () => {
      try {
        const response = await getAllCategories();
        if (active) setCategories(response.data.data);
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Categories");
        }
      }
    };

    fetchUsers();
    fetchAuthors();
    fetchCategories();

    return () => {
      active = false;
    };
  }, [canAssignUser]);

  useEffect(() => {
    let active = true;

    const fetchBook = async () => {
      try {
        const response = await getBookById(id);
        const book = response.data.data;

        if (!book) {
          toast.error("Book Not Found");
          return;
        }

        if (active) {
          setName(book.name);
          setPrice(book.price);
          setSelectedAuthor(book.author?.id || "");
          setSelectedUser(book.users?.id || "");
          setAssignedUserName(book.users?.name || "");
          setSelectedCategories(
            book.category ? book.category.map((category) => category.id) : []
          );
        }
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "Book Not Found");
        }
      } finally {
        if (active) setLoading(false);
      }
    };

    fetchBook();

    return () => {
      active = false;
    };
  }, [id]);

  const handleCategoryChange = (categoryId) => {
    if (selectedCategories.includes(categoryId)) {
      setSelectedCategories(
        selectedCategories.filter((idValue) => idValue !== categoryId)
      );
    } else {
      setSelectedCategories([...selectedCategories, categoryId]);
    }
  };

  const handleUpdate = async (event) => {
    event.preventDefault();

    if (
      name.trim() === "" ||
      price === "" ||
      selectedAuthor === "" ||
      selectedCategories.length === 0 ||
      (canAssignUser && selectedUser === "")
    ) {
      toast.warning("Please Fill All Required Fields");
      return;
    }

    setSaving(true);

    try {
      const book = {
        name: name.trim(),
        price: Number(price),
        author: {
          id: Number(selectedAuthor),
        },
        users: selectedUser ? { id: Number(selectedUser) } : null,
        category: selectedCategories.map((categoryId) => ({
          id: Number(categoryId),
        })),
      };

      await updateBook(id, book);

      toast.success("Book Updated Successfully");
      navigate("/books");
    } catch (error) {
      toast.error(error.response?.data?.message || "Update Failed");
    } finally {
      setSaving(false);
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
      <div className="page-header">
        <h1>Update Book</h1>
      </div>

      <form className="form-container" onSubmit={handleUpdate}>
        <div className="form-group">
          <label htmlFor="edit-book-name">Book Name</label>
          <input
            id="edit-book-name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="edit-book-price">Price</label>
          <input
            id="edit-book-price"
            type="number"
            min="0"
            step="0.01"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="edit-book-author">Author</label>
          <select
            id="edit-book-author"
            value={selectedAuthor}
            onChange={(e) => setSelectedAuthor(e.target.value)}
            required
          >
            <option value="">Select Author</option>

            {authors.map((author) => (
              <option key={author.id} value={author.id}>
                {author.name}
              </option>
            ))}
          </select>
        </div>

        {canAssignUser ? (
          <div className="form-group">
            <label htmlFor="edit-book-user">Assign to User</label>
            <select
              id="edit-book-user"
              value={selectedUser}
              onChange={(e) => setSelectedUser(e.target.value)}
              required
            >
              <option value="">Select User</option>

              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.name}
                </option>
              ))}
            </select>
          </div>
        ) : (
          <p className="form-hint">
            {assignedUserName
              ? `Currently assigned to ${assignedUserName}. Borrower changes are managed by an administrator.`
              : "This book is not assigned to a user. Borrower changes are managed by an administrator."}
          </p>
        )}

        <div className="form-group">
          <p className="form-group-label">Categories</p>

          <div className="checkbox-list">
            {categories.map((category) => (
              <label key={category.id} className="checkbox-item">
                <input
                  type="checkbox"
                  checked={selectedCategories.includes(category.id)}
                  onChange={() => handleCategoryChange(category.id)}
                />
                {category.name}
              </label>
            ))}
          </div>
        </div>

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Updating..." : "Update Book"}
          </button>
          <Link to="/books" className="form-cancel">
            Cancel
          </Link>
        </div>
      </form>
    </Layout>
  );
};

export default EditBook;
