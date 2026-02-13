/**
 * HyArena2 - Main JavaScript
 * Pure ES6, no dependencies
 */

// ==========================================
// DOM Ready
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    initScrollEffects();
    initCopyButton();
    initServerStatus();
    initLiveStats();

    // Close modals with ESC key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeMatchModal();
        }
    });
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

        document.querySelectorAll('.kit-card, .feature-card').forEach(el => {
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
// Server Status
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
// Live Stats
// ==========================================
function initLiveStats() {
    loadServerStats();
    loadLeaderboard();
    loadRecentMatches();
    loadArenas();

    // Refresh every 30 seconds (except arenas - they rarely change)
    setInterval(() => {
        loadServerStats();
        loadLeaderboard();
        loadRecentMatches();
    }, 30000);
}

/**
 * Animate a number counting up
 */
function animateNumber(element, target, duration = 1000) {
    if (!element) return;
    const start = parseInt(element.textContent) || 0;
    const increment = (target - start) / (duration / 16);
    let current = start;

    const timer = setInterval(() => {
        current += increment;
        if ((increment > 0 && current >= target) || (increment < 0 && current <= target)) {
            element.textContent = formatNumber(target);
            clearInterval(timer);
        } else {
            element.textContent = formatNumber(Math.floor(current));
        }
    }, 16);
}

/**
 * Format large numbers with K/M suffix
 */
function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toLocaleString('en-US');
}

/**
 * Load server statistics (4 counters)
 */
async function loadServerStats() {
    try {
        const response = await fetch('/api/server-stats');
        const data = await response.json();

        if (data.success) {
            const stats = data.data;
            animateNumber(document.getElementById('stat-matches'), stats.total_matches);
            animateNumber(document.getElementById('stat-players'), stats.total_players);
            animateNumber(document.getElementById('stat-kills'), stats.total_kills);
            animateNumber(document.getElementById('stat-today'), stats.matches_today);
        }
    } catch (e) {
        console.log('Failed to load server stats:', e.message);
    }
}

/**
 * Load leaderboard top 5
 */
async function loadLeaderboard() {
    const container = document.getElementById('leaderboard-list');
    if (!container) return;

    try {
        const response = await fetch('/api/leaderboard?limit=5');
        const data = await response.json();

        if (data.success && data.data.entries && data.data.entries.length > 0) {
            const entries = data.data.entries;
            container.innerHTML = entries.map((entry, index) => {
                const botKd = ((entry.bot_kills || 0) / Math.max(entry.bot_deaths || 1, 1)).toFixed(2);
                return `
                <div class="leaderboard-entry rank-${index + 1}">
                    <span class="leaderboard-rank">#${entry.rank}</span>
                    <a href="/player/${encodeURIComponent(entry.username)}" class="leaderboard-name player-link">${escapeHtml(entry.username)}</a>
                    <div class="leaderboard-stats">
                        <span class="wins">${entry.wins} Wins</span>
                        <span class="kd" title="PvP K/D">K/D ${entry.kd_ratio.toFixed(2)}</span>
                        <span class="kd bot-kd" title="Bot K/D">Bot ${botKd}</span>
                    </div>
                </div>
            `;
            }).join('');
        } else {
            container.innerHTML = `
                <div class="no-data">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M12 15l-2 5-1-2-2 1 1.5-4.5M12 15l2 5 1-2 2 1-1.5-4.5M12 15V8M8 8h8"/>
                        <circle cx="12" cy="5" r="3"/>
                    </svg>
                    <p>No fighters yet.<br>Be the first!</p>
                </div>
            `;
        }
    } catch (e) {
        console.log('Failed to load leaderboard:', e.message);
        container.innerHTML = '<div class="leaderboard-loading">Failed to load</div>';
    }
}

// Store matches globally for modal access
let recentMatchesData = [];

/**
 * Load recent matches
 */
async function loadRecentMatches() {
    const container = document.getElementById('battles-list');
    if (!container) return;

    try {
        const response = await fetch('/api/recent-matches?limit=5');
        const data = await response.json();

        if (data.success && data.data.matches && data.data.matches.length > 0) {
            const matches = data.data.matches;
            recentMatchesData = matches;

            container.innerHTML = matches.map((match, index) => {
                const timeAgo = getTimeAgo(new Date(match.ended_at));
                const winnerKills = match.winner_kills ?? 0;
                const winnerIsBot = match.winner_is_bot ?? false;

                let winnerDisplay;
                if (!match.winner) {
                    winnerDisplay = 'Draw';
                } else if (winnerIsBot) {
                    winnerDisplay = `🤖 ${escapeHtml(match.winner)}`;
                } else {
                    winnerDisplay = escapeHtml(match.winner);
                }

                const duration = formatDuration(match.duration_seconds);

                return `
                    <div class="battle-entry clickable" onclick="showMatchDetails(${index})" title="Click for details">
                        <span class="battle-arena">${escapeHtml(match.arena_name)}</span>
                        <span class="battle-result">
                            <span class="winner ${winnerIsBot ? 'bot-winner' : ''}">${winnerDisplay}</span>
                            ${match.winner ? `<span class="vs">scored ${winnerKills} kills in ${duration}</span>` : ''}
                        </span>
                        <span class="battle-time">${timeAgo}</span>
                    </div>
                `;
            }).join('');
        } else {
            container.innerHTML = `
                <div class="no-data">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M14.5 4l-5 5 5 5M9.5 20l5-5-5-5"/>
                    </svg>
                    <p>No battles yet.<br>Start the first match!</p>
                </div>
            `;
        }
    } catch (e) {
        console.log('Failed to load recent matches:', e.message);
        container.innerHTML = '<div class="battles-loading">Failed to load</div>';
    }
}

/**
 * Show match details modal
 */
function showMatchDetails(index) {
    const match = recentMatchesData[index];
    if (!match) return;

    // Create modal if it doesn't exist
    let modal = document.getElementById('match-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'match-modal';
        modal.className = 'modal';
        modal.innerHTML = `
            <div class="modal-content match-details-modal">
                <button class="modal-close" onclick="closeMatchModal()">&times;</button>
                <div id="match-modal-body"></div>
            </div>
        `;
        document.body.appendChild(modal);

        // Close on backdrop click
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeMatchModal();
        });
    }

    const duration = formatDuration(match.duration_seconds);
    const endedAt = new Date(match.ended_at).toLocaleString('en-US');
    const winConditionText = translateWinCondition(match.win_condition, 0);

    const participants = match.participants || [...(match.players || []), ...(match.bots || [])];

    let participantsHtml = '';
    if (participants.length > 0) {
        participantsHtml = `
            <table class="match-scoreboard">
                <thead>
                    <tr>
                        <th>#</th>
                        <th>Participant</th>
                        <th>Kills</th>
                        <th>Deaths</th>
                        <th>Damage</th>
                        <th>K/D</th>
                    </tr>
                </thead>
                <tbody>
                    ${participants.map((p, i) => {
                        const totalKills = (p.kills || 0) + (p.bot_kills || 0);
                        const deaths = p.deaths || 0;
                        const kd = deaths > 0 ? (totalKills / deaths).toFixed(2) : totalKills.toFixed(2);
                        const damageDealt = Math.round(p.damage_dealt || 0);
                        const damageReceived = Math.round(p.damage_received || 0);
                        const isBot = p.is_bot ?? false;
                        const isWinner = p.is_winner ?? false;

                        let nameHtml;
                        if (isBot) {
                            nameHtml = `<span class="participant-bot">🤖 ${escapeHtml(p.username)}</span>`;
                        } else {
                            nameHtml = `<a href="/player/${encodeURIComponent(p.username)}" class="participant-player">${escapeHtml(p.username)}</a>`;
                        }

                        return `
                            <tr class="${isWinner ? 'winner-row' : ''}">
                                <td>${i + 1}</td>
                                <td>${nameHtml} ${isWinner ? '<span class="winner-badge">👑</span>' : ''}</td>
                                <td>${totalKills}</td>
                                <td>${deaths}</td>
                                <td><span class="dmg-dealt">${damageDealt}</span>/<span class="dmg-received">${damageReceived}</span></td>
                                <td>${kd}</td>
                            </tr>
                        `;
                    }).join('')}
                </tbody>
            </table>
        `;
    } else {
        participantsHtml = '<p class="no-participants">No participant data available.</p>';
    }

    document.getElementById('match-modal-body').innerHTML = `
        <h2>Match Details</h2>
        <div class="match-info">
            <div class="match-info-row">
                <span class="label">Arena:</span>
                <span class="value">${escapeHtml(match.arena_name)}</span>
            </div>
            <div class="match-info-row">
                <span class="label">Game Mode:</span>
                <span class="value">${winConditionText}</span>
            </div>
            <div class="match-info-row">
                <span class="label">Duration:</span>
                <span class="value">${duration}</span>
            </div>
            <div class="match-info-row">
                <span class="label">Participants:</span>
                <span class="value">${match.player_count}</span>
            </div>
            <div class="match-info-row">
                <span class="label">Ended:</span>
                <span class="value">${endedAt}</span>
            </div>
        </div>
        <h3>Scoreboard</h3>
        ${participantsHtml}
    `;

    modal.classList.add('show');
    document.body.style.overflow = 'hidden';
}

