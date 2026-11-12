const StatusBadge = ({ status }) => {
    const normalized = (status || "UNKNOWN").toLowerCase();

    return (
        <span className={`status-badge status-${normalized}`}>
            {status || "UNKNOWN"}
        </span>
    );
};

export default StatusBadge;
