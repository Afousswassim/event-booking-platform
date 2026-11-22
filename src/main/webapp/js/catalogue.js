(() => {
    const get = id => document.getElementById(id);
    const parameters = new URLSearchParams(window.location.search);
    const search = get('explore-query');
    const city = get('explore-city');
    const date = get('explore-date');
    const budget = get('explore-budget');
    const available = get('explore-available');
    const sort = get('explore-sort');
    const maximum = get('explore-price');
    const advanced = get('advanced-filters');
    const advancedToggle = get('advanced-toggle');
    const grid = get('explore-grid');
    const categories = new Map([
        ['', ['Tous les événements', '✳']],
        ['musique', ['Musique', '♫']],
        ['technologie', ['Technologie', '⌘']],
        ['art-culture', ['Art & culture', '✺']],
        ['sport', ['Sport', '◇']],
        ['gastronomie', ['Gastronomie', '♨']]
    ]);
    const cities = [
        'Agadir', 'Béni Mellal', 'Bouskoura', 'Casablanca', 'Dakhla', 'El Jadida', 'Essaouira',
        'Fès', 'Ifrane', 'Kénitra', 'Laâyoune', 'Larache', 'Marrakech', 'Martil', 'Meknès',
        'Mohammédia', 'Nador', 'Ouarzazate', 'Oujda', 'Rabat', 'Safi', 'Salé', 'Tanger',
        'Taroudant', 'Témara', 'Tétouan'
    ];
    const dateFormatter = new Intl.DateTimeFormat('fr-MA', { timeZone: 'Africa/Casablanca', year: 'numeric', month: '2-digit', day: '2-digit' });
    const monthFormatter = new Intl.DateTimeFormat('fr', { timeZone: 'UTC', month: 'long', year: 'numeric' });
    const monthShort = new Intl.DateTimeFormat('fr', { timeZone: 'UTC', month: 'short' });
    const money = new Intl.NumberFormat('fr-MA', { maximumFractionDigits: 2 });

    function fold(value) {
        return value.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();
    }

    function categoryKey(label) {
        const normalized = fold(label);
        if (/musiqu|concert/.test(normalized)) return 'musique';
        if (/tech|innovation|informatique/.test(normalized)) return 'technologie';
        if (/\barts?\b|cultur|exposition|theatre/.test(normalized)) return 'art-culture';
        if (/sport/.test(normalized)) return 'sport';
        if (/gastronom|culinair|cuisine/.test(normalized)) return 'gastronomie';
        return normalized;
    }

    function cityFromVenue(venue) {
        const words = fold(venue).replace(/[^a-z0-9]+/g, ' ');
        const padded = ' ' + words + ' ';
        return cities.find(name => padded.includes(' ' + fold(name) + ' ')) || '';
    }

    function button(label, key, className, icon) {
        const element = document.createElement('button');
        element.type = 'button';
        element.className = className;
        element.value = key;
        element.setAttribute('aria-pressed', 'false');
        if (icon) {
            const symbol = document.createElement('span');
            symbol.setAttribute('aria-hidden', 'true');
            symbol.textContent = icon;
            element.append(symbol);
        }
        element.append(label);
        return element;
    }

    function dateAtMonth(key) {
        return new Date(key + '-01T00:00:00Z');
    }

    // Legacy links use server-side filters; reload with client-side parameters to recover the full catalogue.
    if (parameters.has('q') || parameters.has('category')) {
        const next = new URL(window.location.href);
        if (parameters.get('q')) next.searchParams.set('recherche', parameters.get('q').slice(0, 120));
        if (parameters.get('category')) next.searchParams.set('categorie', categoryKey(parameters.get('category')));
        next.searchParams.delete('q');
        next.searchParams.delete('category');
        window.location.replace(next);
        return;
    }

    const entries = Array.from(grid.querySelectorAll('.explore-card'), card => {
        const start = card.querySelector('time');
        const day = start.dateTime.slice(0, 10);
        const month = day.slice(0, 7);
        const venue = card.querySelector('.explore-card-venue > span:last-child').textContent;
        const cityName = cityFromVenue(venue);
        const categoryLabel = card.dataset.category;
        const key = categoryKey(categoryLabel);
        const price = card.dataset.price === 'Gratuit'
            ? 0
            : Number(card.dataset.price.replace(/[^\d,]/g, '').replace(',', '.'));
        if (!categories.has(key)) categories.set(key, [categoryLabel, '✧']);

        const cityLabel = card.querySelector('.explore-card-city');
        cityLabel.textContent = cityName + ' · ';
        cityLabel.hidden = !cityName;

        const fullDate = document.createElement('span');
        fullDate.className = 'visually-hidden';
        fullDate.textContent = start.textContent.trim();
        const dateNumber = document.createElement('strong');
        dateNumber.setAttribute('aria-hidden', 'true');
        dateNumber.textContent = day.slice(-2);
        const dateMonth = document.createElement('span');
        dateMonth.setAttribute('aria-hidden', 'true');
        dateMonth.textContent = monthShort.format(dateAtMonth(month)).replace('.', '');
        start.replaceChildren(fullDate, dateNumber, dateMonth);

        const searchable = [
            card.querySelector('h3').textContent, venue, cityName, categoryLabel, categories.get(key)[0],
            card.querySelector('.explore-card-description').textContent
        ].map(fold).join(' ');
        return { card, start: start.dateTime, day, month, cityName, key, price, searchable,
            availablePlaces: Number(card.dataset.available) };
    });

    const categoryButtons = Array.from(categories, ([key, [label, icon]]) => button(label, key, 'explore-option', icon));
    const months = [...new Set(entries.map(entry => entry.month))].sort();
    const monthButtons = months.map(key => button(monthFormatter.format(dateAtMonth(key)), key, 'explore-month'));
    const cityNames = [...new Set(entries.map(entry => entry.cityName).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'fr'));
    const priceCeiling = entries.reduce((ceiling, entry) => Math.max(ceiling, Math.ceil(entry.price)), 0);
    maximum.max = priceCeiling;
    maximum.disabled = priceCeiling === 0;
    city.append(...cityNames.map(name => new Option(name, name)));
    city.disabled = cityNames.length === 0;
    get('category-options').append(...categoryButtons);
    get('month-options').append(...monthButtons);
    if (!months.length) get('month-options').textContent = 'Aucun mois disponible';
    if (cityNames.length) {
        get('city-stat').querySelector('strong').textContent = cityNames.length;
        get('city-stat').hidden = false;
    }

    function selectFromUrl(select, key) {
        select.value = parameters.get(key) || '';
        if (select.selectedIndex < 0) select.selectedIndex = 0;
    }

    const parts = Object.fromEntries(dateFormatter.formatToParts(new Date()).map(({ type, value }) => [type, value]));
    const today = parts.year + '-' + parts.month + '-' + parts.day;
    const currentMonth = today.slice(0, 7);
    const nextWeek = new Date(Date.parse(today) + 7 * 86400000).toISOString().slice(0, 10);
    const startOfNextMonth = dateAtMonth(currentMonth);
    startOfNextMonth.setUTCMonth(startOfNextMonth.getUTCMonth() + 1);
    const nextMonth = startOfNextMonth.toISOString().slice(0, 7);
    const dateMatches = {
        week: entry => entry.day >= today && entry.day < nextWeek,
        month: entry => entry.month === currentMonth && entry.day >= today,
        'next-month': entry => entry.month === nextMonth
    };
    const budgetMatches = {
        free: price => price === 0,
        low: price => price <= 150,
        medium: price => price > 150 && price <= 300,
        high: price => price > 300
    };

    let query = (parameters.get('recherche') || '').slice(0, 120).trim();
    let selectedCity = '';
    let category = categories.has(parameters.get('categorie')) ? parameters.get('categorie') : '';
    let month = months.includes(parameters.get('mois')) ? parameters.get('mois') : '';
    const initialPrice = parameters.get('prix');
    maximum.value = initialPrice !== null && /^\d+$/.test(initialPrice) ? initialPrice : priceCeiling;
    search.value = query;
    selectFromUrl(city, 'ville');
    selectedCity = city.value;
    selectFromUrl(date, 'date');
    selectFromUrl(budget, 'budget');
    available.checked = parameters.get('places') === '1';
    selectFromUrl(sort, 'tri');

    function saveState(maxPrice) {
        const url = new URL(window.location.href);
        const state = { recherche: query, ville: selectedCity, categorie: category, date: date.value,
            budget: budget.value, mois: month, prix: maxPrice, tri: sort.value, places: available.checked ? '1' : '' };
        for (const [key, value] of Object.entries(state)) {
            if (value === '' || value === null) url.searchParams.delete(key);
            else url.searchParams.set(key, value);
        }
        if (url.href !== window.location.href) history.replaceState(history.state, '', url);
    }

    function render(updateUrl = true) {
        const terms = fold(query).split(/\s+/).filter(Boolean);
        const maxPrice = Number(maximum.value) < priceCeiling ? Number(maximum.value) : null;
        const matchesDate = dateMatches[date.value];
        const matchesBudget = budgetMatches[budget.value];

        for (const choice of categoryButtons) choice.setAttribute('aria-pressed', String(choice.value === category));
        for (const choice of monthButtons) choice.setAttribute('aria-pressed', String(choice.value === month));

        let resultCount = 0;
        for (const entry of entries) {
            const visible = (!category || entry.key === category)
                && (!selectedCity || entry.cityName === selectedCity)
                && terms.every(term => entry.searchable.includes(term))
                && (!matchesDate || matchesDate(entry))
                && (!matchesBudget || matchesBudget(entry.price))
                && (!available.checked || entry.availablePlaces > 0)
                && (maxPrice === null || entry.price <= maxPrice)
                && (!month || entry.month === month);
            entry.card.hidden = !visible;
            if (visible) resultCount++;
        }

        get('result-count').textContent = resultCount === 1
            ? '1 événement à découvrir'
            : resultCount + ' événements à découvrir';
        get('explore-empty').hidden = resultCount > 0;
        get('explore-empty-title').textContent = entries.length
            ? 'Aucun événement ne correspond à vos filtres.'
            : 'Aucun événement publié pour le moment.';
        get('empty-reset').hidden = !entries.length;
        get('explore-price-value').value = maxPrice === null ? 'Sans limite' : money.format(maxPrice) + ' DH';

        const labels = [
            query && '« ' + query + ' »',
            selectedCity,
            category && categories.get(category)[0],
            date.value && date.selectedOptions[0].textContent,
            budget.value && budget.selectedOptions[0].textContent,
            available.checked && 'Places disponibles',
            maxPrice !== null && 'Prix max. ' + money.format(maxPrice) + ' DH',
            month && monthFormatter.format(dateAtMonth(month))
        ].filter(Boolean);
        const chips = get('active-filters');
        chips.replaceChildren(...labels.map(label => {
            const chip = document.createElement('li');
            chip.textContent = label;
            return chip;
        }));
        chips.hidden = !labels.length;
        if (updateUrl) saveState(maxPrice);
    }

    function orderEntries() {
        entries.sort((a, b) => {
            const byDate = a.start.localeCompare(b.start);
            if (sort.value === 'price-asc') return a.price - b.price || byDate;
            if (sort.value === 'price-desc') return b.price - a.price || byDate;
            const aPast = a.day < today;
            const bPast = b.day < today;
            return Number(aPast) - Number(bPast) || (aPast ? -byDate : byDate);
        });
        grid.append(...entries.map(entry => entry.card));
    }

    function setAdvanced(open) {
        advancedToggle.setAttribute('aria-expanded', String(open));
        advanced.setAttribute('aria-hidden', String(!open));
        advanced.inert = !open;
    }

    function reset() {
        query = search.value = '';
        selectedCity = city.value = '';
        category = month = date.value = budget.value = sort.value = '';
        available.checked = false;
        maximum.value = priceCeiling;
        orderEntries();
        render();
    }

    get('event-search').addEventListener('submit', event => {
        event.preventDefault();
        query = search.value.trim();
        selectedCity = city.value;
        render();
        get('catalogue').scrollIntoView();
    });
    for (const choice of categoryButtons) choice.addEventListener('click', () => {
        category = choice.value;
        render();
    });
    for (const choice of monthButtons) choice.addEventListener('click', () => {
        month = choice.value === month ? '' : choice.value;
        render();
    });
    for (const field of [date, budget, available]) field.addEventListener('change', () => render());
    maximum.addEventListener('input', () => render(false));
    maximum.addEventListener('change', () => render());
    sort.addEventListener('change', () => {
        orderEntries();
        render();
    });
    advancedToggle.addEventListener('click', () => setAdvanced(advanced.inert));
    get('explore-reset').addEventListener('click', reset);
    get('empty-reset').addEventListener('click', event => {
        event.preventDefault();
        reset();
        get('explore-reset').focus();
    });

    orderEntries();
    render(false);
    get('categories').hidden = false;
    get('explore-sort-control').hidden = false;
    city.closest('.explore-city').hidden = false;
})();
