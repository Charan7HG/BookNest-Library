import { NavLink, useLocation } from "react-router-dom";
import { getStoredUser } from "./service/AuthService";
import "./Components.css";

const Sidebar = () => {
  const location = useLocation();
  const role = getStoredUser()?.role;

  const links = [
    { to: "/", label: "Home", icon: "🏠", sections: ["/"] },
    {
      to: "/books",
      label: "Books",
      icon: "📚",
      sections: ["/books", "/add-book", "/edit-book"],
    },
    {
      to: "/authors",
      label: "Authors",
      icon: "👤",
      sections: ["/authors", "/add-author", "/edit-author"],
    },
    {
      to: "/categories",
      label: "Categories",
      icon: "🏷",
      sections: ["/categories", "/add-category", "/edit-category"],
    },
  ];

  if (role === "ADMIN") {
    links.push({
      to: "/users",
      label: "Users",
      icon: "👥",
      sections: ["/users", "/add-user", "/edit-user"],
    });
  }

  const isLinkActive = (link) => {
    if (link.to === "/") {
      return location.pathname === "/";
    }

    return link.sections.some(
      (section) =>
        location.pathname === section ||
        location.pathname.startsWith(`${section}/`)
    );
  };

  return (
    <nav className="sidebar" aria-label="Main navigation">
      <h2 className="logo">📚 Library</h2>

      {links.map((link) => {
        const active = isLinkActive(link);

        return (
          <NavLink
            key={link.to}
            to={link.to}
            end={link.to === "/"}
            className={() => (active ? "active" : "")}
            aria-current={active ? "page" : undefined}
          >
            <span aria-hidden="true">{link.icon}</span> {link.label}
          </NavLink>
        );
      })}
    </nav>
  );
};

export default Sidebar;
