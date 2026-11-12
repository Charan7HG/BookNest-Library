import { useCallback, useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import Spinner from "../ccomponents/Spinner";
import StatusBadge from "../ccomponents/StatusBadge";
import {
  approveBorrowRequest,
  getPendingBorrowRequests,
  rejectBorrowRequest,
} from "../ccomponents/service/BorrowingService";
import { formatDateTime, getErrorMessage } from "../ccomponents/borrowUtils";
import { toast } from "react-toastify";
import "./Books.css";

const BorrowRequests = () => {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(null);

  const loadRequests = useCallback(async (showSpinner = true) => {
    if (showSpinner) {
      setLoading(true);
    }

    try {
      const response = await getPendingBorrowRequests();
      setRequests(response.data.data || []);
    } catch (error) {
      toast.error(getErrorMessage(error, "Failed to load pending requests"));
    } finally {
      if (showSpinner) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    let active = true;

    getPendingBorrowRequests()
      .then((response) => {
        if (active) {
          setRequests(response.data.data || []);
        }
      })
      .catch((error) => {
        if (active) {
          toast.error(
            getErrorMessage(error, "Failed to load pending requests")
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

  const handleReview = async (requestId, action) => {
    if (processing !== null) {
      return;
    }

    setProcessing({ requestId, action });

    try {
      const response =
        action === "approve"
          ? await approveBorrowRequest(requestId)
          : await rejectBorrowRequest(requestId);

      toast.success(
        response.data?.message ||
          (action === "approve"
            ? "Borrow request approved."
            : "Borrow request rejected.")
      );

      await loadRequests(false);
    } catch (error) {
      toast.error(
        getErrorMessage(
          error,
          action === "approve"
            ? "Failed to approve the borrow request."
            : "Failed to reject the borrow request."
        )
      );
    } finally {
      setProcessing(null);
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
        <h1>Borrow Requests</h1>

        <div className="page-actions">
          <button type="button" onClick={() => loadRequests()}>
            Refresh
          </button>
        </div>
      </div>

      <p className="borrow-hint">
        Approval can fail when no copy is available or the member has reached
        the active-loan limit. In that case the request stays pending.
      </p>

      <div className="table-scroll">
        <table className="book-table">
          <thead>
            <tr>
              <th>Member</th>
              <th>Book</th>
              <th>Requested</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {requests.length === 0 ? (
              <tr>
                <td colSpan="5">No pending borrow requests.</td>
              </tr>
            ) : (
              requests.map((request) => {
                const isProcessing =
                  processing?.requestId === request.requestId;

                return (
                  <tr key={request.requestId}>
                    <td>
                      {request.memberName || `Member #${request.memberId}`}
                      {request.memberEmail && (
                        <span className="detail-note">
                          {request.memberEmail}
                        </span>
                      )}
                    </td>
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
                    <td className="nowrap">
                      <div className="action-buttons">
                        <button
                          type="button"
                          onClick={() =>
                            handleReview(request.requestId, "approve")
                          }
                          disabled={processing !== null}
                        >
                          {isProcessing && processing.action === "approve"
                            ? "Approving..."
                            : "Approve"}
                        </button>

                        <button
                          type="button"
                          onClick={() =>
                            handleReview(request.requestId, "reject")
                          }
                          disabled={processing !== null}
                        >
                          {isProcessing && processing.action === "reject"
                            ? "Rejecting..."
                            : "Reject"}
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </Layout>
  );
};

export default BorrowRequests;
