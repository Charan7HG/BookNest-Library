import { useEffect } from "react";

const ConfirmDialog = ({
    open,
    title,
    message,
    confirmLabel = "Confirm",
    cancelLabel = "Cancel",
    busy = false,
    onConfirm,
    onCancel,
}) => {
    useEffect(() => {
        if (!open) {
            return undefined;
        }

        const handleKeyDown = (event) => {
            if (event.key === "Escape" && !busy) {
                onCancel();
            }
        };

        window.addEventListener("keydown", handleKeyDown);

        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [open, busy, onCancel]);

    if (!open) {
        return null;
    }

    return (
        <div
            className="confirm-backdrop"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !busy) {
                    onCancel();
                }
            }}
        >
            <div
                className="confirm-dialog"
                role="dialog"
                aria-modal="true"
                aria-labelledby="confirm-dialog-title"
            >
                <h2 id="confirm-dialog-title">{title}</h2>
                <p>{message}</p>

                <div className="confirm-actions">
                    <button
                        type="button"
                        className="confirm-cancel"
                        onClick={onCancel}
                        disabled={busy}
                        autoFocus
                    >
                        {cancelLabel}
                    </button>

                    <button
                        type="button"
                        className="confirm-primary"
                        onClick={onConfirm}
                        disabled={busy}
                    >
                        {busy ? "Please wait..." : confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmDialog;
