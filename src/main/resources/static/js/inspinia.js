/*
 *
 *   INSPINIA - Responsive Admin Theme
 *   version 2.9.4
 *
 */

function localStorageSupport() {
    return (('localStorage' in window) && window['localStorage'] !== null);
}

function hasSlimScroll() {
    return typeof $.fn.slimScroll === 'function';
}

function syncResponsiveMenuState() {
    var body = $('body');
    if (window.innerWidth < 769) {
        body.addClass('body-small mini-navbar');
        return;
    }

    body.removeClass('body-small');
    if (!localStorageSupport()) {
        return;
    }

    var collapse = localStorage.getItem("collapse_menu");
    if (collapse == 'on') {
        body.addClass('mini-navbar');
    } else if (collapse == 'off') {
        body.removeClass('mini-navbar');
    }
}

$(document).ready(function () {

    function applyTheme(themeMode) {
        var body = $('body');
        var themeToggle = $('#light-dark-mode');
        var root = $(document.documentElement);
        if (themeMode === 'light') {
            body.addClass('light-skin');
        } else {
            body.removeClass('light-skin');
        }
        root.removeClass('theme-light theme-dark').addClass(themeMode === 'light' ? 'theme-light' : 'theme-dark');
        if (themeToggle.length) {
            themeToggle.attr('aria-label', themeMode === 'light' ? 'Switch to dark mode' : 'Switch to light mode');
            themeToggle.find('.mode-light-moon').toggle(themeMode !== 'light');
            themeToggle.find('.mode-light-sun').toggle(themeMode === 'light');
        }
    }

    function getThemeMode() {
        if (localStorageSupport()) {
            return localStorage.getItem('theme_mode') || 'light';
        }
        return 'light';
    }

    // Fast fix for position issue with Popper.js
    // Will be fixed in Bootstrap 4.1 - https://github.com/twbs/bootstrap/pull/24092
    if (window.Popper
        && Popper.Defaults
        && Popper.Defaults.modifiers
        && Popper.Defaults.modifiers.computeStyle) {
        Popper.Defaults.modifiers.computeStyle.gpuAcceleration = false;
    }

    syncResponsiveMenuState();

    // MetisMenu
    $('#side-menu').metisMenu();

    // Collapse ibox function
    $('.collapse-link').on('click', function (e) {
        e.preventDefault();
        var ibox = $(this).closest('div.ibox');
        var button = $(this).find('i');
        var content = ibox.children('.ibox-content');
        content.slideToggle(200);
        button.toggleClass('fa-chevron-up').toggleClass('fa-chevron-down');
        ibox.toggleClass('').toggleClass('border-bottom');
        setTimeout(function () {
            ibox.resize();
            ibox.find('[id^=map-]').resize();
        }, 50);
    });

    // Close ibox function
    $('.close-link').on('click', function (e) {
        e.preventDefault();
        var content = $(this).closest('div.ibox');
        content.remove();
    });

    // Fullscreen ibox function
    $('.fullscreen-link').on('click', function (e) {
        e.preventDefault();
        var ibox = $(this).closest('div.ibox');
        var button = $(this).find('i');
        $('body').toggleClass('fullscreen-ibox-mode');
        button.toggleClass('fa-expand').toggleClass('fa-compress');
        ibox.toggleClass('fullscreen');
        setTimeout(function () {
            $(window).trigger('resize');
        }, 100);
    });

    // Close menu in canvas mode
    $('.close-canvas-menu').on('click', function (e) {
        e.preventDefault();
        $("body").toggleClass("mini-navbar");
        SmoothlyMenu();
    });

    // Run menu of canvas
    if (hasSlimScroll()) {
        $('body.canvas-menu .sidebar-collapse').slimScroll({
            height: '100%',
            railOpacity: 0.9
        });
    }

    // Open close right sidebar
    $('.right-sidebar-toggle').on('click', function (e) {
        e.preventDefault();
        $('#right-sidebar').toggleClass('sidebar-open');
    });

    // Initialize slimscroll for right sidebar
    if (hasSlimScroll()) {
        $('.sidebar-container').slimScroll({
            height: '100%',
            railOpacity: 0.4,
            wheelStep: 10
        });
    }

    // Open close small chat
    $('.open-small-chat').on('click', function (e) {
        e.preventDefault();
        $(this).children().toggleClass('fa-comments').toggleClass('fa-times');
        $('.small-chat-box').toggleClass('active');
    });

    // Initialize slimscroll for small chat
    if (hasSlimScroll()) {
        $('.small-chat-box .content').slimScroll({
            height: '234px',
            railOpacity: 0.4
        });
    }

    // Small todo handler
    $('.check-link').on('click', function () {
        var button = $(this).find('i');
        var label = $(this).next('span');
        button.toggleClass('fa-check-square').toggleClass('fa-square-o');
        label.toggleClass('todo-completed');
        return false;
    });

    // Minimalize menu
    $('.navbar-minimalize').on('click', function (event) {
        event.preventDefault();
        var body = $("body");
        body.toggleClass("mini-navbar");
        if (localStorageSupport() && !body.hasClass('body-small')) {
            localStorage.setItem("collapse_menu", body.hasClass("mini-navbar") ? "on" : "off");
        }
        SmoothlyMenu();
    });

    // Toggle light/dark mode
    $('#light-dark-mode').on('click', function () {
        var nextTheme = getThemeMode() === 'light' ? 'dark' : 'light';
        if (localStorageSupport()) {
            localStorage.setItem('theme_mode', nextTheme);
        }
        applyTheme(nextTheme);
    });

    // Close the mobile drawer after choosing a page or tapping outside it
    $('#side-menu a').on('click', function () {
        var href = $(this).attr('href');
        if ($('body').hasClass('body-small') && href && href !== '#') {
            $('body').removeClass('mini-navbar');
        }
    });

    $(document).on('click', function (event) {
        var body = $('body');
        if (!body.hasClass('body-small') || !body.hasClass('mini-navbar')) {
            return;
        }

        if ($(event.target).closest('.navbar-static-side, .navbar-minimalize').length === 0) {
            body.removeClass('mini-navbar');
        }
    });

    // Tooltips demo
    $('.tooltip-demo').tooltip({
        selector: "[data-toggle=tooltip]",
        container: "body"
    });

    // Move right sidebar top after scroll
    $(window).scroll(function () {
        if ($(window).scrollTop() > 0 && !$('body').hasClass('fixed-nav')) {
            $('#right-sidebar').addClass('sidebar-top');
        } else {
            $('#right-sidebar').removeClass('sidebar-top');
        }
    });

    $("[data-toggle=popover]").popover();

    // Add slimscroll to element
    if (hasSlimScroll()) {
        $('.full-height-scroll').slimscroll({
            height: '100%'
        });
    }

    applyTheme(getThemeMode());
});

