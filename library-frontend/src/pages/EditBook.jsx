import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getBookById, updateBook } from "../ccomponents/service/BookService";
import { toast } from "react-toastify";
import { getAllAuthors } from "../ccomponents/service/AuthorService";
import { getAllCategories } from "../ccomponents/service/CategoryService";
import Spinner from "../ccomponents/Spinner";
import "./Forms.css";

const EARLIEST_PUBLISHED_YEAR = 1000;

const EditBook = () => {
  const { id } = useParams();
  return <EditBookForm key={id} id={id} />;
};

const EditBookForm = ({ id }) => {
  const navigate = useNavigate();

  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [price, setPrice] = useState("");
  const [publishedYear, setPublishedYear] = useState("");
  const [language, setLanguage] = useState("");
  const [selectedAuthor, setSelectedAuthor] = useState("");
  const [authors, setAuthors] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategories, setSelectedCategories] = useState([]);
  const [copyCounts, setCopyCounts] = useState({
    totalCopies: 0,
    availableCopies: 0,
    onLoanCopies: 0,
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState("");

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
          setName(book.name || "");
          setDescription(book.description || "");
          setPrice(book.price ?? "");
          setPublishedYear(book.publishedYear ?? "");
          setLanguage(book.language || "");
          setSelectedAuthor(book.author?.id || "");
          setSelectedCategories(
            book.category ? book.category.map((category) => category.id) : []
          );
          setCopyCounts({
            totalCopies: book.totalCopies ?? 0,
            availableCopies: book.availableCopies ?? 0,
            onLoanCopies: book.onLoanCopies ?? 0,
          });
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

    if (!name.trim()) {
      setFormError("Book name is required.");
      toast.warning("Book name is required.");
      return;
    }

    if (price === "" || Number.isNaN(Number(price)) || Number(price) < 0) {
      setFormError("Price must be zero or greater.");
      toast.warning("Price must be zero or greater.");
      return;
    }

    const year = Number(publishedYear);
    if (
      publishedYear === "" ||
      !Number.isInteger(year) ||
      year < EARLIEST_PUBLISHED_YEAR ||
      year > latestPublishedYear
    ) {
      const message = `Published year must be a whole number between ${EARLIEST_PUBLISHED_YEAR} and ${latestPublishedYear}.`;
      setFormError(message);
      toast.warning(message);
      return;
    }

    if (!language.trim()) {
      setFormError("Language is required.");
      toast.warning("Language is required.");
      return;
    }

    if (selectedAuthor === "") {
      setFormError("Please select an author.");
      toast.warning("Please select an author.");
      return;
    }

    if (selectedCategories.length === 0) {
      setFormError("Please select at least one category.");
      toast.warning("Please select at least one category.");
      return;
    }

    setFormError("");
    setSaving(true);

    try {
      const book = {
        name: name.trim(),
        description: description.trim(),
        price: Number(price),
        publishedYear: Number(publishedYear),
        language: language.trim(),
        author: {
          id: Number(selectedAuthor),
        },
        category: selectedCategories.map((categoryId) => ({
          id: Number(categoryId),
        })),
      };

      await updateBook(id, book);

      toast.success("Book Updated Successfully");
      navigate("/books");
    } catch (error) {
      const message = error.response?.data?.message || "Update Failed";
      setFormError(message);
      toast.error(message);
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

      <form className="form-container" onSubmit={handleUpdate} noValidate>
        {formError && (
          <p className="form-error" role="alert">
            {formError}
          </p>
        )}

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
          <label htmlFor="edit-book-description">Description</label>
          <textarea
            id="edit-book-description"
            rows="3"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
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
          <label htmlFor="edit-book-published-year">Published Year</label>
          <input
            id="edit-book-published-year"
            type="number"
            min={EARLIEST_PUBLISHED_YEAR}
            max={latestPublishedYear}
            step="1"
            value={publishedYear}
            onChange={(e) => setPublishedYear(e.target.value)}
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="edit-book-language">Language</label>
          <input
            id="edit-book-language"
            type="text"
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
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

        <div className="form-group">
          <p className="form-group-label">Copies (read-only)</p>
          <p className="form-hint">
            Total: {copyCounts.totalCopies} | Available:{" "}
            {copyCounts.availableCopies} | On Loan: {copyCounts.onLoanCopies}
          </p>
        </div>

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
