/**
 * HyArena2 - Arenas Page JavaScript
 * Handles arena table loading and game mode cards
 */

// Game mode icons (SVG paths)
const gameModeIcons = {
    'duel': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 17.5L3 6V3h3l11.5 11.5"/><path d="M13 7.5l4-4 4 4-4 4"/><path d="M9.5 17.5L21 6V3h-3L6.5 14.5"/></svg>',
    'lms': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 15l-2 5-1-2-2 1 1.5-4.5M12 15l2 5 1-2 2 1-1.5-4.5M12 15V8M8 8h8"/><circle cx="12" cy="5" r="3"/></svg>',
    'deathmatch': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 8v8M8 12h8"/></svg>',
    'koth': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M2 20h20L18 8l-3 4-3-8-3 8-3-4z"/></svg>',
    'kit_roulette': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12a9 9 0 11-6.22-8.56"/><path d="M21 3v6h-6"/></svg>',
    'wave_defense': '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>'
};

// Game mode color accents
const gameModeColors = {
    'duel': '#e74c3c',
    'lms': '#e67e22',
    'deathmatch': '#c0392b',
    'koth': '#f1c40f',
    'kit_roulette': '#9b59b6',
    'wave_defense': '#2ecc71'
};

// Cache game modes for arena table rendering
let gameModeCache = {};

document.addEventListener('DOMContentLoaded', () => {
    loadGameModes();
    loadArenas();
});

/**
 * Load game modes and render cards
 */
async function loadGameModes() {
    const grid = document.getElementById('game-modes-grid');
    if (!grid) return;

    try {
        const response = await fetch('/api/game-modes');
        const data = await response.json();

        if (data.success && data.data.game_modes && data.data.game_modes.length > 0) {
            const modes = data.data.game_modes;

            // Cache for arena table
            modes.forEach(m => { gameModeCache[m.id] = m.display_name; });

            grid.innerHTML = modes.map(mode => {
                const icon = gameModeIcons[mode.id] || gameModeIcons['deathmatch'];
                const color = gameModeColors[mode.id] || '#e74c3c';

                return `
                    <div class="game-mode-card" style="--mode-color: ${color}">
                        <div class="game-mode-icon">${icon}</div>
                        <h4 class="game-mode-name">${escapeHtml(mode.display_name)}</h4>
                        <p class="game-mode-desc">${escapeHtml(mode.description || '')}</p>
                    </div>
                `;
            }).join('');
        } else {
            grid.innerHTML = '<div class="game-modes-empty">No game modes available yet.</div>';
        }
    } catch (e) {
        console.log('Failed to load game modes:', e.message);
        grid.innerHTML = '<div class="game-modes-empty">Failed to load game modes.</div>';
    }
}

/**
 * Translate game_mode string to display text (uses cache or fallback)
 */
function translateGameMode(mode) {
    if (gameModeCache[mode]) return gameModeCache[mode];
    const fallback = {
        'duel': 'Duel',
        'lms': 'Last Man Standing',
        'deathmatch': 'Deathmatch',
        'koth': 'King of the Hill',
        'kit_roulette': 'Kit Roulette',
        'wave_defense': 'Wave Defense'
    };
    return fallback[mode] || mode;
}

// Placeholder SVG for arenas without a map image
const arenaPlaceholderSvg = `<svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5" width="64" height="64">
    <polygon points="32 4 58 18 58 46 32 60 6 46 6 18 32 4" opacity="0.3"/>
    <polygon points="32 12 50 22 50 42 32 52 14 42 14 22 32 12" opacity="0.5"/>
    <line x1="32" y1="52" x2="32" y2="60" opacity="0.3"/>
    <polyline points="50 22 32 32 14 22" opacity="0.3"/>
    <line x1="32" y1="32" x2="32" y2="52" opacity="0.3"/>
</svg>`;

/**
 * Load arenas and render card grid
 */
async function loadArenas() {
    const container = document.getElementById('arena-cards');
    if (!container) return;

    try {
        const response = await fetch('/api/arenas');
        const data = await response.json();

        if (data.success && data.data.arenas && data.data.arenas.length > 0) {
            const arenas = data.data.arenas;
            container.innerHTML = arenas.map(arena => {
                const playerRange = arena.min_players === arena.max_players
                    ? arena.min_players
                    : `${arena.min_players}-${arena.max_players}`;

                const gameMode = translateGameMode(arena.game_mode);

                const imageHtml = arena.icon
                    ? `<div class="arena-card-image" style="background-image: url('/uploads/arenas/${encodeURIComponent(arena.icon)}')"></div>`
                    : `<div class="arena-card-image" style="background-image: url('/uploads/arenas/noimage.png')"></div>`;

                return `
                    <div class="arena-card">
                        ${imageHtml}
                        <div class="arena-card-body">
                            <h3 class="arena-card-name">${escapeHtml(arena.display_name)}</h3>
                            ${arena.description ? `<p class="arena-card-desc">${escapeHtml(arena.description)}</p>` : ''}
                            <div class="arena-card-meta">
                                <span class="arena-card-players">\u{1F465} ${playerRange}</span>
                                <span class="badge badge-mode">${escapeHtml(gameMode)}</span>
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        } else {
            container.innerHTML = '<div class="arena-cards-empty">No arenas available.</div>';
        }
    } catch (e) {
        console.log('Failed to load arenas:', e.message);
        container.innerHTML = '<div class="arena-cards-error">Failed to load arenas.</div>';
    }
}
