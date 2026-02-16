/**
 * HyArena2 - Player Profile Page JavaScript
 * Loads and displays player statistics with PvP/PvE split
 */

document.addEventListener('DOMContentLoaded', () => {
    loadPlayerProfile();
});

async function loadPlayerProfile() {
    const loadingEl = document.getElementById('player-loading');
    const errorEl = document.getElementById('player-error');
    const contentEl = document.getElementById('player-content');
    const playerName = document.getElementById('player-identifier')?.dataset.name;

    if (!playerName) {
        showError();
        return;
    }

    try {
        const response = await fetch(`/api/player/${encodeURIComponent(playerName)}`);
        const data = await response.json();

        if (data.success && data.data.player) {
            displayPlayerProfile(data.data);
            loadingEl.style.display = 'none';
            contentEl.style.display = 'block';
            document.title = `${data.data.player.username} | HyArena`;
        } else {
            showError();
        }
    } catch (e) {
        console.error('Failed to load player profile:', e);
        showError();
    }
}

function showError() {
    document.getElementById('player-loading').style.display = 'none';
    document.getElementById('player-error').style.display = 'flex';
}

function displayPlayerProfile(data) {
    const player = data.player;
    const globalStats = data.global_stats || {};
    const arenaStats = data.arena_stats || [];
    const recentMatches = data.recent_matches || [];

    // Player Header
    document.getElementById('player-name').textContent = player.username;

    const joinedEl = document.getElementById('player-joined');
    if (joinedEl && player.first_seen) {
        const joinDate = new Date(player.first_seen);
        joinedEl.textContent = `Joined: ${joinDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}`;
    }

    const lastSeenEl = document.getElementById('player-last-seen');
    if (lastSeenEl && player.last_seen) {
        lastSeenEl.textContent = `Last seen: ${getTimeAgo(new Date(player.last_seen))}`;
    }

    // Honor rank badge
    const rankEl = document.getElementById('player-rank');
    if (rankEl) {
        const rank = player.honor_rank || 'Unranked';
        rankEl.textContent = rank;
        rankEl.dataset.rank = rank.toLowerCase();
    }

    // Honor value
    setStatText('player-honor', formatNumber(player.honor || 0));

    // Arena Points
    setStatText('player-ap', formatNumber(player.arena_points || 0));

    // Global PvP Stats
    setStatText('stat-wins', formatNumber(globalStats.matches_won || 0));
    setStatText('stat-pvp-kills', formatNumber(globalStats.pvp_kills || 0));
    setStatText('stat-pvp-deaths', formatNumber(globalStats.pvp_deaths || 0));
    setStatText('stat-pvp-kd', parseFloat(globalStats.pvp_kd_ratio || 0).toFixed(2));
    setStatText('stat-games', formatNumber(globalStats.matches_played || 0));
    setStatText('stat-winrate', parseFloat(globalStats.win_rate || 0).toFixed(1) + '%');

    // Global PvE Stats
    setStatText('stat-pve-kills', formatNumber(globalStats.pve_kills || 0));
    setStatText('stat-pve-deaths', formatNumber(globalStats.pve_deaths || 0));
    const pveKd = globalStats.pve_deaths > 0
        ? (parseFloat(globalStats.pve_kills || 0) / globalStats.pve_deaths).toFixed(2)
        : parseFloat(globalStats.pve_kills || 0).toFixed(2);
    setStatText('stat-pve-kd', pveKd);

    // Kit Stats
    const kitStats = data.kit_stats || [];
    const kitContainer = document.getElementById('kit-stats-container');
    if (kitContainer) {
        if (kitStats.length > 0) {
            const rows = kitStats.map(kit => {
                const avgDmg = kit.matches_played > 0
                    ? Math.round(kit.damage_dealt / kit.matches_played)
                    : 0;
                return `
                    <tr>
                        <td class="col-kit">${escapeHtml(kit.kit_name || kit.kit_id)}</td>
                        <td class="col-stat">${kit.matches_played}</td>
                        <td class="col-stat col-usage">
                            <div class="usage-bar-wrapper">
                                <div class="usage-bar-fill" style="width: ${parseFloat(kit.usage_pct || 0)}%"></div>
                                <span class="usage-bar-text">${parseFloat(kit.usage_pct || 0).toFixed(1)}%</span>
                            </div>
                        </td>
                        <td class="col-stat">${kit.matches_won}</td>
                        <td class="col-stat highlight">${parseFloat(kit.win_rate || 0).toFixed(1)}%</td>
                        <td class="col-stat highlight">${parseFloat(kit.pvp_kd_ratio || 0).toFixed(2)}</td>
                        <td class="col-stat pve-stat">${parseFloat(kit.pve_kd_ratio || 0).toFixed(2)}</td>
                        <td class="col-stat">${formatNumber(avgDmg)}</td>
                    </tr>
                `;
            }).join('');

            kitContainer.innerHTML = `
                <table class="kit-stats-table">
                    <thead>
                        <tr>
                            <th class="col-kit">Kit</th>
                            <th class="col-stat">Matches</th>
                            <th class="col-stat col-usage">Usage</th>
                            <th class="col-stat">Wins</th>
                            <th class="col-stat">Win Rate</th>
                            <th class="col-stat">PvP K/D</th>
                            <th class="col-stat">PvE K/D</th>
                            <th class="col-stat">Avg Dmg</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            `;
        } else {
            kitContainer.innerHTML = '<div class="no-data-message"><p>No kit statistics yet.</p></div>';
        }
    }

    // Arena Stats
    const arenaContainer = document.getElementById('arena-stats-list');
    if (arenaContainer) {
        if (arenaStats.length > 0) {
            arenaContainer.innerHTML = arenaStats.map(arena => {
                const pvpKd = parseFloat(arena.pvp_kd_ratio || 0).toFixed(2);
                const winRate = parseFloat(arena.win_rate || 0).toFixed(1);
                const hasPveStats = (arena.pve_kills || 0) > 0 || (arena.pve_deaths || 0) > 0;
                const pveKd = arena.pve_deaths > 0
                    ? (parseFloat(arena.pve_kills || 0) / arena.pve_deaths).toFixed(2)
                    : parseFloat(arena.pve_kills || 0).toFixed(2);
                const isWaveDef = arena.game_mode === 'wave_defense';

                // Wave defense: show Best Wave instead of Wins/Win Rate
                let primaryStatsHtml;
                if (isWaveDef) {
                    const bestWave = arena.best_waves_survived != null ? arena.best_waves_survived : '-';
                    primaryStatsHtml = `
                        <div class="arena-stat highlight">
                            <span class="value">${bestWave}</span>
                            <span class="label">Best Wave</span>
                        </div>
                        <div class="arena-stat">
                            <span class="value">${arena.pve_kills || 0}</span>
                            <span class="label">PvE Kills</span>
                        </div>
                        <div class="arena-stat">
                            <span class="value">${arena.pve_deaths || 0}</span>
                            <span class="label">PvE Deaths</span>
                        </div>
                        <div class="arena-stat highlight">
                            <span class="value">${pveKd}</span>
                            <span class="label">PvE K/D</span>
                        </div>
                    `;
                } else {
                    primaryStatsHtml = `
                        <div class="arena-stat">
                            <span class="value">${arena.matches_won || 0}</span>
                            <span class="label">Wins</span>
                        </div>
                        <div class="arena-stat">
                            <span class="value">${arena.pvp_kills || 0}</span>
                            <span class="label">Kills</span>
                        </div>
                        <div class="arena-stat">
                            <span class="value">${arena.pvp_deaths || 0}</span>
                            <span class="label">Deaths</span>
                        </div>
                        <div class="arena-stat highlight">
                            <span class="value">${pvpKd}</span>
                            <span class="label">K/D</span>
                        </div>
                        <div class="arena-stat highlight">
                            <span class="value">${winRate}%</span>
                            <span class="label">Win Rate</span>
                        </div>
                    `;
                }

                return `
                <div class="arena-stat-card">
                    <div class="arena-stat-header">
                        <h3>${escapeHtml(arena.arena_name || arena.arena_id)}</h3>
                        <div class="arena-header-meta">
                            <span class="arena-games">${arena.matches_played} games</span>
                        </div>
                    </div>
                    <h4 class="arena-stat-subtitle">${isWaveDef ? 'PvE' : 'PvP'}</h4>
                    <div class="arena-stat-grid">
                        ${primaryStatsHtml}
                    </div>
                    ${!isWaveDef && hasPveStats ? `
                    <h4 class="arena-stat-subtitle pve-subtitle">PvE</h4>
                    <div class="arena-stat-grid pve-grid">
                        <div class="arena-stat pve-stat">
                            <span class="value">${arena.pve_kills || 0}</span>
                            <span class="label">PvE Kills</span>
                        </div>
                        <div class="arena-stat pve-stat">
                            <span class="value">${arena.pve_deaths || 0}</span>
                            <span class="label">PvE Deaths</span>
                        </div>
                        <div class="arena-stat pve-stat highlight">
                            <span class="value">${pveKd}</span>
                            <span class="label">PvE K/D</span>
                        </div>
                    </div>
                    ` : ''}
                </div>
            `;
            }).join('');
        } else {
            arenaContainer.innerHTML = '<div class="no-data-message"><p>No arena statistics yet.</p></div>';
        }
    }

    // Season History
    loadPlayerSeasonHistory(player.uuid);

    // Recent Matches
    const matchesContainer = document.getElementById('recent-matches-list');
    if (matchesContainer) {
        if (recentMatches.length > 0) {
            matchesContainer.innerHTML = recentMatches.map(match => {
                const isWave = match.game_mode === 'wave_defense';
                const isWinner = match.is_winner == 1;
                const resultClass = isWave ? 'wave' : (isWinner ? 'win' : 'loss');
                const resultText = isWave
                    ? `Wave ${match.waves_survived != null ? match.waves_survived : '?'}`
                    : (isWinner ? 'Victory' : 'Defeat');
                const timeAgo = getTimeAgo(new Date(match.ended_at));

                let statsHtml = `<span class="match-stats-pvp">${match.pvp_kills || 0} PvP Kills / ${match.pvp_deaths || 0} Deaths</span>`;
                if ((match.pve_kills || 0) > 0 || (match.pve_deaths || 0) > 0) {
                    statsHtml += `<span class="match-stats-pve">${match.pve_kills || 0} PvE Kills / ${match.pve_deaths || 0} Deaths</span>`;
                }

                const matchBgSrc = match.arena_icon
                    ? `/assets/images/maps/${encodeURIComponent(match.arena_icon)}`
                    : '/assets/images/maps/noimage.png';

                return `
                    <div class="match-card ${resultClass}" style="background-image: url('${matchBgSrc}')">
                        <span class="result-badge ${resultClass}">${resultText}</span>
                        <span class="match-arena">${escapeHtml(match.arena_name)}</span>
                        <div class="match-stats-wrapper">${statsHtml}</div>
                        <span class="match-time">${timeAgo}</span>
                    </div>
                `;
            }).join('');
        } else {
            matchesContainer.innerHTML = '<div class="no-data-message"><p>No matches played yet.</p></div>';
        }
    }
}

