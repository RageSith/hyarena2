/**
 * HyArena2 - Live Stats Page JavaScript
 * Handles stats counters, leaderboard, recent battles, and match modal
 */

document.addEventListener('DOMContentLoaded', () => {
    loadServerStats();
    loadLeaderboard();
    loadRecentMatches();

    // Refresh every 30 seconds
    setInterval(() => {
        loadServerStats();
        loadLeaderboard();
        loadRecentMatches();
    }, 30000);

    // Close modals with ESC key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            closeMatchModal();
        }
    });
});

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
 * Load server statistics (4 counters)
 */
async function loadServerStats() {
    try {
        const response = await fetch('/api/server-stats');
        const data = await response.json();

        if (data.success) {
            const stats = data.data;
            animateNumber(document.getElementById('stat-matches'), stats.total_matches ?? 0);
            animateNumber(document.getElementById('stat-players'), stats.total_players ?? 0);
            animateNumber(document.getElementById('stat-kills'), stats.total_kills ?? 0);
            animateNumber(document.getElementById('stat-today'), stats.matches_today ?? 0);
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
                const rank = entry.rank_position ?? (index + 1);
                const wins = entry.matches_won ?? 0;
                const pvpKd = parseFloat(entry.pvp_kd_ratio ?? 0).toFixed(2);
                const pveKd = (entry.pve_deaths ?? 0) > 0
                    ? ((entry.pve_kills ?? 0) / entry.pve_deaths).toFixed(2)
                    : parseFloat(entry.pve_kills ?? 0).toFixed(2);

                return `
                <div class="leaderboard-entry rank-${index + 1}">
                    <span class="leaderboard-rank">#${rank}</span>
                    <a href="/player/${encodeURIComponent(entry.username)}" class="leaderboard-name player-link">${escapeHtml(entry.username)}</a>
                    <div class="leaderboard-stats">
                        <span class="wins">${wins} Wins</span>
                        <span class="kd" title="PvP K/D">K/D ${pvpKd}</span>
                        <span class="kd bot-kd" title="PvE K/D">PvE ${pveKd}</span>
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
                const winnerIsBot = match.winner_is_bot == 1;
                const duration = formatDuration(match.duration_seconds);

                let winnerDisplay;
                if (!match.winner_name && !match.winner_bot_name) {
                    winnerDisplay = 'Draw';
                } else if (winnerIsBot) {
                    winnerDisplay = `<span class="bot-winner">Bot: ${escapeHtml(match.winner_bot_name || 'Unknown')}</span>`;
                } else {
                    winnerDisplay = escapeHtml(match.winner_name);
                }

                return `
                    <div class="battle-entry clickable" onclick="showMatchDetails(${match.id})" title="Click for details">
                        <span class="battle-arena">${escapeHtml(match.arena_name)}</span>
                        <span class="battle-result">
                            <span class="winner ${winnerIsBot ? 'bot-winner' : ''}">${winnerDisplay}</span>
                            ${(match.winner_name || match.winner_bot_name) ? `<span class="vs">scored ${winnerKills} kills in ${duration}</span>` : ''}
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
 * Show match details modal (fetches full participant data)
 */
async function showMatchDetails(matchId) {
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

        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeMatchModal();
        });
    }

    // Show loading state
    document.getElementById('match-modal-body').innerHTML = '<div class="leaderboard-loading">Loading match details...</div>';
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';

    // Find basic match info from cached data
    const match = recentMatchesData.find(m => m.id == matchId);
    if (!match) {
        document.getElementById('match-modal-body').innerHTML = '<p>Match not found.</p>';
        return;
    }

    const duration = formatDuration(match.duration_seconds);
    const endedAt = new Date(match.ended_at).toLocaleString('en-US');
    const gameMode = translateGameMode(match.game_mode);

    // The match data from the list doesn't include participants.
    // Show what we have from the match summary.
    const winnerIsBot = match.winner_is_bot == 1;
    let winnerHtml;
    if (!match.winner_name && !match.winner_bot_name) {
        winnerHtml = '<span class="no-data-text">Draw</span>';
    } else if (winnerIsBot) {
        winnerHtml = `<span class="participant-bot">Bot: ${escapeHtml(match.winner_bot_name || 'Unknown')}</span>`;
    } else {
        winnerHtml = `<a href="/player/${encodeURIComponent(match.winner_name)}" class="participant-player">${escapeHtml(match.winner_name)}</a>`;
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
                <span class="value">${gameMode}</span>
            </div>
            <div class="match-info-row">
                <span class="label">Duration:</span>
                <span class="value">${duration}</span>
            </div>
            <div class="match-info-row">
                <span class="label">Participants:</span>
                <span class="value">${match.participant_count ?? '?'}</span>
            </div>
            <div class="match-info-row">
                <span class="label">Winner:</span>
                <span class="value">${winnerHtml} ${(match.winner_kills ?? 0) > 0 ? `(${match.winner_kills} kills)` : ''}</span>
            </div>
            <div class="match-info-row">
                <span class="label">Ended:</span>
                <span class="value">${endedAt}</span>
            </div>
        </div>
    `;
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
 * Translate game_mode string to display text
 */
function translateGameMode(mode) {
    const translations = {
        'duel': 'Duel (1v1)',
        'lms': 'Last Man Standing',
        'deathmatch': 'Deathmatch',
        'koth': 'King of the Hill',
        'kit_roulette': 'Kit Roulette',
        'wave_defense': 'Wave Defense'
    };
    return translations[mode] || mode;
}