// Minimalize menu when screen is less than 768px
$(window).bind("resize", function () {
    syncResponsiveMenuState();
});

// Fixed Sidebar
$(window).bind("load", function () {
    if ($("body").hasClass('fixed-sidebar') && hasSlimScroll()) {
        $('.sidebar-collapse').slimScroll({
            height: '100%',
            railOpacity: 0.9
        });
    }
});

// Local Storage functions
// Set proper body class and plugins based on user configuration
$(document).ready(function () {
    if (localStorageSupport()) {
        var collapse = localStorage.getItem("collapse_menu");
        var fixedsidebar = localStorage.getItem("fixedsidebar");
        var fixednavbar = localStorage.getItem("fixednavbar");
        var boxedlayout = localStorage.getItem("boxedlayout");
        var fixedfooter = localStorage.getItem("fixedfooter");

        var body = $('body');

        if (fixedsidebar == 'on') {
            body.addClass('fixed-sidebar');
            if (hasSlimScroll()) {
                $('.sidebar-collapse').slimScroll({
                    height: '100%',
                    railOpacity: 0.9
                });
            }
        }

        if (body.hasClass('body-small')) {
            body.addClass('mini-navbar');
        } else if (collapse == 'on') {
            if (!body.hasClass('body-small')) {
                body.addClass('mini-navbar');
            }
        }

        if (fixednavbar == 'on') {
            $(".navbar-static-top").removeClass('navbar-static-top').addClass('navbar-fixed-top');
            body.addClass('fixed-nav');
        }

        if (boxedlayout == 'on') {
            body.addClass('boxed-layout');
        }

        if (fixedfooter == 'on') {
            $(".footer").addClass('fixed');
        }
    }
});

// For demo purpose - animation css script
function animationHover(element, animation) {
    element = $(element);
    element.hover(
        function () {
            element.addClass('animated ' + animation);
        },
        function () {
            // wait for animation to finish before removing classes
            window.setTimeout(function () {
                element.removeClass('animated ' + animation);
            }, 2000);
        }
    );
}

function SmoothlyMenu() {
    if (!$('body').hasClass('mini-navbar') || $('body').hasClass('body-small')) {
        // Hide menu in order to smoothly turn on when maximize menu
        $('#side-menu').hide();
        // For smoothly turn on menu
        setTimeout(
            function () {
                $('#side-menu').fadeIn(400);
            }, 200);
    } else if ($('body').hasClass('fixed-sidebar')) {
        $('#side-menu').hide();
        setTimeout(
            function () {
                $('#side-menu').fadeIn(400);
            }, 100);
    } else {
        // Remove all inline style from jquery fadeIn function to reset menu state
        $('#side-menu').removeAttr('style');
    }
}