async function loadPlayerSeasonHistory(playerUuid) {
    const container = document.getElementById('season-history-list');
    if (!container) return;

    try {
        const response = await fetch(`/api/player/${encodeURIComponent(playerUuid)}/seasons`);
        const data = await response.json();

        if (data.success && data.data.seasons && data.data.seasons.length > 0) {
            container.innerHTML = data.data.seasons.map(season => {
                const isActive = season.status === 'active';
                const statusClass = isActive ? 'active' : 'ended';
                const statusText = isActive ? 'Active' : 'Ended';

                const startDate = new Date(season.starts_at).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
                const endDate = new Date(season.ends_at).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });

                const rank = season.rank_position
                    ? `#${season.rank_position}${season.total_participants ? ` / ${season.total_participants}` : ''}`
                    : '-';
                const rankClass = season.rank_position && season.rank_position <= 3 ? `rank-${season.rank_position}` : '';

                return `
                    <a href="/seasons/${encodeURIComponent(season.slug)}" class="season-history-card ${statusClass}">
                        <div class="season-history-header">
                            <span class="season-history-name">${escapeHtml(season.name)}</span>
                            <span class="season-history-status ${statusClass}">${statusText}</span>
                        </div>
                        <div class="season-history-stats">
                            <div class="season-history-rank ${rankClass}">
                                <span class="value">${rank}</span>
                                <span class="label">Rank</span>
                            </div>
                            <div class="season-history-stat">
                                <span class="value">${season.matches_played || 0}</span>
                                <span class="label">Games</span>
                            </div>
                            <div class="season-history-stat">
                                <span class="value">${season.matches_won || 0}</span>
                                <span class="label">Wins</span>
                            </div>
                            <div class="season-history-stat">
                                <span class="value">${season.pvp_kills || 0}</span>
                                <span class="label">Kills</span>
                            </div>
                        </div>
                        <div class="season-history-dates">${startDate} - ${endDate}</div>
                    </a>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('Failed to load season history:', e);
    }
}

function setStatText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatNumber(num) {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toLocaleString('en-US');
}

function getTimeAgo(date) {
    const seconds = Math.floor((new Date() - date) / 1000);
    if (seconds < 60) return 'Just now';
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m ago`;
    if (seconds < 86400) return `${Math.floor(seconds / 3600)}h ago`;
    if (seconds < 604800) return `${Math.floor(seconds / 86400)}d ago`;
    return date.toLocaleDateString('en-US');
}
