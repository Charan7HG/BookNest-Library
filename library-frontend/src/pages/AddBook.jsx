import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import { Link, useNavigate } from "react-router-dom";
import { addBook } from "../ccomponents/service/BookService";
import { getAllAuthors } from "../ccomponents/service/AuthorService";
import { toast } from "react-toastify";
import { getAllUsers } from "../ccomponents/service/UserService";
import { getAllCategories } from "../ccomponents/service/CategoryService";
import { hasAnyRole } from "../ccomponents/service/AuthService";
import "./Forms.css";

const AddBook = () => {
  const [name, setName] = useState("");
  const [price, setPrice] = useState("");
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState("");
  const [authors, setAuthors] = useState([]);
  const [authorId, setAuthorId] = useState("");
  const [categories, setCategories] = useState([]);
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [saving, setSaving] = useState(false);
  const navigate = useNavigate();

  const canAssignUser = hasAnyRole("ADMIN");

  useEffect(() => {
    let active = true;

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

    fetchAuthors();
    fetchUsers();
    fetchCategories();

    return () => {
      active = false;
    };
  }, [canAssignUser]);

  const handleCategoryChange = (id) => {
    if (selectedCategories.includes(id)) {
      setSelectedCategories(
        selectedCategories.filter((categoryId) => categoryId !== id)
      );
    } else {
      setSelectedCategories([...selectedCategories, id]);
    }
  };

  const saveBook = async (event) => {
    event.preventDefault();

    if (
      name.trim() === "" ||
      price === "" ||
      authorId === "" ||
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
          id: Number(authorId),
        },
        users: selectedUser ? { id: Number(selectedUser) } : null,
        category: selectedCategories.map((id) => ({
          id: Number(id),
        })),
      };

      await addBook(book);

      toast.success("Book Added");
      navigate("/books");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Add Book");
    } finally {
      setSaving(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Add Book</h1>
      </div>

      <form className="form-container" onSubmit={saveBook}>
        <div className="form-group">
          <label htmlFor="book-name">Book Name</label>
          <input
            id="book-name"
            type="text"
            placeholder="Enter Book Name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="book-price">Price</label>
          <input
            id="book-price"
            type="number"
            min="0"
            step="0.01"
            placeholder="Enter Price"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="book-author">Author</label>
          <select
            id="book-author"
            value={authorId}
            onChange={(e) => setAuthorId(e.target.value)}
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
            <label htmlFor="book-user">Assign to User</label>
            <select
              id="book-user"
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
            Librarians cannot assign books to users. The book will be saved without
            an assigned user.
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
            {saving ? "Saving..." : "Save Book"}
          </button>
          <Link to="/books" className="form-cancel">
            Cancel
          </Link>
        </div>
      </form>
    </Layout>
  );
};

export default AddBook;
