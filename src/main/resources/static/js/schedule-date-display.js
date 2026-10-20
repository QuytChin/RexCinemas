(() => {
    const input = document.querySelector('#schedule-filter-date');
    const display = document.querySelector('[data-schedule-date-display]');
    if (!input || !display) return;

    const renderDate = () => {
        const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(input.value);
        display.textContent = match ? `${match[3]}/${match[2]}/${match[1]}` : 'dd/MM/yyyy';
    };

    input.addEventListener('input', renderDate);
    input.addEventListener('change', renderDate);
    renderDate();
})();
