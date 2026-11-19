(() => {
    'use strict';

    const form = document.querySelector('.auth-form');
    if (!form) return;

    const registering = form.dataset.mode === 'register';
    const name = document.getElementById('auth-name');
    const email = document.getElementById('auth-email');
    const password = document.getElementById('auth-password');
    const confirmation = document.getElementById('auth-confirm');
    const terms = document.getElementById('auth-terms');
    const remember = document.getElementById('auth-remember');
    const feedback = document.getElementById('auth-feedback');
    const submit = form.querySelector('.auth-submit');
    const submitText = submit.querySelector('.auth-submit-text');
    const readyText = submitText.textContent;
    const emailStorageKey = 'evently.rememberedEmail';
    const fields = [name, email, password, confirmation, terms].filter(Boolean);
    const encoder = new TextEncoder();

    function showFieldError(field, message = '') {
        const error = document.getElementById(`${field.id}-error`);
        error.textContent = message;
        error.hidden = !message;
        if (message) field.setAttribute('aria-invalid', 'true');
        else field.removeAttribute('aria-invalid');
    }

    function showFeedback(message = '') {
        feedback.hidden = !message;
        feedback.textContent = message;
    }

    function validationError(field) {
        switch (field) {
            case name:
                return name.value.trim() ? '' : 'Renseignez votre nom complet.';
            case email:
                if (!email.value.trim()) return 'Renseignez votre adresse email.';
                return email.validity.typeMismatch ? 'Saisissez une adresse email valide.' : '';
            case password:
                if (!password.value) return 'Renseignez votre mot de passe.';
                if (registering && (!password.value.trim() || Array.from(password.value).length < 12)) {
                    return 'Utilisez au moins 12 caractères.';
                }
                if (encoder.encode(password.value).length > 72) return 'Le mot de passe dépasse 72 octets.';
                return '';
            case confirmation:
                if (!confirmation.value) return 'Confirmez votre mot de passe.';
                return confirmation.value !== password.value ? 'Les mots de passe ne correspondent pas.' : '';
            case terms:
                return terms.checked ? '' : 'Acceptez les conditions d’utilisation pour continuer.';
            default:
                return '';
        }
    }

    function validate() {
        const firstInvalid = fields.filter(field => {
            const error = validationError(field);
            showFieldError(field, error);
            return Boolean(error);
        })[0];
        if (firstInvalid) firstInvalid.focus();
        return !firstInvalid;
    }

    function showServerError(code) {
        const fieldErrors = {
            invalid_name: [name, 'Saisissez un nom valide.'],
            invalid_email: [email, 'Saisissez une adresse email valide.'],
            invalid_password: [password, 'Ce mot de passe ne respecte pas les règles de sécurité.'],
            email_taken: [email, 'Cette adresse email est déjà utilisée.'],
            password_mismatch: [confirmation, 'Les mots de passe ne correspondent pas.'],
            terms_required: [terms, 'Acceptez les conditions d’utilisation pour continuer.']
        };
        const fieldError = fieldErrors[code];
        if (Array.isArray(fieldError) && fieldError[0]) {
            showFieldError(...fieldError);
            fieldError[0].focus();
            return;
        }
        const messages = {
            invalid_credentials: 'L’adresse email ou le mot de passe est incorrect.',
            invalid_csrf: 'Votre session a expiré. Rechargez la page et réessayez.',
            authentication_required: 'Votre session a expiré. Rechargez la page et réessayez.'
        };
        showFeedback(Object.hasOwn(messages, code) ? messages[code] : 'L’envoi a échoué. Réessayez dans un instant.');
    }

    function setLoading(loading) {
        submit.disabled = loading;
        submitText.textContent = loading ? submit.dataset.loading : readyText;
        form.setAttribute('aria-busy', String(loading));
    }

    document.querySelectorAll('.auth-reveal').forEach(button => {
        const field = document.getElementById(button.getAttribute('aria-controls'));
        button.hidden = false;
        button.addEventListener('click', () => {
            const visible = field.type === 'password';
            field.type = visible ? 'text' : 'password';
            button.setAttribute('aria-pressed', String(visible));
            button.lastElementChild.textContent = visible ? 'Masquer' : 'Afficher';
        });
    });

    if (registering) {
        const meter = document.getElementById('auth-strength');
        const strengthLabel = document.getElementById('auth-strength-label');
        const updateStrength = () => {
            const value = password.value;
            const length = Array.from(value).length;
            const diversity = [/\p{Ll}/u, /\p{Lu}/u, /\p{N}/u, /[^\p{L}\p{N}\s]/u].filter(pattern => pattern.test(value)).length;
            let strength = 0;
            if (value.trim()) {
                strength = 1;
                if (length >= 12) strength = 2;
                if (length >= 12 && (length >= 18 || diversity >= 3)) strength = 3;
            }
            if (encoder.encode(value).length > 72) strength = 0;
            meter.parentElement.hidden = !value;
            meter.value = strength;
            strengthLabel.textContent = ['Insuffisant', 'Faible', 'Acceptable', 'Fort'][strength];
        };
        password.addEventListener('input', updateStrength);
        updateStrength();
    }

    if (remember) {
        try {
            const previousEmail = localStorage.getItem(emailStorageKey);
            if (previousEmail) {
                if (!email.value) email.value = previousEmail;
                remember.checked = true;
            }
            document.getElementById('auth-remember-option').hidden = false;
            document.getElementById('auth-remember-hint').hidden = false;
            remember.addEventListener('change', () => {
                if (!remember.checked) localStorage.removeItem(emailStorageKey);
            });
        } catch {
            // The form still works if this browser blocks local storage.
        }
    }

    fields.forEach(field => {
        field.addEventListener('input', () => {
            if (field.hasAttribute('aria-invalid')) showFieldError(field, validationError(field));
            if (field === password && confirmation?.hasAttribute('aria-invalid')) {
                showFieldError(confirmation, validationError(confirmation));
            }
        });
    });

    form.noValidate = true;
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (submit.disabled) return;
        showFeedback();
        if (!validate()) return;

        const body = new URLSearchParams(new FormData(form));
        setLoading(true);
        let redirecting = false;
        try {
            const response = await fetch(form.dataset.endpoint, {
                method: 'POST',
                headers: { Accept: 'application/json' },
                body,
                credentials: 'same-origin'
            });
            const data = await response.json().catch(() => null);
            if (response.status !== (registering ? 201 : 200) || !data?.user || !data.redirectUrl) {
                showServerError(data?.error);
                return;
            }
            if (remember?.checked) {
                try {
                    localStorage.setItem(emailStorageKey, data.user.email || body.get('email'));
                } catch {
                    // Saving an address is optional and must not block login.
                }
            }
            window.location.assign(data.redirectUrl);
            redirecting = true;
        } catch {
            showFeedback('Le serveur est inaccessible. Vérifiez votre connexion et réessayez.');
        } finally {
            if (!redirecting) setLoading(false);
        }
    });

    window.addEventListener('pageshow', event => {
        if (event.persisted) setLoading(false);
    });
})();
