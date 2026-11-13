import { useCallback, useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import Spinner from "../ccomponents/Spinner";
import StatusBadge from "../ccomponents/StatusBadge";
import ConfirmDialog from "../ccomponents/ConfirmDialog";
import {
  approvePasswordResetRequest,
  getPasswordResetRequestHistory,
  getPendingPasswordResetRequests,
  rejectPasswordResetRequest,
} from "../ccomponents/service/PasswordResetService";
import { formatDateTime, getErrorMessage } from "../ccomponents/borrowUtils";
import { getStoredUser } from "../ccomponents/service/AuthService";
import { toast } from "react-toastify";
import "./Books.css";

const PasswordResetRequests = () => {
  const [queue, setQueue] = useState("pending");
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState(false);
  const [dialog, setDialog] = useState(null);
  // Kept in component state only. Never write this to localStorage,
  // sessionStorage, the URL, or any other persistent storage.
  const [temporaryPassword, setTemporaryPassword] = useState(null);

  const currentUser = getStoredUser();
  const currentRole = currentUser?.role;

  // Mirrors the backend matrix so the UI never offers an action the server
  // would reject: ADMIN handles MEMBER and LIBRARIAN requests, while a
  // LIBRARIAN handles MEMBER requests only and never their own account.
  const canProcess = (request) => {
    if (!request || request.memberId === currentUser?.id) {
      return false;
    }

    if (currentRole === "ADMIN") {
      return request.accountRole === "MEMBER" || request.accountRole === "LIBRARIAN";
    }

    if (currentRole === "LIBRARIAN") {
      return request.accountRole === "MEMBER";
    }

    return false;
  };

  const loadRequests = useCallback(
    async (showSpinner = true) => {
      if (showSpinner) {
        setLoading(true);
      }

      try {
        const response =
          queue === "pending"
            ? await getPendingPasswordResetRequests()
            : await getPasswordResetRequestHistory();
        setRequests(response.data.data || []);
      } catch (error) {
        toast.error(
          getErrorMessage(error, "Failed to load password reset requests")
        );
      } finally {
        if (showSpinner) {
          setLoading(false);
        }
      }
    },
    [queue]
  );

  useEffect(() => {
    let active = true;

    const request =
      queue === "pending"
        ? getPendingPasswordResetRequests()
        : getPasswordResetRequestHistory();

    request
      .then((response) => {
        if (active) {
          setRequests(response.data.data || []);
        }
      })
      .catch((error) => {
        if (active) {
          toast.error(
            getErrorMessage(error, "Failed to load password reset requests")
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
  }, [queue]);

  const switchQueue = (nextQueue) => {
    if (nextQueue === queue) {
      return;
    }

    // The one-time password belongs to the action just performed, not to a
    // queue view. Clear it when the user changes context.
    setTemporaryPassword(null);
    setRequests([]);
    setLoading(true);
    setQueue(nextQueue);
  };

  const handleConfirm = async () => {
    if (!dialog || processing) {
      return;
    }

    const { request, action } = dialog;
    setProcessing(true);

    try {
      if (action === "approve") {
        const response = await approvePasswordResetRequest(request.requestId);
        const data = response.data?.data;

        if (data?.temporaryPassword) {
          setTemporaryPassword({
            email: data.email || request.memberEmail,
            password: data.temporaryPassword,
          });
        }

        toast.success("Password reset approved.");
      } else {
        await rejectPasswordResetRequest(request.requestId);
        toast.success("Password reset rejected.");
      }

      setDialog(null);
      await loadRequests(false);
    } catch (error) {
      toast.error(
        getErrorMessage(
          error,
          action === "approve"
            ? "Failed to approve the password reset request."
            : "Failed to reject the password reset request."
        )
      );
    } finally {
      setProcessing(false);
    }
  };

  const closeTemporaryPassword = () => {
    setTemporaryPassword(null);
  };

  const copyTemporaryPassword = async () => {
    if (!temporaryPassword) {
      return;
    }

    try {
      await navigator.clipboard.writeText(temporaryPassword.password);
      toast.success("Temporary password copied.");
    } catch {
      toast.info("Select the password and copy it manually.");
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
        <h1>Password Reset Requests</h1>

        <div className="page-actions">
          <button
            type="button"
            className={queue === "pending" ? "active-filter" : ""}
            aria-pressed={queue === "pending"}
            onClick={() => switchQueue("pending")}
          >
            Pending
          </button>
          <button
            type="button"
            className={queue === "history" ? "active-filter" : ""}
            aria-pressed={queue === "history"}
            onClick={() => switchQueue("history")}
          >
            History
          </button>
          <button type="button" onClick={() => loadRequests()}>
            Refresh
          </button>
        </div>
      </div>

      <p className="borrow-hint">
        {currentRole === "ADMIN"
          ? "ADMIN reviews both MEMBER and LIBRARIAN requests. Approving generates a one-time temporary password; rejecting never changes the password."
          : "LIBRARIAN reviews MEMBER requests only. LIBRARIAN password requests are handled by an ADMIN. Approving generates a one-time temporary password."}
      </p>

      <div className="table-scroll">
        <table className="book-table">
          <thead>
            <tr>
              <th>Member</th>
              <th>Account</th>
              <th>Requested</th>
              <th>Status</th>
              <th>Reviewed By</th>
              <th>Reviewed At</th>
              {queue === "pending" && <th>Actions</th>}
            </tr>
          </thead>

          <tbody>
            {requests.length === 0 ? (
              <tr>
                <td colSpan={queue === "pending" ? 7 : 6}>
                  {queue === "pending"
                    ? "No pending password reset requests."
                    : "No password reset request history yet."}
                </td>
              </tr>
            ) : (
              requests.map((request) => (
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
                    <span className="role-pill">
                      {request.accountRole || "UNKNOWN"}
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
                  {queue === "pending" && (
                    <td className="nowrap">
                      {canProcess(request) ? (
                        <div className="action-buttons">
                          <button
                            type="button"
                            onClick={() =>
                              setDialog({ request, action: "approve" })
                            }
                          >
                            Approve
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              setDialog({ request, action: "reject" })
                            }
                          >
                            Reject
                          </button>
                        </div>
                      ) : (
                        <span className="detail-note">
                          {request.memberId === currentUser?.id
                            ? "Own account — use Change Password"
                            : "ADMIN review required"}
                        </span>
                      )}
                    </td>
                  )}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <ConfirmDialog
        open={dialog !== null}
        title={
          dialog?.action === "approve"
            ? "Approve Password Reset"
            : "Reject Password Reset"
        }
        message={
          dialog
            ? dialog.action === "approve"
              ? `Approve the password reset request for ${
                  dialog.request.memberName ||
                  dialog.request.memberEmail ||
                  `member #${dialog.request.memberId}`
                }? A temporary password will be generated and shown once.`
              : `Reject the password reset request for ${
                  dialog.request.memberName ||
                  dialog.request.memberEmail ||
                  `member #${dialog.request.memberId}`
                }? The member's password will not change.`
            : ""
        }
        confirmLabel={dialog?.action === "approve" ? "Approve" : "Reject"}
        busy={processing}
        onConfirm={handleConfirm}
        onCancel={() => setDialog(null)}
      />

      {temporaryPassword && (
        <div
          className="confirm-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeTemporaryPassword();
            }
          }}
        >
          <div
            className="confirm-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="temporary-password-title"
          >
            <h2 id="temporary-password-title">Temporary Password</h2>
            <p>
              Share this password securely with{" "}
              {temporaryPassword.email || "the member"}. It is shown only once
              and is never stored by the application.
            </p>

            <code className="temp-password-value">
              {temporaryPassword.password}
            </code>

            <p className="temp-password-warning">
              The member must change it immediately after logging in.
            </p>

            <div className="confirm-actions">
              <button
                type="button"
                className="confirm-cancel"
                onClick={copyTemporaryPassword}
              >
                Copy Password
              </button>
              <button
                type="button"
                className="confirm-primary"
                onClick={closeTemporaryPassword}
                autoFocus
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </Layout>
  );
};

export default PasswordResetRequests;
