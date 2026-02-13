/**
 * HyArena2 - Leaderboard Page JavaScript
 * Handles arena tabs, sorting, and pagination
 */

let currentArena = 'global';
let currentSort = 'pvp_kills';
let sortDirection = 'desc';
let currentPage = 1;
let totalPages = 0;
const ENTRIES_PER_PAGE = 25;

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    loadArenas();
    loadLeaderboardData();
    initSorting();
    initPagination();
});

// ==========================================
// Tabs
// ==========================================
function initTabs() {
    const globalTab = document.querySelector('.tab-btn[data-arena="global"]');
    if (globalTab) {
        globalTab.addEventListener('click', () => selectArena('global'));
    }
}

async function loadArenas() {
    const tabsContainer = document.getElementById('leaderboard-tabs');
    if (!tabsContainer) return;

    try {
        const response = await fetch('/api/arenas');
        const data = await response.json();

        if (data.success && data.data.length > 0) {
            data.data.forEach(arena => {
                if (tabsContainer.querySelector(`[data-arena="${arena.id}"]`)) return;

                const btn = document.createElement('button');
                btn.className = 'tab-btn';
                btn.dataset.arena = arena.id;
                btn.textContent = arena.display_name;
                btn.addEventListener('click', () => selectArena(arena.id));
                tabsContainer.appendChild(btn);
            });
        }
    } catch (e) {
        console.log('Failed to load arenas:', e.message);
    }
}

function selectArena(arenaId) {
    currentArena = arenaId;
    currentPage = 1;

    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.arena === arenaId);
    });

    loadLeaderboardData();
}

// ==========================================
// Load Data
// ==========================================
async function loadLeaderboardData() {
    const tbody = document.getElementById('leaderboard-body');
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="9">
                <div class="table-loading">
                    <div class="spinner"></div>
                    <span>Loading leaderboard...</span>
                </div>
            </td>
        </tr>
    `;

    try {
        const arena = currentArena === 'global' ? 'global' : currentArena;
        const response = await fetch(
            `/api/leaderboard?arena=${encodeURIComponent(arena)}&sort=${currentSort}&order=${sortDirection}&page=${currentPage}&per_page=${ENTRIES_PER_PAGE}`
        );
        const data = await response.json();

        if (data.success) {
            const entries = data.data.entries;
            totalPages = data.data.total_pages;

            const summaryPlayers = document.getElementById('summary-players');
            if (summaryPlayers) {
                summaryPlayers.textContent = data.data.total.toLocaleString('en-US');
            }

            if (entries.length > 0) {
                renderLeaderboard(entries);
            } else {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="9">
                            <div class="table-empty">
                                <p>No players found for this arena yet.</p>
                            </div>
                        </td>
                    </tr>
                `;
            }

            updatePagination();
        }
    } catch (e) {
        console.log('Failed to load leaderboard:', e.message);
        tbody.innerHTML = `
            <tr>
                <td colspan="9">
                    <div class="table-error"><p>Failed to load leaderboard</p></div>
                </td>
            </tr>
        `;
    }
}

function renderLeaderboard(entries) {
    const tbody = document.getElementById('leaderboard-body');

    tbody.innerHTML = entries.map(entry => {
        const rank = entry.rank_position;
        const rankClass = rank <= 3 ? `rank-${rank}` : '';
        const pvpKd = parseFloat(entry.pvp_kd_ratio || 0).toFixed(2);
        const winRate = parseFloat(entry.win_rate || 0).toFixed(1);

        return `
            <tr class="leaderboard-row ${rankClass}">
                <td class="col-rank">
                    <span class="rank-badge ${rankClass}">${rank}</span>
                </td>
                <td class="col-player">
                    <a href="/player/${encodeURIComponent(entry.username)}" class="player-name player-link">${escapeHtml(entry.username)}</a>
                </td>
                <td class="col-stat">${formatNumber(entry.matches_won || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pvp_kills || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pvp_deaths || 0)}</td>
                <td class="col-stat kd-ratio">${pvpKd}</td>
                <td class="col-stat">${formatNumber(entry.pve_kills || 0)}</td>
                <td class="col-stat">${formatNumber(entry.pve_deaths || 0)}</td>
                <td class="col-stat win-rate">${winRate}%</td>
            </tr>
        `;
    }).join('');
}

// ==========================================
// Sorting
// ==========================================
function initSorting() {
    document.querySelectorAll('.sortable').forEach(th => {
        th.addEventListener('click', () => {
            const sortKey = th.dataset.sort;

            if (currentSort === sortKey) {
                sortDirection = sortDirection === 'desc' ? 'asc' : 'desc';
            } else {
                currentSort = sortKey;
                sortDirection = 'desc';
            }

            document.querySelectorAll('.sortable').forEach(el => {
                el.classList.remove('active', 'asc', 'desc');
            });
            th.classList.add('active', sortDirection);

            currentPage = 1;
            loadLeaderboardData();
        });
    });
}

// ==========================================
// Pagination
// ==========================================
function initPagination() {
    document.getElementById('prev-page')?.addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            loadLeaderboardData();
        }
    });

    document.getElementById('next-page')?.addEventListener('click', () => {
        if (currentPage < totalPages) {
            currentPage++;
            loadLeaderboardData();
        }
    });
}

function updatePagination() {
    const prevBtn = document.getElementById('prev-page');
    const nextBtn = document.getElementById('next-page');
    const info = document.getElementById('pagination-info');

    if (prevBtn) prevBtn.disabled = currentPage <= 1;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages;
    if (info) info.textContent = `Page ${currentPage} of ${Math.max(totalPages, 1)}`;
}

// ==========================================
// Utilities
// ==========================================
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
