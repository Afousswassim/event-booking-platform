<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="Evently — Les meilleurs concerts, festivals et expériences près de chez vous. Des moments qu'on n'oublie pas, à portée de clic.">
    <meta name="theme-color" content="#0B1029">
    <title>Evently — Moins de scroll. Plus de vibes.</title>

    <!-- Google Fonts: Plus Jakarta Sans -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">

    <!-- Stylesheets -->
    <link rel="stylesheet" href="${ctx}/css/variables.css">
    <link rel="stylesheet" href="${ctx}/css/style.css">
    <link rel="stylesheet" href="${ctx}/css/responsive.css">
</head>
<body class="page-home">

    <!-- Header Fragment (Navbar, Search, Auth actions) -->
    <%@ include file="/WEB-INF/views/fragments/header.jspf" %>

    <!-- Main Content -->
    <main id="main-content">
        
        <!-- Hero Section -->
        <section class="hero-section" aria-label="Présentation Evently">
            
            <!-- Immersive Concert Background & Dark Overlays -->
            <div class="hero-bg-container" aria-hidden="true">
                <img 
                    src="${ctx}/images/hero-bg.jpg" 
                    alt="Concert immersif avec foule en liesse et jeux de lumières" 
                    class="hero-bg-image" 
                />
                <div class="hero-overlay hero-overlay-radial"></div>
                <div class="hero-overlay hero-overlay-linear"></div>
                <div class="hero-overlay hero-overlay-bottom"></div>
            </div>

            <!-- Hero Layout Container -->
            <div class="hero-container">
                
                <!-- Left Column: Copy & CTAs -->
                <div class="hero-content">
                    
                    <!-- Eyebrow Tag -->
                    <div class="hero-eyebrow">
                        <span class="eyebrow-dash" aria-hidden="true"></span>
                        <span class="eyebrow-text">VOTRE PROCHAINE HISTOIRE COMMENCE ICI</span>
                    </div>

                    <!-- Main Catchphrase -->
                    <h1 class="hero-title">
                        <span class="title-block">Moins de scroll.</span>
                        <span class="title-block">Plus de <span class="highlight-vibes">vibes.<svg class="vibes-underline" viewBox="0 0 160 20" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M4 14C38 4 102 6 156 12" stroke="#F4856B" stroke-width="4.5" stroke-linecap="round"/></svg></span></span>
                    </h1>

                    <!-- Subtitle / Value Proposition -->
                    <p class="hero-description">
                        Les meilleurs concerts, festivals et expériences près de chez vous. Des moments qu'on n'oublie pas, à portée de clic.
                    </p>

                    <!-- CTA Action Buttons -->
                    <div class="hero-cta-group">
                        <button type="button" class="btn btn-hero-primary" id="btn-explore-events">
                            <span>Explorer les événements</span>
                            <svg class="cta-arrow-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                <line x1="7" y1="17" x2="17" y2="7"></line>
                                <polyline points="7 7 17 7 17 17"></polyline>
                            </svg>
                        </button>
                        
                        <button type="button" class="btn btn-hero-secondary" id="btn-discover-music">
                            <span>Découvrir la musique</span>
                            <span class="music-icon" aria-hidden="true">♫</span>
                        </button>
                    </div>

                </div>

                <!-- Right Column: Featured Event Card -->
                <div class="hero-featured">
                    
                    <div class="featured-card" id="featured-card" tabindex="0" role="region" aria-label="Événement à la une : Atlas Afterdark">
                        
                        <!-- Floating Golden Date Badge (Top-Left, Tilted) -->
                        <div class="badge-date" aria-label="Date : 12 Décembre 2026">
                            <span class="badge-date-number">12</span>
                            <span class="badge-date-month">DEC 2026</span>
                        </div>

                        <!-- Floating Circular "GOOD VIBES ONLY" Badge (Right Edge) -->
                        <div class="badge-good-vibes" aria-hidden="true">
                            <span class="vibes-line">GOOD</span>
                            <span class="vibes-line">VIBES</span>
                            <span class="vibes-line vibes-icon">ONLY <span class="sparkle-star">✳</span></span>
                        </div>

                        <!-- Inner Arched Card Wrapper with Concert Photography & Gradient -->
                        <div class="featured-arch-wrapper">
                            <img 
                                src="${ctx}/images/atlas-afterdark.jpg" 
                                alt="Concert live Atlas Afterdark filmé par un spectateur" 
                                class="featured-img" 
                            />
                            <div class="featured-arch-tint" aria-hidden="true"></div>
                            <div class="featured-arch-gradient" aria-hidden="true"></div>

                            <!-- Card Bottom Information -->
                            <div class="featured-info">
                                <span class="featured-badge-tag">FEATURED EVENT</span>
                                <h2 class="featured-title">Atlas Afterdark</h2>
                                <p class="featured-meta">
                                    <span class="featured-city">Casablanca</span>
                                    <span class="featured-sep" aria-hidden="true">·</span>
                                    <span class="featured-category">Live music</span>
                                </p>
                            </div>
                        </div>

                        <!-- Overlapping Price Float Card (Bottom-Right) -->
                        <div class="price-float-card">
                            <span class="price-card-label">LE PROCHAIN GRAND MOMENT</span>
                            <div class="price-card-amount">250 DH</div>
                            <button type="button" class="btn btn-view-event" id="btn-view-event" aria-label="Voir les détails de l'événement Atlas Afterdark">
                                <span>Voir l'événement</span>
                                <svg class="btn-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <line x1="7" y1="17" x2="17" y2="7"></line>
                                    <polyline points="7 7 17 7 17 17"></polyline>
                                </svg>
                            </button>
                        </div>

                    </div>

                </div>

            </div>

        </section>

        <!-- Visual Transition Bridge: dark navy → ivory -->
        <div class="hero-search-bridge" aria-hidden="true"></div>

        <!-- Section Search & Filters (Floating between Hero and Content) -->
        <section class="search-filter-section" id="search-section" aria-label="Recherche et filtres d'événements">
            <div class="search-filter-container">
                <form class="search-filter-bar" id="homepage-search-form" onsubmit="event.preventDefault();">
                    
                    <!-- Search Input -->
                    <div class="search-field-group">
                        <svg class="field-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <circle cx="11" cy="11" r="8"></circle>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                        <div class="field-input-wrap">
                            <label for="filter-keyword" class="field-label">Événement ou Artiste</label>
                            <input 
                                type="text" 
                                id="filter-keyword" 
                                class="filter-input" 
                                placeholder="Un événement, un artiste, une ville..." 
                                autocomplete="off"
                            />
                        </div>
                    </div>

                    <!-- Divider -->
                    <div class="search-divider" aria-hidden="true"></div>

                    <!-- City Select -->
                    <div class="search-field-group city-select-group">
                        <svg class="field-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                            <circle cx="12" cy="10" r="3"></circle>
                        </svg>
                        <div class="field-input-wrap">
                            <label for="filter-city" class="field-label">Ville</label>
                            <select id="filter-city" class="filter-select" aria-label="Sélectionner une ville">
                                <option value="">Toutes les villes</option>
                                <option value="Casablanca">Casablanca</option>
                                <option value="Rabat">Rabat</option>
                                <option value="Marrakech">Marrakech</option>
                                <option value="Tanger">Tanger</option>
                                <option value="Agadir">Agadir</option>
                                <option value="Fès">Fès</option>
                            </select>
                        </div>
                    </div>

                    <!-- Submit Button -->
                    <button type="submit" class="btn btn-search-submit" id="btn-homepage-search" aria-label="Lancer la recherche">
                        <span>Rechercher</span>
                        <svg class="btn-arrow-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                            <line x1="7" y1="17" x2="17" y2="7"></line>
                            <polyline points="7 7 17 7 17 17"></polyline>
                        </svg>
                    </button>

                </form>
            </div>
        </section>

        <!-- Light Theme Content Wrapper -->
        <div class="light-content-wrapper">
            <!-- Subtle top transition gradient -->
            <div class="section-transition-top" aria-hidden="true"></div>

            <!-- Section Event Categories -->
            <section class="categories-section" id="categories" aria-label="Explorer par catégorie">
                <div class="section-container">
                    
                    <!-- Section Header -->
                    <div class="section-header categories-header">
                        <div class="header-left">
                            <span class="section-eyebrow">EXPLORE YOUR VIBE</span>
                            <h2 class="section-title">Qu'est-ce qui vous inspire ?</h2>
                        </div>
                        <div class="header-right">
                            <p class="section-subtitle-badge">
                                <span>Trouvez votre univers</span>
                                <span class="sparkle-coral" aria-hidden="true">✳</span>
                            </p>
                        </div>
                    </div>

                    <!-- 3-column / 5-cards grid -->
                    <div class="categories-grid" role="list">
                        
                        <!-- 1. Musique (Lavande pastel) -->
                        <div class="category-card card-musique" role="listitem" tabindex="0" data-category="Musique" aria-label="Catégorie Musique, 48 événements disponibles">
                            <div class="card-top-row">
                                <div class="category-icon-wrap" aria-hidden="true">
                                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M9 18V5l12-2v13"></path>
                                        <circle cx="6" cy="18" r="3"></circle>
                                        <circle cx="18" cy="16" r="3"></circle>
                                    </svg>
                                </div>
                                <span class="card-arrow" aria-hidden="true">↗</span>
                            </div>
                            <div class="card-bottom-row">
                                <h3 class="category-name">Musique</h3>
                                <span class="category-count">48 événements disponibles</span>
                            </div>
                        </div>

                        <!-- 2. Technologie (Bleu pastel) -->
                        <div class="category-card card-technologie" role="listitem" tabindex="0" data-category="Technologie" aria-label="Catégorie Technologie, 24 événements disponibles">
                            <div class="card-top-row">
                                <div class="category-icon-wrap" aria-hidden="true">
                                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <rect x="2" y="3" width="20" height="14" rx="2" ry="2"></rect>
                                        <line x1="8" y1="21" x2="16" y2="21"></line>
                                        <line x1="12" y1="17" x2="12" y2="21"></line>
                                    </svg>
                                </div>
                                <span class="card-arrow" aria-hidden="true">↗</span>
                            </div>
                            <div class="card-bottom-row">
                                <h3 class="category-name">Technologie</h3>
                                <span class="category-count">24 événements disponibles</span>
                            </div>
                        </div>

                        <!-- 3. Art & culture (Pêche pastel) -->
                        <div class="category-card card-art" role="listitem" tabindex="0" data-category="Art & culture" aria-label="Catégorie Art et culture, 32 événements disponibles">
                            <div class="card-top-row">
                                <div class="category-icon-wrap" aria-hidden="true">
                                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <circle cx="13.5" cy="6.5" r=".5" fill="currentColor"></circle>
                                        <circle cx="17.5" cy="10.5" r=".5" fill="currentColor"></circle>
                                        <circle cx="8.5" cy="7.5" r=".5" fill="currentColor"></circle>
                                        <circle cx="6.5" cy="12.5" r=".5" fill="currentColor"></circle>
                                        <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.926 0 1.648-.746 1.648-1.688 0-.437-.18-.835-.437-1.125-.29-.289-.438-.652-.438-1.125a1.64 1.64 0 0 1 1.668-1.668h1.996c3.051 0 5.563-2.512 5.563-5.563C22 6.5 17.5 2 12 2z"></path>
                                    </svg>
                                </div>
                                <span class="card-arrow" aria-hidden="true">↗</span>
                            </div>
                            <div class="card-bottom-row">
                                <h3 class="category-name">Art & culture</h3>
                                <span class="category-count">32 événements disponibles</span>
                            </div>
                        </div>

                        <!-- 4. Sport (Jaune crème) -->
                        <div class="category-card card-sport" role="listitem" tabindex="0" data-category="Sport" aria-label="Catégorie Sport, 19 événements disponibles">
                            <div class="card-top-row">
                                <div class="category-icon-wrap" aria-hidden="true">
                                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"></path>
                                        <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"></path>
                                        <path d="M4 22h16"></path>
                                        <path d="M10 14.66V17c0 .55-.45 1-1 1H8c-.55 0-1 .45-1 1v1c0 .55.45 1 1 1h8c.55 0 1-.45 1-1v-1c0-.55-.45-1-1-1h-1c-.55 0-1-.45-1-1v-2.34"></path>
                                        <path d="M6 4h12v5a6 6 0 0 1-12 0V4z"></path>
                                    </svg>
                                </div>
                                <span class="card-arrow" aria-hidden="true">↗</span>
                            </div>
                            <div class="card-bottom-row">
                                <h3 class="category-name">Sport</h3>
                                <span class="category-count">19 événements disponibles</span>
                            </div>
                        </div>

                        <!-- 5. Gastronomie (Violet très clair) -->
                        <div class="category-card card-gastronomie" role="listitem" tabindex="0" data-category="Gastronomie" aria-label="Catégorie Gastronomie, 15 événements disponibles">
                            <div class="card-top-row">
                                <div class="category-icon-wrap" aria-hidden="true">
                                    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M18 8h1a4 4 0 0 1 0 8h-1"></path>
                                        <path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"></path>
                                        <line x1="6" y1="1" x2="6" y2="4"></line>
                                        <line x1="10" y1="1" x2="10" y2="4"></line>
                                        <line x1="14" y1="1" x2="14" y2="4"></line>
                                    </svg>
                                </div>
                                <span class="card-arrow" aria-hidden="true">↗</span>
                            </div>
                            <div class="card-bottom-row">
                                <h3 class="category-name">Gastronomie</h3>
                                <span class="category-count">15 événements disponibles</span>
                            </div>
                        </div>

                    </div>
                </div>
            </section>

            <!-- Section Featured Events -->
            <section class="featured-events-section" id="featured-events" aria-label="Événements à ne pas manquer">
                <div class="section-container">
                    
                    <!-- Section Header -->
                    <div class="section-header events-header">
                        <div class="header-left">
                            <span class="section-eyebrow">CURATED FOR YOU</span>
                            <h2 class="section-title">Les événements à ne pas manquer <span class="sparkle-violet">✳</span></h2>
                        </div>
                        <div class="header-right">
                            <button type="button" class="btn btn-outline-dark" id="btn-see-all-events">
                                <span>Voir tout</span>
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="7" y1="17" x2="17" y2="7"></line><polyline points="7 7 17 7 17 17"></polyline></svg>
                            </button>
                        </div>
                    </div>

                    <!-- Filter state feedback indicator (visible when filter active) -->
                    <div class="active-filter-bar" id="active-filter-bar" style="display: none;">
                        <span class="filter-status-text">Filtre actif : <strong id="filter-status-label">Musique</strong></span>
                        <button type="button" class="btn-clear-filter" id="btn-clear-filter" aria-label="Réinitialiser les filtres">
                            <span>Effacer le filtre</span> ✕
                        </button>
                    </div>

                    <!-- 3-column Events Grid -->
                    <div class="events-grid" id="events-grid">
                        
                        <!-- Event 1: Musique -->
                        <article class="event-card" data-category="Musique" data-city="Casablanca" tabindex="0">
                            <div class="event-image-wrap">
                                <img 
                                    src="${ctx}/images/event-music.jpg" 
                                    alt="Concert Jazzablanca Sunset Live à l'Hippodrome d'Anfa Casablanca" 
                                    class="event-img"
                                    loading="lazy"
                                />
                                <span class="event-badge-cat cat-musique">MUSIQUE</span>
                                <div class="event-badge-date">
                                    <span class="date-day">24</span>
                                    <span class="date-month">JUIN 2026</span>
                                </div>
                            </div>
                            <div class="event-card-body">
                                <h3 class="event-card-title">Jazzablanca Sunset Live</h3>
                                <div class="event-meta-location">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                                    <span>Casablanca · Hippodrome d'Anfa</span>
                                </div>
                                <div class="event-meta-capacity">
                                    <span class="status-indicator"></span>
                                    <span>340 places restantes</span>
                                </div>
                                <div class="event-card-divider"></div>
                                <div class="event-card-footer">
                                    <div class="event-price-box">
                                        <span class="price-prefix">À partir de</span>
                                        <span class="price-value">350 DH</span>
                                    </div>
                                    <button type="button" class="btn btn-event-details" data-event-title="Jazzablanca Sunset Live" data-event-price="350 DH" data-event-city="Casablanca" aria-label="Voir les détails de Jazzablanca Sunset Live">
                                        <span>Voir détails</span>
                                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="7" y1="17" x2="17" y2="7"></line><polyline points="7 7 17 7 17 17"></polyline></svg>
                                    </button>
                                </div>
                            </div>
                        </article>

                        <!-- Event 2: Technologie -->
                        <article class="event-card" data-category="Technologie" data-city="Rabat" tabindex="0">
                            <div class="event-image-wrap">
                                <img 
                                    src="${ctx}/images/event-tech.jpg" 
                                    alt="Morocco AI & Web3 Summit au Palais des Congrès Rabat" 
                                    class="event-img"
                                    loading="lazy"
                                />
                                <span class="event-badge-cat cat-tech">TECHNOLOGIE</span>
                                <div class="event-badge-date">
                                    <span class="date-day">15</span>
                                    <span class="date-month">JUIL 2026</span>
                                </div>
                            </div>
                            <div class="event-card-body">
                                <h3 class="event-card-title">Morocco AI & Web3 Summit</h3>
                                <div class="event-meta-location">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                                    <span>Rabat · Palais des Congrès</span>
                                </div>
                                <div class="event-meta-capacity">
                                    <span class="status-indicator"></span>
                                    <span>120 places restantes</span>
                                </div>
                                <div class="event-card-divider"></div>
                                <div class="event-card-footer">
                                    <div class="event-price-box">
                                        <span class="price-prefix">Accès Pass</span>
                                        <span class="price-value">180 DH</span>
                                    </div>
                                    <button type="button" class="btn btn-event-details" data-event-title="Morocco AI & Web3 Summit" data-event-price="180 DH" data-event-city="Rabat" aria-label="Voir les détails de Morocco AI & Web3 Summit">
                                        <span>Voir détails</span>
                                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="7" y1="17" x2="17" y2="7"></line><polyline points="7 7 17 7 17 17"></polyline></svg>
                                    </button>
                                </div>
                            </div>
                        </article>

                        <!-- Event 3: Art & Culture -->
                        <article class="event-card" data-category="Art & culture" data-city="Marrakech" tabindex="0">
                            <div class="event-image-wrap">
                                <img 
                                    src="${ctx}/images/event-art.jpg" 
                                    alt="Festival Nuits Lumineuses de la Kasbah au Palais El Badi Marrakech" 
                                    class="event-img"
                                    loading="lazy"
                                />
                                <span class="event-badge-cat cat-art">ART & CULTURE</span>
                                <div class="event-badge-date">
                                    <span class="date-day">02</span>
                                    <span class="date-month">AOÛT 2026</span>
                                </div>
                            </div>
                            <div class="event-card-body">
                                <h3 class="event-card-title">Nuits Lumineuses de la Kasbah</h3>
                                <div class="event-meta-location">
                                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path><circle cx="12" cy="10" r="3"></circle></svg>
                                    <span>Marrakech · Palais El Badi</span>
                                </div>
                                <div class="event-meta-capacity">
                                    <span class="status-indicator warning"></span>
                                    <span>85 places restantes</span>
                                </div>
                                <div class="event-card-divider"></div>
                                <div class="event-card-footer">
                                    <div class="event-price-box">
                                        <span class="price-prefix">À partir de</span>
                                        <span class="price-value">200 DH</span>
                                    </div>
                                    <button type="button" class="btn btn-event-details" data-event-title="Nuits Lumineuses de la Kasbah" data-event-price="200 DH" data-event-city="Marrakech" aria-label="Voir les détails des Nuits Lumineuses de la Kasbah">
                                        <span>Voir détails</span>
                                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="7" y1="17" x2="17" y2="7"></line><polyline points="7 7 17 7 17 17"></polyline></svg>
                                    </button>
                                </div>
                            </div>
                        </article>

                    </div>
                </div>
            </section>

            <!-- Section Promotional Banner -->
            <section class="promo-banner-section" aria-label="Découvrir la vie culturelle">
                <div class="section-container">
                    <div class="promo-banner-card">
                        
                        <!-- Background glow shapes -->
                        <div class="promo-glow glow-violet" aria-hidden="true"></div>
                        <div class="promo-glow glow-coral" aria-hidden="true"></div>
                        
                        <div class="promo-content">
                            <span class="promo-eyebrow">THE CITY IS YOURS</span>
                            <h2 class="promo-title">Les meilleurs souvenirs commencent en sortant.</h2>
                            <p class="promo-desc">
                                Votre prochain événement vous attend. Découvrez des expériences uniques, réservez vos billets et vivez des moments inoubliables.
                            </p>
                            <button type="button" class="btn btn-promo-cta" id="btn-promo-explore">
                                <span>Explorer les événements</span>
                                <svg class="btn-arrow" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                                    <line x1="7" y1="17" x2="17" y2="7"></line>
                                    <polyline points="7 7 17 7 17 17"></polyline>
                                </svg>
                            </button>
                        </div>

                        <!-- Right Graphic Cards & Social Proof -->
                        <div class="promo-visual-side">
                            <div class="promo-badge-item badge-pill">
                                <span class="badge-icon">✨</span>
                                <div class="badge-text">
                                    <strong>+50 000 Billets</strong>
                                    <span>Réservés en toute confiance</span>
                                </div>
                            </div>
                            <div class="promo-badge-item badge-rating">
                                <div class="stars-row">★★★★★</div>
                                <div class="badge-text">
                                    <strong>4.9 / 5 Note moyenne</strong>
                                    <span>Sur +12 000 spectateurs ravis</span>
                                </div>
                            </div>
                            <div class="promo-badge-item badge-secure">
                                <span class="badge-icon">🔒</span>
                                <div class="badge-text">
                                    <strong>QR Code 100% Garanti</strong>
                                    <span>Accès instantané coupe-file</span>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </section>

        </div>

    </main>

    <!-- Footer Fragment -->
    <%@ include file="/WEB-INF/views/fragments/footer.jspf" %>

    <!-- Modal for Step 1 Interactive Feedback -->
    <div class="evently-modal-backdrop" id="evently-modal" aria-hidden="true" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div class="evently-modal-box">
            <button type="button" class="modal-close-btn" id="modal-close-btn" aria-label="Fermer la boîte de dialogue">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
            </button>
            <div class="modal-icon-badge" id="modal-icon-badge">✨</div>
            <h3 class="modal-title" id="modal-title">Étape 1 — Prototype Visiteur</h3>
            <p class="modal-message" id="modal-message">Cette fonctionnalité sera disponible dans les étapes suivantes du projet Evently.</p>
            <div class="modal-actions">
                <button type="button" class="btn btn-primary-nav" id="modal-confirm-btn">Compris !</button>
            </div>
        </div>
    </div>

    <!-- Notification Toast Container -->
    <div class="toast-container" id="toast-container" aria-live="polite" aria-atomic="true"></div>

    <!-- Scripts -->
    <script src="${ctx}/js/main.js" defer></script>
</body>
</html>
