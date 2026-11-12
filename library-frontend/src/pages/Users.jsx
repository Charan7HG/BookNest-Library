import { useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import {
  getAllUsers,
  deleteUser,
  getUserById,
  searchUserByName,
} from "../ccomponents/service/UserService";
import { useNavigate } from "react-router-dom";
import Spinner from "../ccomponents/Spinner";
import { toast } from "react-toastify";
import "./Books.css";

const Users = () => {
  const [users, setUsers] = useState([]);
  const [searchId, setSearchId] = useState("");
  const [searchName, setSearchName] = useState("");
  const [loading, setLoading] = useState(true);

  const navigate = useNavigate();

  useEffect(() => {
    let active = true;

    getAllUsers()
      .then((response) => {
        if (active) {
          setUsers(response.data.data);
        }
      })
      .catch((error) => {
        if (active) {
          toast.error(error.response?.data?.message || "Failed to Load Users");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await getAllUsers();
      setUsers(response.data.data);
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Load Users");
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    const confirmRemove = window.confirm(
      "Are you sure you want to remove this User?"
    );

    if (!confirmRemove) {
      return;
    }

    try {
      await deleteUser(id);
      toast.success("User Removed Successfully");
      fetchUsers();
    } catch (error) {
      toast.error(error.response?.data?.message || "Failed to Delete User");
    }
  };

  const handleSearch = async (event) => {
    event.preventDefault();

    try {
      if (searchId !== "") {
        const response = await getUserById(searchId);
        const user = response.data.data;

        if (user) {
          setUsers([user]);
        } else {
          toast.error("User Not Found");
          setUsers([]);
        }
      } else if (searchName !== "") {
        const response = await searchUserByName(searchName);
        setUsers(response.data.data);
      } else {
        toast.warning("Please enter User ID or Name");
      }
    } catch (error) {
      toast.error(error.response?.data?.message || "User Not Found");
      setUsers([]);
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
        <h1>Users</h1>

        <button type="button" onClick={() => navigate("/add-user")}>
          Add User
        </button>
      </div>

      <form className="search-container" onSubmit={handleSearch}>
        <input
          type="number"
          placeholder="Search By ID"
          aria-label="Search users by ID"
          value={searchId}
          onChange={(e) => setSearchId(e.target.value)}
        />

        <input
          type="text"
          placeholder="Search By Name"
          aria-label="Search users by name"
          value={searchName}
          onChange={(e) => setSearchName(e.target.value)}
        />

        <button type="submit">Search</button>

        <button
          type="button"
          onClick={() => {
            setSearchId("");
            setSearchName("");
            fetchUsers();
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
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Role</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {users.length === 0 ? (
              <tr>
                <td colSpan="6">No Users Found</td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.name}</td>
                  <td>{user.profile?.email}</td>
                  <td>{user.profile?.phone}</td>
                  <td>
                    <span className="role-pill">{user.role}</span>
                  </td>
                  <td>
                    <div className="action-buttons">
                      <button
                        type="button"
                        onClick={() => navigate(`/edit-user/${user.id}`)}
                      >
                        Edit
                      </button>

                      <button
                        type="button"
                        onClick={() => handleDelete(user.id)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </Layout>
  );
};

export default Users;
