import { useCallback, useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import Spinner from "../ccomponents/Spinner";
import StatusBadge from "../ccomponents/StatusBadge";
import { getMyBorrowRequests } from "../ccomponents/service/BorrowingService";
import { formatDateTime, getErrorMessage } from "../ccomponents/borrowUtils";
import { toast } from "react-toastify";
import "./Books.css";

const MyBorrowRequests = () => {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);

  const loadRequests = useCallback(async () => {
    setLoading(true);

    try {
      const response = await getMyBorrowRequests();
      setRequests(response.data.data || []);
    } catch (error) {
      toast.error(
        getErrorMessage(error, "Failed to load your borrow requests")
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;

    getMyBorrowRequests()
      .then((response) => {
        if (active) {
          setRequests(response.data.data || []);
        }
      })
      .catch((error) => {
        if (active) {
          toast.error(
            getErrorMessage(error, "Failed to load your borrow requests")
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

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
        <h1>My Borrow Requests</h1>

        <div className="page-actions">
          <button type="button" onClick={loadRequests}>
            Refresh
          </button>
        </div>
      </div>

      <p className="borrow-hint">
        Library staff review pending requests. A request is approved only when a
        physical copy can be issued.
      </p>

      <div className="table-scroll">
        <table className="book-table">
          <thead>
            <tr>
              <th>Book</th>
              <th>Requested</th>
              <th>Status</th>
              <th>Reviewed By</th>
              <th>Reviewed At</th>
              <th>Loan Details</th>
            </tr>
          </thead>

          <tbody>
            {requests.length === 0 ? (
              <tr>
                <td colSpan="6">
                  You have not submitted any borrow requests yet.
                </td>
              </tr>
            ) : (
              requests.map((request) => (
                <tr key={request.requestId}>
                  <td>
                    {request.bookTitle || `Book #${request.bookId}`}
                    <span className="detail-note">
                      Request #{request.requestId}
                    </span>
                  </td>
                  <td className="nowrap">
                    {formatDateTime(request.requestedAt)}
                  </td>
                  <td>
                    <StatusBadge status={request.status} />
                  </td>
                  <td>
                    {request.reviewedByName ||
                      (request.reviewedById
                        ? `User #${request.reviewedById}`
                        : "—")}
                  </td>
                  <td className="nowrap">
                    {formatDateTime(request.reviewedAt)}
                  </td>
                  <td>
                    {request.loanId ? (
                      <>
                        <span>Loan #{request.loanId}</span>
                        <span className="detail-note">
                          Due: {formatDateTime(request.dueDate)}
                        </span>
                        {request.returnDate && (
                          <span className="detail-note">
                            Returned: {formatDateTime(request.returnDate)}
                          </span>
                        )}
                      </>
                    ) : (
                      "—"
                    )}
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

export default MyBorrowRequests;
