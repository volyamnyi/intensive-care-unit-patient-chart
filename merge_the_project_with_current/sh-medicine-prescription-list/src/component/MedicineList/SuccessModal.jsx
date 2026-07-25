export default function SuccessModal({ setShowSuccessModal, generateDe }) {
  
  function handleOk() {
    setShowSuccessModal(false);
  }

  return (
    <>
      <div class="modal-overlay">
        <div class="modal">
          <h2>
            Листок призначень успішно {generateDe ? "згенеровано у Доктор Елекс" : "збережено"}!
          </h2>
          <div className="buttons-modal">
            <button type="button" className="cancel-btn" onClick={handleOk}>
              ОК
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
