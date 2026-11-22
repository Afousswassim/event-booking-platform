(() => {
    const form = document.getElementById('booking-form');
    if (!form) return;
    const steps = [...form.querySelectorAll('.booking-step')];
    const progress = document.querySelector('.booking-progress');
    const markers = [...progress.children];
    const quantity = document.getElementById('booking-quantity');
    const controls = [...form.querySelectorAll('[data-quantity-change]')];
    const totals = [...document.querySelectorAll('[data-booking-total]')];
    const quantities = [...document.querySelectorAll('[data-booking-quantity]')];
    const confirm = form.querySelector('[type=submit]');
    const name = document.getElementById('booking-name');
    const email = document.getElementById('booking-email');
    const money = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 2 });
    const unit = Number(form.dataset.unitCents);
    let current = 0;
    let submitting = false;

    function updateTotal() {
        const count = quantity.valueAsNumber;
        const valid = quantity.validity.valid && Number.isInteger(count);
        const total = valid ? money.format(unit * count / 100) + ' DH' : '—';
        for (const output of totals) output.textContent = total;
        for (const output of quantities) output.textContent = valid ? count : '—';
        controls[0].disabled = valid && count <= Number(quantity.min);
        controls[1].disabled = valid && count >= Number(quantity.max);
    }

    function show(index, focus = true) {
        current = index;
        steps.forEach((step, position) => { step.hidden = position !== index; });
        markers.forEach((marker, position) => {
            marker.classList.toggle('booking-passed', position < index);
            if (position === index) marker.setAttribute('aria-current', 'step');
            else marker.removeAttribute('aria-current');
        });
        if (focus) steps[index].querySelector('h2').focus();
    }

    function advance() {
        if (current === 0 && !quantity.reportValidity()) return;
        if (current === 1 && (!name.value.trim() || !email.value.includes('@'))) {
            const message = document.getElementById('booking-contact-error');
            message.hidden = false;
            message.focus();
            return;
        }
        show(current + 1);
    }

    for (const control of controls) {
        control.addEventListener('click', () => {
            const value = quantity.validity.valid ? quantity.valueAsNumber : Number(quantity.min);
            quantity.value = Math.min(Number(quantity.max), Math.max(Number(quantity.min),
                value + Number(control.dataset.quantityChange)));
            updateTotal();
        });
    }
    quantity.addEventListener('input', updateTotal);
    for (const button of form.querySelectorAll('[data-booking-next]')) button.addEventListener('click', advance);
    for (const button of form.querySelectorAll('[data-booking-back]')) {
        button.addEventListener('click', () => show(current - 1));
    }

    // Validate only the visible step first so an invisible control never steals focus.
    form.noValidate = true;
    form.addEventListener('submit', event => {
        if (submitting || current < 2) {
            event.preventDefault();
            if (!submitting) advance();
            return;
        }
        if (!quantity.validity.valid) {
            event.preventDefault();
            show(0);
            quantity.reportValidity();
            return;
        }
        if (!form.elements.confirmed.reportValidity()) {
            event.preventDefault();
            return;
        }
        submitting = true;
        confirm.disabled = true;
        form.setAttribute('aria-busy', 'true');
    });

    window.addEventListener('pageshow', () => {
        submitting = false;
        confirm.disabled = false;
        form.removeAttribute('aria-busy');
        updateTotal();
    });

    quantity.readOnly = false;
    form.dataset.enhanced = '';
    progress.hidden = false;
    for (const button of form.querySelectorAll('button[hidden]')) button.hidden = false;
    updateTotal();
    show(0, false);
})();
