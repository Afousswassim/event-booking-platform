(() => {
    const toolbar = document.querySelector('.ticket-toolbar');
    if (toolbar) {
        const search = document.getElementById('ticket-search');
        const filters = [...toolbar.querySelectorAll('[data-ticket-filter]')];
        const results = document.getElementById('ticket-results');
        const empty = document.getElementById('ticket-no-match');
        const fold = value => value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
        const tickets = [...document.querySelectorAll('[data-ticket]')].map(card => ({
            card,
            searchable: fold([card.querySelector('h3'), card.querySelector('.ticket-reference'),
                card.querySelector('.ticket-code')].map(node => node.textContent).join(' '))
        }));
        let status = 'all';

        function render() {
            const words = fold(search.value).split(/\s+/).filter(Boolean);
            let count = 0;
            for (const { card, searchable } of tickets) {
                const visible = (status === 'all' || card.dataset.status === status)
                    && words.every(word => searchable.includes(word));
                card.hidden = !visible;
                card.classList.toggle('is-featured', visible && count === 0);
                if (visible) count++;
            }
            for (const filter of filters) filter.setAttribute('aria-pressed', String(filter.dataset.ticketFilter === status));
            results.textContent = count + (count === 1 ? ' billet affiché' : ' billets affichés');
            empty.hidden = count > 0;
        }

        for (const filter of filters) {
            filter.addEventListener('click', () => {
                status = filter.dataset.ticketFilter;
                render();
            });
        }
        search.addEventListener('input', render);
        search.form.addEventListener('submit', event => {
            event.preventDefault();
            render();
        });
        document.getElementById('ticket-reset').addEventListener('click', () => {
            status = 'all';
            search.value = '';
            render();
            search.focus();
        });
        toolbar.hidden = false;
        results.hidden = false;
        render();
    }

    const print = document.querySelector('[data-print-ticket]');
    const copy = document.querySelector('[data-copy-ticket]');
    if (print && copy) {
        const code = document.querySelector('.ticket-code');
        const feedback = document.querySelector('.ticket-feedback');
        print.addEventListener('click', () => window.print());
        copy.addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(code.textContent);
                feedback.textContent = 'Code copié.';
            } catch {
                const range = document.createRange();
                range.selectNodeContents(code);
                const selection = window.getSelection();
                selection.removeAllRanges();
                selection.addRange(range);
                feedback.textContent = 'Le code est sélectionné. Utilisez la commande Copier de votre navigateur.';
            }
        });
        print.hidden = copy.hidden = false;
    }
})();
