export default function ApproveModal({
  setShowApproveModal,
  handleDeleteDocument,
}) {
  function handleYes() {
    handleDeleteDocument();
    setShowApproveModal(false);
  }

  function handleNo() {
    setShowApproveModal(false);
  }

  return (
    <>
      <div class="modal-overlay">
    <div class="modal">
      <h2>Ви справді бажаєте<br/>видалити це призначення?</h2>
      <p>Після підтвердження його буде<br/>неможливо відновити</p>
      <div className="buttons-modal">
        <button type="button" className="cancel-btn" onClick={handleNo}>Скасувати</button>
        <button type="button" className="delete-btn" onClick={handleYes}>Видалити</button>
      </div>
    </div>
  </div>
    </>
  );
}
