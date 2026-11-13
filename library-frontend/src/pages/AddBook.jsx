import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import { Link, useNavigate } from "react-router-dom";
import { addBook } from "../ccomponents/service/BookService";
import { addAuthor, getAllAuthors } from "../ccomponents/service/AuthorService";
import { addCategory, getAllCategories } from "../ccomponents/service/CategoryService";
import { toast } from "react-toastify";
import "./Forms.css";

const EARLIEST_PUBLISHED_YEAR = 1000;

const AddBook = () => {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [publishedYear, setPublishedYear] = useState("");
  const [language, setLanguage] = useState("");
  const [authorId, setAuthorId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [numberOfCopies, setNumberOfCopies] = useState("1");

  const [authors, setAuthors] = useState([]);
  const [categories, setCategories] = useState([]);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

  const [authorModalOpen, setAuthorModalOpen] = useState(false);
  const [newAuthorName, setNewAuthorName] = useState("");
  const [savingAuthor, setSavingAuthor] = useState(false);

  const [categoryModalOpen, setCategoryModalOpen] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState("");
  const [savingCategory, setSavingCategory] = useState(false);

  const latestPublishedYear = new Date().getFullYear() + 1;

  useEffect(() => {
    let active = true;

    const fetchAuthors = async () => {
      try {
        const response = await getAllAuthors();
        if (active) setAuthors(response.data.data || []);
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Authors");
        }
      }
    };

    const fetchCategories = async () => {
      try {
        const response = await getAllCategories();
        if (active) setCategories(response.data.data || []);
      } catch (error) {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Categories");
        }
      }
    };

    fetchAuthors();
    fetchCategories();

    return () => {
      active = false;
    };
  }, []);

  const validate = () => {
    if (!name.trim()) {
      return "Book name is required.";
    }

    if (price === "" || Number.isNaN(Number(price)) || Number(price) < 0) {
      return "Price must be zero or greater.";
    }

    const year = Number(publishedYear);
    if (
      publishedYear === "" ||
      !Number.isInteger(year) ||
      year < EARLIEST_PUBLISHED_YEAR ||
      year > latestPublishedYear
    ) {
      return `Published year must be a whole number between ${EARLIEST_PUBLISHED_YEAR} and ${latestPublishedYear}.`;
    }

    if (!language.trim()) {
      return "Language is required.";
    }

    if (!authorId) {
      return "Please select an author.";
    }

    if (!categoryId) {
      return "Please select a category.";
    }

    const copies = Number(numberOfCopies);
    if (numberOfCopies === "" || !Number.isInteger(copies) || copies < 1) {
      return "Number of copies must be a positive whole number.";
    }

    return "";
  };

  const saveBook = async (event) => {
    event.preventDefault();

    const validationError = validate();
    if (validationError) {
      setFormError(validationError);
      toast.warning(validationError);
      return;
    }

    setFormError("");
    setSaving(true);

    try {
      // All eight values from the form are sent to the server. Description is
      // optional; the backend stores it as NULL when it is blank.
      const book = {
        name: name.trim(),
        description: description.trim() || null,
        price: Number(price),
        publishedYear: Number(publishedYear),
        language: language.trim(),
        authorId: Number(authorId),
        categoryId: Number(categoryId),
        numberOfCopies: Number(numberOfCopies),
      };

      await addBook(book);

      toast.success("Book and physical copies added successfully.");
      navigate("/books");
    } catch (error) {
      const message = error.response?.data?.message || "Failed to Add Book";
      setFormError(message);
      toast.error(message);
    } finally {
      setSaving(false);
    }
  };

  const createAuthor = async (event) => {
    event.preventDefault();

    if (savingAuthor) {
      return;
    }

    if (!newAuthorName.trim()) {
      toast.warning("Author name is required.");
      return;
    }

    setSavingAuthor(true);

    try {
      const response = await addAuthor({ name: newAuthorName.trim() });
      const created = response.data?.data;

      if (!created?.id) {
        throw new Error("The server did not return the new author.");
      }

      // Refresh the dropdown from the server, then automatically select the
      // record that was just created.
      try {
        const refreshed = await getAllAuthors();
        const list = refreshed.data.data || [];
        setAuthors(
          list.some((author) => author.id === created.id)
            ? list
            : [...list, created]
        );
      } catch {
        setAuthors((current) =>
          current.some((author) => author.id === created.id)
            ? current
            : [...current, created]
        );
      }

      setAuthorId(String(created.id));
      setNewAuthorName("");
      setAuthorModalOpen(false);
      toast.success("Author added and selected.");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Add Author");
    } finally {
      setSavingAuthor(false);
    }
  };

  const createCategory = async (event) => {
    event.preventDefault();

    if (savingCategory) {
      return;
    }

    if (!newCategoryName.trim()) {
      toast.warning("Category name is required.");
      return;
    }

    setSavingCategory(true);

    try {
      const response = await addCategory({ name: newCategoryName.trim() });
      const created = response.data?.data;

      if (!created?.id) {
        throw new Error("The server did not return the new category.");
      }

      try {
        const refreshed = await getAllCategories();
        const list = refreshed.data.data || [];
        setCategories(
          list.some((category) => category.id === created.id)
            ? list
            : [...list, created]
        );
      } catch {
        setCategories((current) =>
          current.some((category) => category.id === created.id)
            ? current
            : [...current, created]
        );
      }

      setCategoryId(String(created.id));
      setNewCategoryName("");
      setCategoryModalOpen(false);
      toast.success("Category added and selected.");
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Add Category");
    } finally {
      setSavingCategory(false);
    }
  };

  return (
    <Layout>
      <div className="page-header">
        <h1>Add Book</h1>
      </div>

      <form className="form-container" onSubmit={saveBook} noValidate>
        {formError && (
          <p className="form-error" role="alert">
            {formError}
          </p>
        )}

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
          <label htmlFor="book-description">Description</label>
          <textarea
            id="book-description"
            rows="3"
            placeholder="Enter Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
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
          <label htmlFor="book-published-year">Published Year</label>
          <input
            id="book-published-year"
            type="number"
            min={EARLIEST_PUBLISHED_YEAR}
            max={latestPublishedYear}
            step="1"
            placeholder="Enter Published Year"
            value={publishedYear}
            onChange={(e) => setPublishedYear(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="book-language">Language</label>
          <input
            id="book-language"
            type="text"
            placeholder="Enter Language"
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="book-author">Author</label>
          <div className="select-with-action">
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

            <button
              type="button"
              className="form-link-button"
              onClick={() => {
                setNewAuthorName("");
                setAuthorModalOpen(true);
              }}
            >
              + Add New Author
            </button>
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="book-category">Category</label>
          <div className="select-with-action">
            <select
              id="book-category"
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value)}
              required
            >
              <option value="">Select Category</option>

              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>

            <button
              type="button"
              className="form-link-button"
              onClick={() => {
                setNewCategoryName("");
                setCategoryModalOpen(true);
              }}
            >
              + Add New Category
            </button>
          </div>
        </div>

        <div className="form-group">
          <label htmlFor="book-copies">Number of Copies</label>
          <input
            id="book-copies"
            type="number"
            min="1"
            step="1"
            placeholder="Enter Number of Copies"
            value={numberOfCopies}
            onChange={(e) => setNumberOfCopies(e.target.value)}
            required
          />
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

      {authorModalOpen && (
        <div
          className="confirm-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !savingAuthor) {
              setAuthorModalOpen(false);
            }
          }}
        >
          <div
            className="confirm-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="new-author-title"
          >
            <h2 id="new-author-title">Add New Author</h2>

            <form className="dialog-form" onSubmit={createAuthor} noValidate>
              <div className="form-group">
                <label htmlFor="new-author-name">Author Name</label>
                <input
                  id="new-author-name"
                  type="text"
                  value={newAuthorName}
                  onChange={(e) => setNewAuthorName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              <div className="confirm-actions">
                <button
                  type="button"
                  className="confirm-cancel"
                  onClick={() => setAuthorModalOpen(false)}
                  disabled={savingAuthor}
                >
                  Cancel
                </button>
                <button type="submit" className="confirm-create" disabled={savingAuthor}>
                  {savingAuthor ? "Saving..." : "Add Author"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {categoryModalOpen && (
        <div
          className="confirm-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !savingCategory) {
              setCategoryModalOpen(false);
            }
          }}
        >
          <div
            className="confirm-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="new-category-title"
          >
            <h2 id="new-category-title">Add New Category</h2>

            <form className="dialog-form" onSubmit={createCategory} noValidate>
              <div className="form-group">
                <label htmlFor="new-category-name">Category Name</label>
                <input
                  id="new-category-name"
                  type="text"
                  value={newCategoryName}
                  onChange={(e) => setNewCategoryName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              <div className="confirm-actions">
                <button
                  type="button"
                  className="confirm-cancel"
                  onClick={() => setCategoryModalOpen(false)}
                  disabled={savingCategory}
                >
                  Cancel
                </button>
                <button type="submit" className="confirm-create" disabled={savingCategory}>
                  {savingCategory ? "Saving..." : "Add Category"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </Layout>
  );
};

export default AddBook;
