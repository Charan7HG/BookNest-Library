import { Navigate, Route, Routes } from "react-router-dom";
import Home from "./pages/Home";
import Books from "./pages/Books";
import Authors from "./pages/Authors";
import Categories from "./pages/Categories";
import Users from "./pages/Users";
import MyBorrowRequests from "./pages/MyBorrowRequests";
import BorrowRequests from "./pages/BorrowRequests";
import PasswordResetRequests from "./pages/PasswordResetRequests";
import ActiveLoans from "./pages/ActiveLoans";
import Login from "./pages/Login";
import ForgotPassword from "./pages/ForgotPassword";
import ChangePassword from "./pages/ChangePassword";
import AddBook from "./pages/AddBook";
import EditBook from "./pages/EditBook";
import AddUser from "./pages/AddUser";
import EditUser from "./pages/EditUser";
import AddAuthor from "./pages/AddAuthor";
import EditAuthor from "./pages/EditAuthor";
import AddCategory from "./pages/AddCategory";
import EditCategory from "./pages/EditCategory";
import ProtectedRoute from "./pages/ProtectedRoute";

const CATALOG_MANAGERS = ["ADMIN", "LIBRARIAN"];
const ADMIN_ONLY = ["ADMIN"];
const MEMBER_ONLY = ["MEMBER"];

const App = () => {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/forgot-password" element={<ForgotPassword />} />

      {/* Shared by ADMIN, LIBRARIAN and MEMBER. Temporary-password users are
          allowed to stay on this page while every other page redirects here. */}
      <Route
        path="/change-password"
        element={
          <ProtectedRoute>
            <ChangePassword />
          </ProtectedRoute>
        }
      />

      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Home />
          </ProtectedRoute>
        }
      />

      <Route path="/dashboard" element={<Navigate to="/" replace />} />

      <Route
        path="/books"
        element={
          <ProtectedRoute>
            <Books />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrow-requests/my"
        element={
          <ProtectedRoute allowedRoles={MEMBER_ONLY}>
            <MyBorrowRequests />
          </ProtectedRoute>
        }
      />

      <Route
        path="/borrow-requests"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <BorrowRequests />
          </ProtectedRoute>
        }
      />

      <Route
        path="/password-reset-requests"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <PasswordResetRequests />
          </ProtectedRoute>
        }
      />

      <Route
        path="/loans/active"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <ActiveLoans />
          </ProtectedRoute>
        }
      />

      <Route
        path="/add-book"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <AddBook />
          </ProtectedRoute>
        }
      />

      <Route
        path="/edit-book/:id"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <EditBook />
          </ProtectedRoute>
        }
      />

      <Route
        path="/authors"
        element={
          <ProtectedRoute>
            <Authors />
          </ProtectedRoute>
        }
      />

      <Route
        path="/add-author"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <AddAuthor />
          </ProtectedRoute>
        }
      />

      <Route
        path="/edit-author/:id"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <EditAuthor />
          </ProtectedRoute>
        }
      />

      <Route
        path="/categories"
        element={
          <ProtectedRoute>
            <Categories />
          </ProtectedRoute>
        }
      />

      <Route
        path="/add-category"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <AddCategory />
          </ProtectedRoute>
        }
      />

      <Route
        path="/edit-category/:id"
        element={
          <ProtectedRoute allowedRoles={CATALOG_MANAGERS}>
            <EditCategory />
          </ProtectedRoute>
        }
      />

      <Route
        path="/users"
        element={
          <ProtectedRoute allowedRoles={ADMIN_ONLY}>
            <Users />
          </ProtectedRoute>
        }
      />

      <Route
        path="/add-user"
        element={
          <ProtectedRoute allowedRoles={ADMIN_ONLY}>
            <AddUser />
          </ProtectedRoute>
        }
      />

      <Route
        path="/edit-user/:id"
        element={
          <ProtectedRoute allowedRoles={ADMIN_ONLY}>
            <EditUser />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
};

export default App;
