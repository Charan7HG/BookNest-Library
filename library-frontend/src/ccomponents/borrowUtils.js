// Shared formatting and error helpers for the borrowing pages.

export const formatDateTime = (value) => {
    if (!value) {
        return "—";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "—";
    }

    return date.toLocaleString();
};

export const getErrorMessage = (error, fallback) => {
    return error.response?.data?.message || fallback;
};