/**
 * Close match details modal
 */
function closeMatchModal() {
    const modal = document.getElementById('match-modal');
    if (modal) {
        modal.classList.remove('show');
        document.body.style.overflow = '';
    }
}

/**
 * Load arenas table
 */
async function loadArenas() {
    const tbody = document.getElementById('arenas-tbody');
    if (!tbody) return;

    try {
        const response = await fetch('/api/arenas');
        const data = await response.json();

        if (data.success && data.data.arenas && data.data.arenas.length > 0) {
            const arenas = data.data.arenas;
            tbody.innerHTML = arenas.map(arena => {
                const playerRange = arena.min_players === arena.max_players
                    ? arena.min_players
                    : `${arena.min_players}-${arena.max_players}`;

                const matchLength = formatMatchDuration(arena.match_duration_seconds);
                const winCondition = translateWinCondition(arena.win_condition, arena.kill_target);
                const respawnBadge = arena.allow_respawn
                    ? '<span class="badge badge-respawn-yes">Yes</span>'
                    : '<span class="badge badge-respawn-no">No</span>';

                const sizeFormatted = formatAreaSize(arena.area_size_blocks);

                return `
                    <tr>
                        <td>
                            <div class="arena-name">${escapeHtml(arena.display_name)}</div>
                            ${arena.description ? `<div class="arena-desc">${escapeHtml(arena.description)}</div>` : ''}
                        </td>
                        <td>${playerRange}</td>
                        <td>${matchLength}</td>
                        <td><span class="badge badge-mode">${winCondition}</span></td>
                        <td>${respawnBadge}</td>
                        <td>${arena.spawn_point_count}</td>
                        <td><span class="size-value">${sizeFormatted}</span> <span class="size-unit">Blocks</span></td>
                    </tr>
                `;
            }).join('');
        } else {
            tbody.innerHTML = `
                <tr class="arenas-empty">
                    <td colspan="7">No arenas available.</td>
                </tr>
            `;
        }
    } catch (e) {
        console.log('Failed to load arenas:', e.message);
        tbody.innerHTML = `
            <tr class="arenas-error">
                <td colspan="7">Failed to load arenas.</td>
            </tr>
        `;
    }
}

// ==========================================
// Utility Functions
// ==========================================

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
