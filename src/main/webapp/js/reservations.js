(() => {
    const toolbar = document.querySelector('.reservation-toolbar');
    if (!toolbar) return;
    const search = document.getElementById('reservation-search');
    const sort = document.getElementById('reservation-sort');
    const filters = [...toolbar.querySelectorAll('[data-reservation-filter]')];
    const result = document.getElementById('reservation-results');
    const empty = document.getElementById('reservation-no-match');
    const list = document.getElementById('reservation-list');
    const more = document.getElementById('reservation-more');
    const moreCount = document.getElementById('reservation-more-count');
    const fold = value => value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
    const cards = [...list.querySelectorAll('[data-reservation]')].map(card => ({
        card,
        group: card.dataset.group,
        eventDate: card.dataset.eventDate,
        bookedAt: card.dataset.bookedAt,
        searchable: fold(card.querySelector('h3').textContent + ' ' + card.querySelector('.reservation-reference').textContent)
    }));
    const groupOrder = {upcoming: 0, finished: 1, cancelled: 2};
    let active = filters.find(filter => filter.getAttribute('aria-pressed') === 'true').dataset.reservationFilter;

    const sorters = {
        upcoming: (a, b) => groupOrder[a.group] - groupOrder[b.group]
            || (a.group === 'upcoming' ? a.eventDate.localeCompare(b.eventDate) : b.bookedAt.localeCompare(a.bookedAt)),
        recent: (a, b) => b.bookedAt.localeCompare(a.bookedAt),
        'event-date': (a, b) => b.eventDate.localeCompare(a.eventDate)
    };

    function render() {
        const words = fold(search.value).split(/\s+/).filter(Boolean);
        const visible = [];
        const hidden = [];
        for (const entry of cards) {
            const matches = (active === 'all' || active === entry.group) && words.every(word => entry.searchable.includes(word));
            entry.card.hidden = !matches;
            entry.card.classList.remove('is-featured');
            (matches ? visible : hidden).push(entry);
        }
        visible.sort(sorters[sort.value]);
        const visibleNodes = visible.map(entry => entry.card);
        const featured = visibleNodes.shift();
        if (featured) featured.classList.add('is-featured');
        list.replaceChildren(...(featured ? [featured, more, ...visibleNodes] : [more]), ...hidden.map(entry => entry.card));
        more.hidden = visibleNodes.length === 0;
        moreCount.textContent = visibleNodes.length + (visibleNodes.length === 1 ? ' réservation' : ' réservations');
        for (const filter of filters) filter.setAttribute('aria-pressed', String(filter.dataset.reservationFilter === active));
        result.textContent = visible.length + (visible.length === 1 ? ' réservation affichée' : ' réservations affichées');
        empty.hidden = visible.length > 0;
    }

    for (const filter of filters) {
        filter.addEventListener('click', () => {
            active = filter.dataset.reservationFilter;
            render();
        });
    }
    search.addEventListener('input', render);
    sort.addEventListener('change', render);
    search.form.addEventListener('submit', event => {
        event.preventDefault();
        render();
    });
    document.getElementById('reservation-reset').addEventListener('click', () => {
        search.value = '';
        sort.value = 'upcoming';
        active = 'all';
        render();
        search.focus();
    });
    toolbar.hidden = false;
    result.hidden = false;
    render();
})();
