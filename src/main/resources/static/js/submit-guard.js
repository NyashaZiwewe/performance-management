(function () {
    'use strict';

    if (window.__pmSubmitGuardInstalled) {
        return;
    }
    window.__pmSubmitGuardInstalled = true;
    var SUBMIT_CONTROL_SELECTOR = 'button:not([type]), button[type="submit"], input[type="submit"]';

    function submitControls(form) {
        return form.querySelectorAll(SUBMIT_CONTROL_SELECTOR);
    }

    function findSubmitter(event, form) {
        if (event.submitter) {
            return event.submitter;
        }

        var activeElement = document.activeElement;
        if (activeElement && activeElement.form === form &&
            activeElement.matches(SUBMIT_CONTROL_SELECTOR)) {
            return activeElement;
        }

        return form.querySelector('button:not([type]):not([disabled]), button[type="submit"]:not([disabled]), input[type="submit"]:not([disabled])');
    }

    function loadingText(submitter) {
        var customText = submitter.getAttribute('data-loading-text');
        if (customText) {
            return customText;
        }

        var label = (submitter.textContent || submitter.value || '').trim().toLowerCase();
        if (label.indexOf('log out') !== -1) {
            return 'Logging out...';
        }
        if (/save|update|add|create|include|clone/.test(label)) {
            return 'Saving...';
        }
        if (/approve|reject|submit|send/.test(label)) {
            return 'Submitting...';
        }
        if (/apply|filter|search|view|next|continue/.test(label)) {
            return 'Loading...';
        }
        return 'Processing...';
    }

    function setLoadingState(submitter) {
        if (!submitter || submitter.dataset.pmLoading === 'true') {
            return;
        }

        submitter.dataset.pmLoading = 'true';
        submitter.dataset.pmOriginalAriaDisabled = submitter.getAttribute('aria-disabled') || '';
        submitter.dataset.pmOriginalMinWidth = submitter.style.minWidth || '';
        submitter.style.minWidth = Math.ceil(submitter.getBoundingClientRect().width) + 'px';
        submitter.classList.add('pm-submit-loading');
        submitter.setAttribute('aria-disabled', 'true');

        var text = loadingText(submitter);
        if (submitter.tagName === 'BUTTON') {
            submitter.dataset.pmOriginalHtml = submitter.innerHTML;
            submitter.innerHTML = '<i class="fa fa-spinner fa-spin" aria-hidden="true"></i> ' + text;
        } else {
            submitter.dataset.pmOriginalValue = submitter.value;
            submitter.value = text;
        }
    }

    function resetControl(control) {
        if (control.dataset.pmLoading !== 'true') {
            return;
        }

        if (control.tagName === 'BUTTON' && control.dataset.pmOriginalHtml !== undefined) {
            control.innerHTML = control.dataset.pmOriginalHtml;
        } else if (control.dataset.pmOriginalValue !== undefined) {
            control.value = control.dataset.pmOriginalValue;
        }

        if (control.dataset.pmOriginalAriaDisabled) {
            control.setAttribute('aria-disabled', control.dataset.pmOriginalAriaDisabled);
        } else {
            control.removeAttribute('aria-disabled');
        }

        control.style.minWidth = control.dataset.pmOriginalMinWidth || '';
        control.classList.remove('pm-submit-loading');
        delete control.dataset.pmLoading;
        delete control.dataset.pmOriginalHtml;
        delete control.dataset.pmOriginalValue;
        delete control.dataset.pmOriginalAriaDisabled;
        delete control.dataset.pmOriginalMinWidth;
    }

    function resetForm(form) {
        delete form.dataset.pmSubmitting;
        form.classList.remove('pm-form-submitting');
        submitControls(form).forEach(resetControl);
    }

    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!form || form.tagName !== 'FORM' || form.dataset.submitGuard === 'off' || event.defaultPrevented) {
            return;
        }

        // Some complex forms already provide their own submission lock.
        if (form.dataset.submitting === 'true' && form.dataset.pmSubmitting !== 'true') {
            return;
        }

        if (form.dataset.pmSubmitting === 'true') {
            event.preventDefault();
            return;
        }

        form.dataset.pmSubmitting = 'true';
        form.classList.add('pm-form-submitting');
        setLoadingState(findSubmitter(event, form));

        // Restore the form when a later event handler cancels submission.
        window.setTimeout(function () {
            if (event.defaultPrevented) {
                resetForm(form);
            }
        }, 0);
    });

    window.addEventListener('pageshow', function () {
        document.querySelectorAll('form.pm-form-submitting').forEach(resetForm);
    });
})();
