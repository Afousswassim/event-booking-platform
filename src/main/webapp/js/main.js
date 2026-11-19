/**
 * EVENTLY — Main JavaScript
 * Interactions, Accessibility, Glassmorphism navbar & UX Prototypes
 */

(function () {
  'use strict';

  // --- DOM Elements ---
  const header = document.getElementById('site-header');
  const globalSearchInput = document.getElementById('global-search-input');
  const searchClearBtn = document.getElementById('search-clear-btn');
  const searchSuggestions = document.getElementById('search-suggestions');
  const searchPill = document.getElementById('search-pill');

  // Mobile navigation elements
  const mobileHamburger = document.getElementById('mobile-hamburger');
  const mobileDrawer = document.getElementById('mobile-drawer');
  const mobileDrawerBackdrop = document.getElementById('mobile-drawer-backdrop');
  const mobileNavBtns = document.querySelectorAll('.mobile-nav-btn');

  // Language dropdown
  const btnLang = document.getElementById('btn-lang');
  const langWrapper = btnLang ? btnLang.closest('.lang-dropdown-wrapper') : null;
  const langItems = document.querySelectorAll('.lang-item');
  const mobileLangOpts = document.querySelectorAll('.mobile-lang-opt');

  // Action buttons
  const btnLogin = document.getElementById('btn-login');
  const btnSignup = document.getElementById('btn-signup');
  const mobileBtnLogin = document.getElementById('mobile-btn-login');
  const mobileBtnSignup = document.getElementById('mobile-btn-signup');
  const btnViewEvent = document.getElementById('btn-view-event');
  const btnExploreEvents = document.getElementById('btn-explore-events');
  const btnDiscoverMusic = document.getElementById('btn-discover-music');

  // Modal elements
  const modalBackdrop = document.getElementById('evently-modal');
  const modalBox = modalBackdrop ? modalBackdrop.querySelector('.evently-modal-box') : null;
  const modalCloseBtn = document.getElementById('modal-close-btn');
  const modalConfirmBtn = document.getElementById('modal-confirm-btn');
  const modalTitle = document.getElementById('modal-title');
  const modalMessage = document.getElementById('modal-message');
  const modalIconBadge = document.getElementById('modal-icon-badge');

  // Toast container
  const toastContainer = document.getElementById('toast-container');

  // ========================================================================
  // 1. NAVBAR SCROLL EFFECT (Glassmorphism transition)
  // ========================================================================
  function handleScroll() {
    if (window.scrollY > 30) {
      header.classList.add('scrolled');
    } else {
      header.classList.remove('scrolled');
    }
  }

  window.addEventListener('scroll', handleScroll, { passive: true });
  handleScroll(); // Initial check

  // ========================================================================
  // 2. DESKTOP DROPDOWNS & KEYBOARD ACCESSIBILITY
  // ========================================================================
  const navItems = document.querySelectorAll('.nav-item.has-dropdown');

  navItems.forEach((item) => {
    const toggle = item.querySelector('.dropdown-toggle');
    const menu = item.querySelector('.dropdown-menu');

    if (!toggle || !menu) return;

    // Toggle on click for touch or keyboard users
    toggle.addEventListener('click', (e) => {
      e.preventDefault();
      const isOpen = item.classList.contains('open');

      // Close other open dropdowns
      navItems.forEach((other) => {
        if (other !== item) {
          other.classList.remove('open');
          const otherToggle = other.querySelector('.dropdown-toggle');
          if (otherToggle) otherToggle.setAttribute('aria-expanded', 'false');
        }
      });

      item.classList.toggle('open', !isOpen);
      toggle.setAttribute('aria-expanded', String(!isOpen));
    });

    // Close on blur outside
    item.addEventListener('focusout', (e) => {
      if (!item.contains(e.relatedTarget)) {
        item.classList.remove('open');
        toggle.setAttribute('aria-expanded', 'false');
      }
    });
  });

  // Close desktop dropdowns on escape
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      navItems.forEach((item) => {
        item.classList.remove('open');
        const toggle = item.querySelector('.dropdown-toggle');
        if (toggle) toggle.setAttribute('aria-expanded', 'false');
      });

      if (langWrapper) {
        langWrapper.classList.remove('open');
        if (btnLang) btnLang.setAttribute('aria-expanded', 'false');
      }

      closeSearchSuggestions();
      closeModal();
      closeMobileDrawer();
    }
  });

  // Close dropdowns when clicking outside
  document.addEventListener('click', (e) => {
    navItems.forEach((item) => {
      if (!item.contains(e.target)) {
        item.classList.remove('open');
        const toggle = item.querySelector('.dropdown-toggle');
        if (toggle) toggle.setAttribute('aria-expanded', 'false');
      }
    });

    if (langWrapper && !langWrapper.contains(e.target)) {
      langWrapper.classList.remove('open');
      if (btnLang) btnLang.setAttribute('aria-expanded', 'false');
    }
  });

  // ========================================================================
  // 3. SEARCH BAR INTERACTIONS & QUICK SUGGESTIONS
  // ========================================================================
  function openSearchSuggestions() {
    if (searchSuggestions) {
      searchSuggestions.classList.add('active');
    }
  }

  function closeSearchSuggestions() {
    if (searchSuggestions) {
      searchSuggestions.classList.remove('active');
    }
  }

  if (globalSearchInput) {
    globalSearchInput.addEventListener('focus', () => {
      openSearchSuggestions();
    });

    globalSearchInput.addEventListener('input', () => {
      if (globalSearchInput.value.trim().length > 0) {
        if (searchClearBtn) searchClearBtn.style.display = 'flex';
      } else {
        if (searchClearBtn) searchClearBtn.style.display = 'none';
      }
      openSearchSuggestions();
    });

    // Handle enter key in search
    globalSearchInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        const query = globalSearchInput.value.trim();
        if (query) {
          closeSearchSuggestions();
          showToast(`Recherche pour : « ${query} »`, 'info');
        }
      }
    });

    // Close on click outside search
    document.addEventListener('click', (e) => {
      if (searchPill && !searchPill.contains(e.target) && searchSuggestions && !searchSuggestions.contains(e.target)) {
        closeSearchSuggestions();
      }
    });
  }

  if (searchClearBtn && globalSearchInput) {
    searchClearBtn.addEventListener('click', () => {
      globalSearchInput.value = '';
      searchClearBtn.style.display = 'none';
      globalSearchInput.focus();
    });
  }

  // Suggestion item selection
  const suggestionItems = document.querySelectorAll('.suggestion-item');
  suggestionItems.forEach((btn) => {
    btn.addEventListener('click', () => {
      const query = btn.getAttribute('data-search') || btn.textContent.trim();
      if (globalSearchInput) {
        globalSearchInput.value = query;
        if (searchClearBtn) searchClearBtn.style.display = 'flex';
      }
      closeSearchSuggestions();
      showToast(`Sélectionné : ${query}`, 'info');
    });
  });

  // ========================================================================
  // 4. MOBILE NAVIGATION DRAWER & ACCORDIONS
  // ========================================================================
  function openMobileDrawer() {
    if (!mobileDrawer) return;
    mobileDrawer.classList.add('open');
    mobileDrawer.setAttribute('aria-hidden', 'false');
    if (mobileDrawerBackdrop) {
      mobileDrawerBackdrop.classList.add('open');
    }
    if (mobileHamburger) {
      mobileHamburger.classList.add('active');
      mobileHamburger.setAttribute('aria-expanded', 'true');
    }
    document.body.style.overflow = 'hidden';
  }

  function closeMobileDrawer() {
    if (!mobileDrawer) return;
    mobileDrawer.classList.remove('open');
    mobileDrawer.setAttribute('aria-hidden', 'true');
    if (mobileDrawerBackdrop) {
      mobileDrawerBackdrop.classList.remove('open');
    }
    if (mobileHamburger) {
      mobileHamburger.classList.remove('active');
      mobileHamburger.setAttribute('aria-expanded', 'false');
    }
    document.body.style.overflow = '';
  }

  if (mobileHamburger) {
    mobileHamburger.addEventListener('click', () => {
      const isOpen = mobileDrawer && mobileDrawer.classList.contains('open');
      if (isOpen) {
        closeMobileDrawer();
      } else {
        openMobileDrawer();
      }
    });
  }

  if (mobileDrawerBackdrop) {
    mobileDrawerBackdrop.addEventListener('click', closeMobileDrawer);
  }

  // Mobile Accordion items
  mobileNavBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-mobile-toggle');
      const targetMenu = document.getElementById(targetId);
      if (!targetMenu) return;

      const isOpen = targetMenu.classList.contains('open');

      // Close all submenus
      document.querySelectorAll('.mobile-submenu').forEach((sub) => {
        sub.classList.remove('open');
      });
      mobileNavBtns.forEach((b) => b.classList.remove('open'));

      if (!isOpen) {
        targetMenu.classList.add('open');
        btn.classList.add('open');
      }
    });
  });

  // Sublinks inside dropdowns / mobile drawer
  document.querySelectorAll('[data-action]').forEach((item) => {
    item.addEventListener('click', (e) => {
      e.preventDefault();
      const action = item.getAttribute('data-action');
      const title = item.querySelector('.dropdown-item-title')?.textContent || item.textContent.trim();
      closeMobileDrawer();
      showModal(
        'Explorer Evently',
        `Vous avez sélectionné « ${title} » (${action}). Le catalogue filtré complet sera implémenté dans l'Étape suivante du projet.`,
        '🎵'
      );
    });
  });

  // ========================================================================
  // 5. LANGUAGE SELECTOR
  // ========================================================================
  if (btnLang && langWrapper) {
    btnLang.addEventListener('click', (e) => {
      e.stopPropagation();
      const isOpen = langWrapper.classList.contains('open');
      langWrapper.classList.toggle('open', !isOpen);
      btnLang.setAttribute('aria-expanded', String(!isOpen));
    });

    langItems.forEach((btn) => {
      btn.addEventListener('click', () => {
        const lang = btn.getAttribute('data-lang');
        langItems.forEach((b) => {
          b.classList.remove('active');
          const check = b.querySelector('.lang-check');
          if (check) check.style.display = 'none';
        });
        btn.classList.add('active');
        const check = btn.querySelector('.lang-check');
        if (check) check.style.display = 'inline';

        langWrapper.classList.remove('open');
        btnLang.setAttribute('aria-expanded', 'false');

        // Update mobile indicator as well
        mobileLangOpts.forEach((mOpt) => {
          mOpt.classList.toggle('active', mOpt.getAttribute('data-lang') === lang);
        });

        showToast(`Langue sélectionnée : ${lang.toUpperCase()}`, 'info');
      });
    });

    mobileLangOpts.forEach((mOpt) => {
      mOpt.addEventListener('click', () => {
        const lang = mOpt.getAttribute('data-lang');
        mobileLangOpts.forEach((b) => b.classList.remove('active'));
        mOpt.classList.add('active');
        showToast(`Langue sélectionnée : ${lang.toUpperCase()}`, 'info');
      });
    });
  }

  // ========================================================================
  // 6. ACTION BUTTONS & INTERACTIVE PROTOFEEDBACK
  // ========================================================================

  try {
    if (sessionStorage.getItem('evently.loginSucceeded') === '1') {
      sessionStorage.removeItem('evently.loginSucceeded');
      showToast('Vous êtes connecté à Evently.');
    }
  } catch {
    // The homepage remains usable with browser storage disabled.
  }

  const homeUrl = document.querySelector('.header-brand .brand-logo').href;

  function handleLoginClick() {
    closeMobileDrawer();
    window.location.assign(new URL('login', homeUrl));
  }

  if (btnLogin) btnLogin.addEventListener('click', handleLoginClick);
  if (mobileBtnLogin) mobileBtnLogin.addEventListener('click', handleLoginClick);

  function handleSignupClick() {
    closeMobileDrawer();
    window.location.assign(new URL('register', homeUrl));
  }

  if (btnSignup) btnSignup.addEventListener('click', handleSignupClick);
  if (mobileBtnSignup) mobileBtnSignup.addEventListener('click', handleSignupClick);

  // Voir l'événement (Atlas Afterdark)
  if (btnViewEvent) {
    btnViewEvent.addEventListener('click', (e) => {
      e.stopPropagation();
      showModal(
        'Atlas Afterdark — Casablanca',
        "Détails de l'événement :\n• Date : 12 Décembre 2026\n• Lieu : Casablanca (Scène Live)\n• Tarif : 250 DH\n\nLa page de réservation détaillée et le choix des places seront branchés à l'Étape 2.",
        '🎟️'
      );
    });
  }

  // Explorer les événements (Hero Primary CTA)
  if (btnExploreEvents) {
    btnExploreEvents.addEventListener('click', () => {
      showToast('Navigation vers le catalogue des événements...', 'coral');
      // Subtle smooth pulse on the featured card to guide the eye
      const card = document.getElementById('featured-card');
      if (card) {
        card.style.transform = 'scale(1.03) translateY(-6px)';
        card.style.boxShadow = '0 0 50px rgba(244, 133, 107, 0.45)';
        setTimeout(() => {
          card.style.transform = '';
          card.style.boxShadow = '';
        }, 1200);
      }
    });
  }

  // Découvrir la musique (Hero Secondary CTA)
  if (btnDiscoverMusic) {
    btnDiscoverMusic.addEventListener('click', () => {
      showToast('Playlist & Vibe Atlas Afterdark chargée ♫', 'info');
    });
  }

  // Clicking on the featured card itself opens details preview
  const featuredCard = document.getElementById('featured-card');
  if (featuredCard) {
    featuredCard.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        if (btnViewEvent) btnViewEvent.click();
      }
    });
  }

  // ========================================================================
  // 7. MODAL SYSTEM
  // ========================================================================
  function showModal(title, message, icon) {
    if (!modalBackdrop) return;
    if (modalTitle) modalTitle.textContent = title;
    if (modalMessage) modalMessage.textContent = message;
    if (modalIconBadge && icon) modalIconBadge.textContent = icon;

    modalBackdrop.classList.add('active');
    modalBackdrop.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden';

    if (modalConfirmBtn) {
      setTimeout(() => modalConfirmBtn.focus(), 100);
    }
  }

  function closeModal() {
    if (!modalBackdrop) return;
    modalBackdrop.classList.remove('active');
    modalBackdrop.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
  }

  if (modalCloseBtn) modalCloseBtn.addEventListener('click', closeModal);
  if (modalConfirmBtn) modalConfirmBtn.addEventListener('click', closeModal);
  if (modalBackdrop) {
    modalBackdrop.addEventListener('click', (e) => {
      if (e.target === modalBackdrop) closeModal();
    });
  }

  // ========================================================================
  // 8. TOAST SYSTEM
  // ========================================================================
  function showToast(message, type = 'info') {
    if (!toastContainer) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type === 'coral' ? 'coral' : ''}`;
    toast.setAttribute('role', 'status');

    const icon = type === 'coral' ? '⚡' : '✨';
    toast.innerHTML = `
      <span class="toast-icon">${icon}</span>
      <span class="toast-text">${escapeHtml(message)}</span>
    `;

    toastContainer.appendChild(toast);

    // Trigger enter animation
    requestAnimationFrame(() => {
      toast.classList.add('show');
    });

    // Auto dismiss after 3.5s
    setTimeout(() => {
      toast.classList.remove('show');
      setTimeout(() => {
        if (toast.parentNode === toastContainer) {
          toastContainer.removeChild(toast);
        }
      }, 400);
    }, 3500);
  }

  function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // ========================================================================
  // 9. HOMEPAGE SEARCH, CATEGORIES & EVENTS FILTERING
  // ========================================================================
  const homepageSearchForm = document.getElementById('homepage-search-form');
  const filterKeyword = document.getElementById('filter-keyword');
  const filterCity = document.getElementById('filter-city');
  const activeFilterBar = document.getElementById('active-filter-bar');
  const filterStatusLabel = document.getElementById('filter-status-label');
  const btnClearFilter = document.getElementById('btn-clear-filter');
  const eventCards = document.querySelectorAll('.events-grid .event-card');
  const categoryCards = document.querySelectorAll('.category-card');
  const btnSeeAllEvents = document.getElementById('btn-see-all-events');
  const btnPromoExplore = document.getElementById('btn-promo-explore');

  // Filter state
  let currentCategory = '';
  let currentCity = '';
  let currentKeyword = '';

  function applyFilters() {
    let visibleCount = 0;

    eventCards.forEach((card) => {
      const cardCategory = card.getAttribute('data-category') || '';
      const cardCity = card.getAttribute('data-city') || '';
      const cardText = (card.textContent || '').toLowerCase();

      let matchCat = !currentCategory || cardCategory.toLowerCase() === currentCategory.toLowerCase();
      let matchCity = !currentCity || cardCity.toLowerCase() === currentCity.toLowerCase();
      let matchKeyword = !currentKeyword || cardText.includes(currentKeyword.toLowerCase());

      if (matchCat && matchCity && matchKeyword) {
        card.style.display = 'flex';
        visibleCount++;
      } else {
        card.style.display = 'none';
      }
    });

    // Update active filter bar
    if (activeFilterBar && filterStatusLabel) {
      const activeLabels = [];
      if (currentCategory) activeLabels.push(`Catégorie : ${currentCategory}`);
      if (currentCity) activeLabels.push(`Ville : ${currentCity}`);
      if (currentKeyword) activeLabels.push(`« ${currentKeyword} »`);

      if (activeLabels.length > 0) {
        filterStatusLabel.textContent = `${activeLabels.join(' + ')} (${visibleCount} résultat${visibleCount > 1 ? 's' : ''})`;
        activeFilterBar.style.display = 'flex';
      } else {
        activeFilterBar.style.display = 'none';
      }
    }
  }

  function resetFilters() {
    currentCategory = '';
    currentCity = '';
    currentKeyword = '';
    if (filterKeyword) filterKeyword.value = '';
    if (filterCity) filterCity.value = '';
    applyFilters();
    showToast('Tous les événements sont affichés', 'info');
  }

  // Search form submit
  if (homepageSearchForm) {
    homepageSearchForm.addEventListener('submit', (e) => {
      e.preventDefault();
      currentKeyword = filterKeyword ? filterKeyword.value.trim() : '';
      currentCity = filterCity ? filterCity.value : '';
      applyFilters();

      const eventsSection = document.getElementById('featured-events');
      if (eventsSection) {
        eventsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }

      showToast(`Recherche : ${currentKeyword || 'Tous'} ${currentCity ? 'à ' + currentCity : ''}`, 'info');
    });
  }

  if (filterCity) {
    filterCity.addEventListener('change', () => {
      currentCity = filterCity.value;
      applyFilters();
    });
  }

  if (btnClearFilter) {
    btnClearFilter.addEventListener('click', resetFilters);
  }

  if (btnSeeAllEvents) {
    btnSeeAllEvents.addEventListener('click', resetFilters);
  }

  // Category card clicks
  categoryCards.forEach((card) => {
    function handleCategorySelect() {
      const cat = card.getAttribute('data-category');
      if (!cat) return;

      currentCategory = cat;
      applyFilters();

      const eventsSection = document.getElementById('featured-events');
      if (eventsSection) {
        eventsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }

      showToast(`Catégorie sélectionnée : ${cat}`, 'coral');
    }

    card.addEventListener('click', handleCategorySelect);
    card.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleCategorySelect();
      }
    });
  });

  // Event Details button clicks
  document.querySelectorAll('.btn-event-details').forEach((btn) => {
    btn.addEventListener('click', (e) => {
      e.stopPropagation();
      const title = btn.getAttribute('data-event-title') || 'Événement';
      const price = btn.getAttribute('data-event-price') || 'Sur réservation';
      const city = btn.getAttribute('data-event-city') || 'Maroc';

      showModal(
        title,
        `Détails de l'événement :\n• Ville : ${city}\n• Tarif : ${price}\n\nLa page de réservation et de sélection des places sera intégrée dans l'Étape suivante du projet Evently.`,
        '🎟️'
      );
    });
  });

  // Promo Banner explore button
  if (btnPromoExplore) {
    btnPromoExplore.addEventListener('click', () => {
      const eventsSection = document.getElementById('featured-events');
      if (eventsSection) {
        eventsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
      showToast('Découvrez notre sélection d\'expériences exclusives', 'coral');
    });
  }

  // Footer login & signup buttons
  const footerBtnLogin = document.getElementById('footer-btn-login');
  const footerBtnSignup = document.getElementById('footer-btn-signup');
  if (footerBtnLogin) footerBtnLogin.addEventListener('click', handleLoginClick);
  if (footerBtnSignup) footerBtnSignup.addEventListener('click', handleSignupClick);

  // Footer category links
  document.querySelectorAll('.footer-link[data-filter]').forEach((link) => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const cat = link.getAttribute('data-filter');
      currentCategory = cat;
      applyFilters();
      const eventsSection = document.getElementById('featured-events');
      if (eventsSection) {
        eventsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
      showToast(`Filtre : ${cat}`, 'info');
    });
  });

})();
