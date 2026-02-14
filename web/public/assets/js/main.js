/**
 * HyArena2 - Shared JavaScript
 * Navigation, scroll effects, copy button, server status, and utilities.
 * Loaded on every page. Page-specific scripts are in separate files.
 */

// ==========================================
// DOM Ready
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initScrollEffects();
    initCopyButton();
    initServerStatus();
});

// ==========================================
// Navigation
// ==========================================
function initNavigation() {
    const navbar = document.getElementById('navbar');
    const navToggle = document.getElementById('nav-toggle');
    const navMenu = document.getElementById('nav-menu');
    const navLinks = document.querySelectorAll('.nav-link');

    // Mobile menu toggle
    navToggle?.addEventListener('click', () => {
        navMenu.classList.toggle('active');
        navToggle.classList.toggle('active');
    });

    // Close menu on link click
    navLinks.forEach(link => {
        link.addEventListener('click', () => {
            navMenu.classList.remove('active');
            navToggle.classList.remove('active');
        });
    });

    // Close menu on outside click
    document.addEventListener('click', (e) => {
        if (!navMenu.contains(e.target) && !navToggle.contains(e.target)) {
            navMenu.classList.remove('active');
            navToggle.classList.remove('active');
        }
    });

    // Navbar scroll effect
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    }, { passive: true });
}

// ==========================================
// Scroll Effects
// ==========================================
function initScrollEffects() {
    // Smooth scroll for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', (e) => {
            const href = anchor.getAttribute('href');
            if (href === '#') return;

            const target = document.querySelector(href);
            if (target) {
                e.preventDefault();
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // Intersection Observer for animations
    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.style.animationPlayState = 'running';
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1 });

        document.querySelectorAll('.kit-card, .feature-card, .preview-card').forEach(el => {
            el.style.animationPlayState = 'paused';
            observer.observe(el);
        });
    }
}

// ==========================================
// Copy Button
// ==========================================
function initCopyButton() {
    const copyBtn = document.getElementById('copy-btn');
    const serverAddress = document.getElementById('server-address');

    copyBtn?.addEventListener('click', async () => {
        const address = serverAddress.textContent;

        try {
            await navigator.clipboard.writeText(address);
            copyBtn.classList.add('copied');

            setTimeout(() => {
                copyBtn.classList.remove('copied');
            }, 2000);
        } catch (err) {
            // Fallback for older browsers
            const textArea = document.createElement('textarea');
            textArea.value = address;
            textArea.style.position = 'fixed';
            textArea.style.left = '-9999px';
            document.body.appendChild(textArea);
            textArea.select();

            try {
                document.execCommand('copy');
                copyBtn.classList.add('copied');
                setTimeout(() => {
                    copyBtn.classList.remove('copied');
                }, 2000);
            } catch (e) {
                console.error('Failed to copy:', e);
            }

            document.body.removeChild(textArea);
        }
    });
}

// ==========================================
// Server Status (hero section)
// ==========================================
function initServerStatus() {
    const statusDot = document.getElementById('status-dot');
    const statusText = document.getElementById('status-text');
    const playerCount = document.getElementById('player-count');

    if (!statusDot || !statusText || !playerCount) return;

    async function checkServerStatus() {
        try {
            const response = await fetch('/api/server-stats', {
                method: 'GET',
                cache: 'no-cache',
                signal: AbortSignal.timeout(10000)
            });

            if (response.ok) {
                return await response.json();
            }
        } catch (e) {
            console.log('Status check failed:', e.message);
        }

        return null;
    }

    function updateStatusUI(data) {
        if (data === null || !data.success) {
            statusDot.className = 'status-dot';
            statusText.textContent = 'play.hyarena.de';
            playerCount.textContent = 'Status unavailable';
            return;
        }

        const stats = data.data;
        const online = stats.online_players ?? 0;
        const max = stats.max_players ?? 0;

        if (online > 0 || max > 0) {
            statusDot.classList.add('online');
            statusDot.classList.remove('offline');
            statusText.textContent = 'Server Online';

            if (max > 0) {
                playerCount.textContent = `${online}/${max} Players`;
            } else if (online > 0) {
                playerCount.textContent = `${online} Players online`;
            } else {
                playerCount.textContent = 'Join now!';
            }
        } else {
            statusDot.classList.add('online');
            statusDot.classList.remove('offline');
            statusText.textContent = 'Server Online';
            playerCount.textContent = 'Join now!';
        }
    }

    // Initial check
    checkServerStatus().then(updateStatusUI);

    // Refresh every 30 seconds
    setInterval(() => {
        checkServerStatus().then(updateStatusUI);
    }, 30000);
}

// ==========================================
// Shared Utility Functions
// ==========================================

/**
 * Format large numbers with K/M suffix
 */
function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toLocaleString('en-US');
}

/**
 * Format match duration in minutes
 */
function formatMatchDuration(seconds) {
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    if (remainingSeconds === 0) return `${minutes} min`;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')} min`;
}

/**
 * Translate win condition
 */
function translateWinCondition(condition, killTarget) {
    const translations = {
        'LAST_STANDING': 'Last Standing',
        'FIRST_TO_KILLS': killTarget > 0 ? `First to ${killTarget} Kills` : 'Most Kills',
        'TIMED_KILLS': 'Timed',
        'WAVE_SURVIVAL': 'Waves'
    };
    return translations[condition] || condition;
}

/**
 * Format area size with suffix
 */
function formatAreaSize(blocks) {
    if (blocks >= 1000000) return (blocks / 1000000).toFixed(1) + 'M';
    if (blocks >= 1000) return (blocks / 1000).toFixed(1) + 'K';
    return blocks.toLocaleString('en-US');
}

/**
 * Format duration in seconds to mm:ss min
 */
function formatDuration(seconds) {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')} min`;
}

/**
 * Calculate time ago string
 */
function getTimeAgo(date) {
    const seconds = Math.floor((new Date() - date) / 1000);
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    return `${Math.floor(seconds / 86400)}d ago`;
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
