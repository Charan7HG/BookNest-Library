import { useCallback, useEffect, useState } from "react";
import Layout from "../ccomponents/Layout";
import Spinner from "../ccomponents/Spinner";
import StatusBadge from "../ccomponents/StatusBadge";
import ConfirmDialog from "../ccomponents/ConfirmDialog";
import {
  getActiveLoans,
  returnLoan,
} from "../ccomponents/service/BorrowingService";
import { formatDateTime, getErrorMessage } from "../ccomponents/borrowUtils";
import { toast } from "react-toastify";
import "./Books.css";

const ActiveLoans = () => {
  const [loans, setLoans] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogLoan, setDialogLoan] = useState(null);
  const [returning, setReturning] = useState(false);

  const loadLoans = useCallback(async (showSpinner = true) => {
    if (showSpinner) {
      setLoading(true);
    }

    try {
      const response = await getActiveLoans();
      setLoans(response.data.data || []);
    } catch (error) {
      toast.error(getErrorMessage(error, "Failed to load active loans"));
    } finally {
      if (showSpinner) {
        setLoading(false);
      }
    }
  }, []);

  useEffect(() => {
    let active = true;

    getActiveLoans()
      .then((response) => {
        if (active) {
          setLoans(response.data.data || []);
        }
      })
      .catch((error) => {
        if (active) {
          toast.error(getErrorMessage(error, "Failed to load active loans"));
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

  const handleConfirmReturn = async () => {
    if (!dialogLoan || returning) {
      return;
    }

    setReturning(true);

    try {
      const response = await returnLoan(dialogLoan.loanId);
      toast.success(response.data?.message || "Book returned successfully.");
      setDialogLoan(null);
      await loadLoans(false);
    } catch (error) {
      toast.error(
        getErrorMessage(error, "Failed to return the book. Please try again.")
      );
      setDialogLoan(null);
      await loadLoans(false);
    } finally {
      setReturning(false);
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
        <h1>Active Loans</h1>

        <div className="page-actions">
          <button type="button" onClick={() => loadLoans()}>
            Refresh
          </button>
        </div>
      </div>

      <p className="borrow-hint">
        Legacy loans were migrated from the previous system, so their issue and
        due dates are not recorded.
      </p>

      <div className="table-scroll">
        <table className="book-table">
          <thead>
            <tr>
              <th>Book</th>
              <th>Copy</th>
              <th>Member</th>
              <th>Issued</th>
              <th>Due</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>

          <tbody>
            {loans.length === 0 ? (
              <tr>
                <td colSpan="7">No active loans.</td>
              </tr>
            ) : (
              loans.map((loan) => (
                <tr key={loan.loanId}>
                  <td>
                    {loan.bookTitle || `Book #${loan.bookId}`}
                    <span className="detail-note">Loan #{loan.loanId}</span>
                  </td>
                  <td>
                    {loan.copyCode || `Copy #${loan.copyId}`}
                    {loan.legacy && (
                      <span className="detail-note">Legacy loan</span>
                    )}
                  </td>
                  <td>
                    {loan.memberName || `Member #${loan.memberId}`}
                    {loan.memberEmail && (
                      <span className="detail-note">{loan.memberEmail}</span>
                    )}
                  </td>
                  <td className="nowrap">{formatDateTime(loan.issueDate)}</td>
                  <td className="nowrap">{formatDateTime(loan.dueDate)}</td>
                  <td>
                    <StatusBadge status={loan.status} />
                  </td>
                  <td className="nowrap">
                    <div className="action-buttons">
                      <button
                        type="button"
                        className="request-btn"
                        onClick={() => setDialogLoan(loan)}
                        disabled={loan.status !== "ACTIVE" || returning}
                      >
                        Return Book
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <ConfirmDialog
        open={dialogLoan !== null}
        title="Return Book"
        message={
          dialogLoan
            ? `Return "${dialogLoan.bookTitle || `Book #${dialogLoan.bookId}`}" borrowed by ${
                dialogLoan.memberName || `member #${dialogLoan.memberId}`
              }?`
            : ""
        }
        confirmLabel="Return Book"
        busy={returning}
        onConfirm={handleConfirmReturn}
        onCancel={() => setDialogLoan(null)}
      />
    </Layout>
  );
};

export default ActiveLoans;
