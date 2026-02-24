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
        const response = await fetch('/api/leaderboard?per_page=15');
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
                const isWaveDefense = match.game_mode === 'wave_defense';
                const isSpeedRun = match.game_mode === 'speed_run';
                const maxWaves = match.max_waves_survived;

                let winnerDisplay;
                let detailSpan;

                if (isSpeedRun) {
                    winnerDisplay = match.winner_finish_time_ms != null
                        ? formatSpeedRunTime(match.winner_finish_time_ms)
                        : 'DNF';
                    detailSpan = `<span class="vs">${escapeHtml(match.arena_name)}</span>`;
                } else if (isWaveDefense) {
                    winnerDisplay = maxWaves != null ? `Wave ${maxWaves}` : 'Wave Defense';
                    detailSpan = `<span class="vs">survived in ${duration}</span>`;
                } else if (!match.winner_name && !match.winner_bot_name) {
                    winnerDisplay = 'Draw';
                    detailSpan = '';
                } else if (winnerIsBot) {
                    winnerDisplay = `<span class="bot-winner">Bot: ${escapeHtml(match.winner_bot_name || 'Unknown')}</span>`;
                    detailSpan = `<span class="vs">scored ${winnerKills} kills in ${duration}</span>`;
                } else {
                    winnerDisplay = escapeHtml(match.winner_name);
                    detailSpan = `<span class="vs">scored ${winnerKills} kills in ${duration}</span>`;
                }

                const bgSrc = match.arena_icon
                    ? `/uploads/arenas/${encodeURIComponent(match.arena_icon)}`
                    : '/uploads/arenas/noimage.png';

                return `
                    <div class="battle-entry clickable" onclick="showMatchDetails(${match.id})" title="Click for details" style="background-image: url('${bgSrc}')">
                        <span class="battle-arena">${escapeHtml(match.arena_name)}</span>
                        <span class="battle-result">
                            <span class="winner ${isSpeedRun ? 'speedrun-result' : (isWaveDefense ? 'wave-result' : (winnerIsBot ? 'bot-winner' : ''))}">${winnerDisplay}</span>
                            ${detailSpan}
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

    try {
        const response = await fetch(`/api/match/${matchId}`);
        const data = await response.json();

        if (!data.success || !data.data.match) {
            document.getElementById('match-modal-body').innerHTML = '<p>Match not found.</p>';
            return;
        }

        const match = data.data.match;
        const participants = match.participants || [];
        const duration = formatDuration(match.duration_seconds);
        const endedAt = new Date(match.ended_at).toLocaleString('en-US');
        const gameMode = translateGameMode(match.game_mode);
        const isWaveDefense = match.game_mode === 'wave_defense';
        const isSpeedRun = match.game_mode === 'speed_run';

        // Result row
        let resultLabel, resultHtml;
        if (isSpeedRun) {
            // Find the best (winner) finish time
            const winner = participants.find(p => p.is_winner == 1);
            resultLabel = 'Finish Time';
            if (winner && winner.finish_time_ms) {
                resultHtml = formatSpeedRunTime(winner.finish_time_ms);
            } else {
                resultHtml = '<span class="no-data-text">Did Not Finish</span>';
            }
        } else if (isWaveDefense) {
            const wavesValues = participants.map(p => p.waves_survived).filter(w => w != null);
            const maxWaves = wavesValues.length > 0 ? Math.max(...wavesValues) : null;
            resultLabel = 'Best Wave';
            resultHtml = maxWaves != null ? `Wave ${maxWaves}` : 'N/A';
        } else if (!match.winner_name) {
            resultLabel = 'Winner';
            resultHtml = '<span class="no-data-text">Draw</span>';
        } else {
            resultLabel = 'Winner';
            resultHtml = `<a href="/player/${encodeURIComponent(match.winner_name)}" class="participant-player">${escapeHtml(match.winner_name)}</a>`;
        }

        // Participants table
        let participantsHtml;
        if (isSpeedRun) {
            // Speed run: show time, checkpoints, lives used
            const players = participants.filter(p => p.is_bot == 0);
            participantsHtml = `
                <table class="participants-table">
                    <thead>
                        <tr>
                            <th>Player</th>
                            <th>Time</th>
                            <th>Checkpoints</th>
                            <th>Lives</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${players.map(p => {
                            let timeDisplay = 'DNF';
                            let checkpoints = '-';
                            let livesUsed = '-';

                            if (p.json_data) {
                                try {
                                    const jd = typeof p.json_data === 'string' ? JSON.parse(p.json_data) : p.json_data;
                                    if (!jd.is_dnf && p.finish_time_ms) {
                                        timeDisplay = formatSpeedRunTime(p.finish_time_ms);
                                    }
                                    if (jd.checkpoints_reached != null) {
                                        checkpoints = jd.checkpoints_reached;
                                    }
                                    if (jd.lives_used != null) {
                                        livesUsed = jd.lives_used;
                                    }
                                } catch (e) {}
                            } else if (p.finish_time_ms) {
                                timeDisplay = formatSpeedRunTime(p.finish_time_ms);
                            }

                            const name = p.username || 'Unknown';
                            const isWinner = p.is_winner == 1;
                            const isPb = p.json_data ? (() => { try { const jd = typeof p.json_data === 'string' ? JSON.parse(p.json_data) : p.json_data; return jd.is_new_pb; } catch(e) { return false; } })() : false;

                            return `
                                <tr class="${isWinner ? 'winner-row' : ''}">
                                    <td><a href="/player/${encodeURIComponent(name)}" class="player-link">${escapeHtml(name)}</a>${isWinner ? ' <span class="winner-badge">W</span>' : ''}${isPb ? ' <span class="pb-badge">PB</span>' : ''}</td>
                                    <td class="${timeDisplay === 'DNF' ? 'dnf-text' : ''}">${timeDisplay}</td>
                                    <td>${checkpoints}</td>
                                    <td>${livesUsed}</td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            `;
        } else if (isWaveDefense) {
            // Wave defense: only real players, show wave survived
            const players = participants.filter(p => p.is_bot == 0);
            participantsHtml = `
                <table class="participants-table">
                    <thead>
                        <tr>
                            <th>Player</th>
                            <th>Kills</th>
                            <th>Deaths</th>
                            <th>Wave</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${players.map(p => {
                            const kills = (parseInt(p.pvp_kills) || 0) + (parseInt(p.pve_kills) || 0);
                            const deaths = (parseInt(p.pvp_deaths) || 0) + (parseInt(p.pve_deaths) || 0);
                            const wave = p.waves_survived ?? '-';
                            const name = p.username || 'Unknown';
                            return `
                                <tr>
                                    <td><a href="/player/${encodeURIComponent(name)}" class="player-link">${escapeHtml(name)}</a></td>
                                    <td>${kills}</td>
                                    <td>${deaths}</td>
                                    <td class="wave-col">${wave}</td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            `;
        } else {
            // Normal modes: all participants, combined kills/deaths
            participantsHtml = `
                <table class="participants-table">
                    <thead>
                        <tr>
                            <th>Player</th>
                            <th>Kills</th>
                            <th>Deaths</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${participants.map(p => {
                            const kills = (parseInt(p.pvp_kills) || 0) + (parseInt(p.pve_kills) || 0);
                            const deaths = (parseInt(p.pvp_deaths) || 0) + (parseInt(p.pve_deaths) || 0);
                            const isBot = p.is_bot == 1;
                            const isWinner = p.is_winner == 1;
                            const name = isBot ? (p.bot_name || 'Bot') : (p.username || 'Unknown');
                            const nameHtml = isBot
                                ? `<span class="participant-bot">${escapeHtml(name)}</span>`
                                : `<a href="/player/${encodeURIComponent(name)}" class="player-link">${escapeHtml(name)}</a>`;
                            return `
                                <tr class="${isWinner ? 'winner-row' : ''}">
                                    <td>${nameHtml}${isWinner ? ' <span class="winner-badge">W</span>' : ''}</td>
                                    <td>${kills}</td>
                                    <td>${deaths}</td>
                                </tr>
                            `;
                        }).join('')}
                    </tbody>
                </table>
            `;
        }

        const modalThumbSrc = match.arena_icon
            ? `/uploads/arenas/${encodeURIComponent(match.arena_icon)}`
            : '/uploads/arenas/noimage.png';

        document.getElementById('match-modal-body').innerHTML = `
            <div class="match-modal-image" style="background-image: url('${modalThumbSrc}')"></div>
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
                    <span class="label">${resultLabel}:</span>
                    <span class="value">${resultHtml}</span>
                </div>
                <div class="match-info-row">
                    <span class="label">Ended:</span>
                    <span class="value">${endedAt}</span>
                </div>
            </div>
            <h3>Participants</h3>
            ${participantsHtml}
        `;
    } catch (e) {
        console.log('Failed to load match details:', e.message);
        document.getElementById('match-modal-body').innerHTML = '<p>Failed to load match details.</p>';
    }
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
        'wave_defense': 'Wave Defense',
        'speed_run': 'Speed Run'
    };
    return translations[mode] || mode;
}

/**
 * Format speedrun time from milliseconds to M:SS.mmm
 */
function formatSpeedRunTime(ms) {
    if (ms == null || ms <= 0) return '--:--.---';
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    const millis = ms % 1000;
    return `${minutes}:${String(seconds).padStart(2, '0')}.${String(millis).padStart(3, '0')}`;
}
