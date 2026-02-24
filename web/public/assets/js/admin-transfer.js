document.addEventListener('DOMContentLoaded', function () {
    var sourceSelect = document.getElementById('tf-source');
    var targetSelect = document.getElementById('tf-target');
    var backupSelect = document.getElementById('bk-server');
    var gameData = null;

    function esc(str) {
        if (str == null) return '';
        var d = document.createElement('div');
        d.textContent = String(str);
        return d.innerHTML;
    }

    function show(el) { el.style.display = 'block'; }
    function hide(el) { el.style.display = 'none'; }

    function typeLabel(type) {
        var labels = {
            world: 'World',
            arena: 'Arena Config',
            kit: 'Kit Config',
            config: 'Config',
            mod_jar: 'Mod JAR'
        };
        return labels[type] || type;
    }

    // ==========================================
    // Transfer Section
    // ==========================================

    sourceSelect.addEventListener('change', onSourceChange);

    function onSourceChange() {
        var id = sourceSelect.value;
        var itemsEl = document.getElementById('tf-items');
        var groupsEl = document.getElementById('tf-groups');
        var loadingEl = document.getElementById('tf-loading');
        var resultEl = document.getElementById('tf-result');

        hide(resultEl);
        gameData = null;
        updateTransferButton();

        if (!id) {
            hide(itemsEl);
            return;
        }

        show(itemsEl);
        hide(groupsEl);
        show(loadingEl);

        fetch('/admin/api/transfer/game-data/' + encodeURIComponent(id))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                hide(loadingEl);

                if (data.error) {
                    hide(groupsEl);
                    show(resultEl);
                    resultEl.className = 'alert alert-error';
                    resultEl.textContent = 'Error: ' + data.error;
                    return;
                }

                gameData = data;
                renderCheckboxes(data);
                show(groupsEl);
                updateTransferButton();
            })
            .catch(function () {
                hide(loadingEl);
                show(resultEl);
                resultEl.className = 'alert alert-error';
                resultEl.textContent = 'Failed to load game data.';
            });
    }

    function renderCheckboxes(data) {
        renderGroup('tf-worlds', 'tf-group-worlds', data.worlds || [], 'world');
        renderGroup('tf-arenas', 'tf-group-arenas', data.arenas || [], 'arena');
        renderGroup('tf-kits', 'tf-group-kits', data.kits || [], 'kit');
        renderGroup('tf-configs', 'tf-group-configs', (data.configs || []).map(function (n) { return n + '.json'; }), 'config');

        // Mod JAR
        var modHtml = '';
        if (data.has_mod_jar) {
            modHtml = '<label><input type="checkbox" class="tf-check" data-type="mod_jar"> HyArena2-1.0.0.jar</label>';
        }
        var modEl = document.getElementById('tf-mod');
        modEl.innerHTML = modHtml || '<span class="text-muted">None available</span>';
        document.getElementById('tf-group-mod').style.display = modHtml ? '' : 'none';

        // Bind change events
        var checks = document.querySelectorAll('.tf-check');
        for (var i = 0; i < checks.length; i++) {
            checks[i].addEventListener('change', updateTransferButton);
        }
    }

    function renderGroup(containerId, groupId, items, type) {
        var container = document.getElementById(containerId);
        var group = document.getElementById(groupId);

        if (items.length === 0) {
            container.innerHTML = '<span class="text-muted">None available</span>';
            group.style.display = 'none';
            return;
        }

        var html = '';
        for (var i = 0; i < items.length; i++) {
            // For config type, display name is "global.json" but data-name is "global"
            var displayName = items[i];
            var dataName = type === 'config' ? items[i].replace(/\.json$/, '') : items[i];
            html += '<label><input type="checkbox" class="tf-check" data-type="' + esc(type) + '" data-name="' + esc(dataName) + '"> ' + esc(displayName) + '</label>';
        }
        container.innerHTML = html;
        group.style.display = '';
    }

    // Select All toggles
    document.addEventListener('change', function (e) {
        if (!e.target.classList.contains('tf-select-all')) return;
        var group = e.target.getAttribute('data-group');
        var checked = e.target.checked;
        var container = document.getElementById('tf-' + group);
        if (!container) return;
        var boxes = container.querySelectorAll('.tf-check');
        for (var i = 0; i < boxes.length; i++) {
            boxes[i].checked = checked;
        }
        updateTransferButton();
    });

    function updateTransferButton() {
        var btn = document.getElementById('tf-btn');
        var hasSource = sourceSelect.value !== '';
        var hasTarget = targetSelect.value !== '';
        var hasItems = document.querySelectorAll('.tf-check:checked').length > 0;
        btn.disabled = !(hasSource && hasTarget && hasItems);
    }

    targetSelect.addEventListener('change', updateTransferButton);

    // Execute transfer
    window.tfExecute = function () {
        var source = sourceSelect.value;
        var target = targetSelect.value;

        if (!source || !target) return;
        if (source === target) {
            alert('Source and target server must be different.');
            return;
        }

        var checked = document.querySelectorAll('.tf-check:checked');
        if (checked.length === 0) return;

        var worlds = [];
        var arenas = [];
        var kits = [];
        var configs = [];
        var modJar = false;

        for (var i = 0; i < checked.length; i++) {
            var cb = checked[i];
            var cbType = cb.getAttribute('data-type');
            var name = cb.getAttribute('data-name');
            if (cbType === 'world') worlds.push(name);
            else if (cbType === 'arena') arenas.push(name);
            else if (cbType === 'kit') kits.push(name);
            else if (cbType === 'config') configs.push(name);
            else if (cbType === 'mod_jar') modJar = true;
        }

        var sourceName = sourceSelect.options[sourceSelect.selectedIndex].getAttribute('data-name');
        var targetName = targetSelect.options[targetSelect.selectedIndex].getAttribute('data-name');

        var summary = [];
        if (worlds.length > 0) summary.push(worlds.length + ' world(s)');
        if (arenas.length > 0) summary.push(arenas.length + ' arena config(s)');
        if (kits.length > 0) summary.push(kits.length + ' kit config(s)');
        if (configs.length > 0) summary.push(configs.length + ' config file(s)');
        if (modJar) summary.push('mod JAR');

        if (!confirm('Transfer ' + summary.join(', ') + ' from "' + sourceName + '" to "' + targetName + '"?\n\nExisting data on the target will be backed up.')) {
            return;
        }

        var btn = document.getElementById('tf-btn');
        var spinner = document.getElementById('tf-spinner');
        var resultEl = document.getElementById('tf-result');

        btn.disabled = true;
        spinner.style.display = 'inline';
        hide(resultEl);

        var payload = {
            source_server: source,
            target_server: target,
            worlds: worlds,
            arenas: arenas,
            kits: kits,
            configs: configs,
            mod_jar: modJar
        };

        fetch('/admin/api/transfer/execute', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': window.HW_CSRF_TOKEN
            },
            body: JSON.stringify(payload)
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                spinner.style.display = 'none';
                show(resultEl);

                if (data.error) {
                    resultEl.className = 'alert alert-error';
                    resultEl.textContent = 'Transfer failed: ' + data.error;
                    updateTransferButton();
                    return;
                }

                var html = '<strong>Transfer complete!</strong><br>';
                var t = data.transferred || {};
                var items = [];
                if (t.worlds && t.worlds.length > 0) items.push('Worlds: ' + t.worlds.join(', '));
                if (t.arenas && t.arenas.length > 0) items.push('Arenas: ' + t.arenas.join(', '));
                if (t.kits && t.kits.length > 0) items.push('Kits: ' + t.kits.join(', '));
                if (t.configs && t.configs.length > 0) items.push('Configs: ' + t.configs.map(function (n) { return n + '.json'; }).join(', '));
                if (t.mod_jar) items.push('Mod JAR');
                html += 'Transferred: ' + (items.length > 0 ? items.join('; ') : 'none');

                var backed = data.backed_up || [];
                if (backed.length > 0) {
                    html += '<br>Backups created: ' + backed.map(esc).join(', ');
                }

                resultEl.className = 'alert alert-success';
                resultEl.innerHTML = html;
                updateTransferButton();

                // Refresh backups if the target server is selected in backup section
                if (backupSelect.value === target) {
                    loadBackups(target);
                }
            })
            .catch(function () {
                spinner.style.display = 'none';
                show(resultEl);
                resultEl.className = 'alert alert-error';
                resultEl.textContent = 'Transfer request failed.';
                updateTransferButton();
            });
    };

    // ==========================================
    // Backup Section — Timestamp-Grouped Tree
    // ==========================================

    backupSelect.addEventListener('change', function () {
        var id = backupSelect.value;
        var resultEl = document.getElementById('bk-result');
        hide(resultEl);

        if (!id) {
            document.getElementById('bk-tree').innerHTML = '<p class="text-muted">Select a server to view backups.</p>';
            return;
        }
        loadBackups(id);
    });

    function loadBackups(serverId) {
        var tree = document.getElementById('bk-tree');
        tree.innerHTML = '<p class="text-muted">Loading...</p>';

        fetch('/admin/api/transfer/backups/' + encodeURIComponent(serverId))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.error) {
                    tree.innerHTML = '<p class="text-muted">Error: ' + esc(data.error) + '</p>';
                    return;
                }

                var backups = data.backups || [];
                if (backups.length === 0) {
                    tree.innerHTML = '<p class="text-muted">No backups found.</p>';
                    return;
                }

                // Group by timestamp, most recent first
                var groups = {};
                var order = [];
                for (var i = 0; i < backups.length; i++) {
                    var ts = backups[i].timestamp;
                    if (!groups[ts]) {
                        groups[ts] = [];
                        order.push(ts);
                    }
                    groups[ts].push(backups[i]);
                }
                order.sort(function (a, b) { return b.localeCompare(a); });

                renderTree(tree, groups, order);
            })
            .catch(function () {
                tree.innerHTML = '<p class="text-muted">Failed to load backups.</p>';
            });
    }

    function formatTimestamp(ts) {
        // "2026-02-24_14-30" → "Feb 24, 2026 at 14:30"
        var parts = ts.split('_');
        if (parts.length !== 2) return ts;
        var dateParts = parts[0].split('-');
        var timeParts = parts[1].split('-');
        if (dateParts.length !== 3 || timeParts.length !== 2) return ts;
        var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        var m = parseInt(dateParts[1], 10);
        var month = (m >= 1 && m <= 12) ? months[m - 1] : dateParts[1];
        return month + ' ' + parseInt(dateParts[2], 10) + ', ' + dateParts[0] + ' at ' + timeParts[0] + ':' + timeParts[1];
    }

    function renderTree(container, groups, order) {
        var html = '';
        for (var g = 0; g < order.length; g++) {
            var ts = order[g];
            var items = groups[ts];
            var expanded = g === 0; // most recent expanded by default
            var groupId = 'bk-group-' + g;

            html += '<div class="bk-tree-group">';
            html += '<div class="bk-tree-header" data-target="' + groupId + '">';
            html += '<span class="bk-tree-toggle">' + (expanded ? '&#9660;' : '&#9654;') + '</span>';
            html += '<span class="bk-tree-ts">' + esc(formatTimestamp(ts)) + '</span>';
            html += '<span class="bk-tree-count">' + items.length + ' item' + (items.length !== 1 ? 's' : '') + '</span>';
            html += '<span class="bk-tree-actions">';
            html += '<button class="btn btn-sm btn-primary bk-batch-btn" data-action="restore" data-ts="' + esc(ts) + '">Restore All</button> ';
            html += '<button class="btn btn-sm btn-danger bk-batch-btn" data-action="delete" data-ts="' + esc(ts) + '">Delete All</button>';
            html += '</span>';
            html += '</div>';
            html += '<div class="bk-tree-items" id="' + groupId + '"' + (expanded ? '' : ' style="display:none"') + '>';

            for (var i = 0; i < items.length; i++) {
                var b = items[i];
                var displayName = b.original;
                if (b.type === 'config') displayName += '.json';
                html += '<div class="bk-tree-item">';
                html += '<span class="badge badge-info">' + esc(typeLabel(b.type)) + '</span>';
                html += '<span class="bk-tree-name">' + esc(displayName) + '</span>';
                html += '<span class="bk-tree-item-actions">';
                html += '<button class="btn btn-sm btn-primary bk-single-btn" data-action="restore" data-type="' + esc(b.type) + '" data-name="' + esc(b.name) + '">Restore</button> ';
                html += '<button class="btn btn-sm btn-danger bk-single-btn" data-action="delete" data-type="' + esc(b.type) + '" data-name="' + esc(b.name) + '">Delete</button>';
                html += '</span>';
                html += '</div>';
            }

            html += '</div>';
            html += '</div>';
        }
        container.innerHTML = html;
    }

    // Toggle collapse/expand
    document.addEventListener('click', function (e) {
        var header = e.target.closest('.bk-tree-header');
        if (!header) return;
        // Ignore clicks on buttons
        if (e.target.closest('.bk-batch-btn')) return;

        var targetId = header.getAttribute('data-target');
        var items = document.getElementById(targetId);
        if (!items) return;

        var toggle = header.querySelector('.bk-tree-toggle');
        if (items.style.display === 'none') {
            items.style.display = '';
            toggle.innerHTML = '&#9660;';
        } else {
            items.style.display = 'none';
            toggle.innerHTML = '&#9654;';
        }
    });

    // Single item restore/delete
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('.bk-single-btn');
        if (!btn) return;

        var serverId = backupSelect.value;
        if (!serverId) return;

        var action = btn.getAttribute('data-action');
        var type = btn.getAttribute('data-type');
        var backupName = btn.getAttribute('data-name');
        var actionLabel = action === 'restore' ? 'Restore' : 'Delete';

        if (!confirm(actionLabel + ' backup "' + backupName + '"?')) return;

        var resultEl = document.getElementById('bk-result');
        hide(resultEl);

        bkRequest(serverId, action, type, backupName)
            .then(function (data) {
                show(resultEl);
                if (data.error) {
                    resultEl.className = 'alert alert-error';
                    resultEl.textContent = actionLabel + ' failed: ' + data.error;
                } else {
                    resultEl.className = 'alert alert-success';
                    resultEl.textContent = data.message || (actionLabel + ' successful.');
                }
                loadBackups(serverId);
            })
            .catch(function () {
                show(resultEl);
                resultEl.className = 'alert alert-error';
                resultEl.textContent = actionLabel + ' request failed.';
            });
    });

    // Batch restore/delete all items in a timestamp group
    document.addEventListener('click', function (e) {
        var btn = e.target.closest('.bk-batch-btn');
        if (!btn) return;

        var serverId = backupSelect.value;
        if (!serverId) return;

        var action = btn.getAttribute('data-action');
        var ts = btn.getAttribute('data-ts');
        var actionLabel = action === 'restore' ? 'Restore' : 'Delete';

        // Gather all items in this group from the DOM
        var header = btn.closest('.bk-tree-header');
        var targetId = header.getAttribute('data-target');
        var itemsContainer = document.getElementById(targetId);
        var singleBtns = itemsContainer.querySelectorAll('.bk-single-btn[data-action="' + action + '"]');

        var items = [];
        for (var i = 0; i < singleBtns.length; i++) {
            items.push({
                type: singleBtns[i].getAttribute('data-type'),
                name: singleBtns[i].getAttribute('data-name')
            });
        }

        if (items.length === 0) return;
        if (!confirm(actionLabel + ' all ' + items.length + ' backup(s) from ' + formatTimestamp(ts) + '?')) return;

        var resultEl = document.getElementById('bk-result');
        hide(resultEl);

        // Disable batch buttons and show progress
        var group = btn.closest('.bk-tree-group');
        var allBtns = group.querySelectorAll('.btn');
        for (var j = 0; j < allBtns.length; j++) allBtns[j].disabled = true;

        bkBatchSequential(serverId, action, items, 0, [], [])
            .then(function (result) {
                show(resultEl);
                if (result.errors.length === 0) {
                    resultEl.className = 'alert alert-success';
                    resultEl.textContent = actionLabel + ' completed: ' + result.ok + ' of ' + items.length + ' item(s) processed.';
                } else {
                    resultEl.className = 'alert alert-error';
                    resultEl.textContent = actionLabel + ' partially failed: ' + result.ok + ' succeeded, ' + result.errors.length + ' failed. Errors: ' + result.errors.join('; ');
                }
                loadBackups(serverId);
            });
    });

    function bkRequest(serverId, action, type, backupName) {
        return fetch('/admin/api/transfer/backup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': window.HW_CSRF_TOKEN
            },
            body: JSON.stringify({
                server: serverId,
                action: action,
                type: type,
                backup_name: backupName
            })
        }).then(function (r) { return r.json(); });
    }

    function bkBatchSequential(serverId, action, items, index, errors, okList) {
        if (index >= items.length) {
            return Promise.resolve({ ok: okList.length, errors: errors });
        }
        var item = items[index];
        return bkRequest(serverId, action, item.type, item.name)
            .then(function (data) {
                if (data.error) {
                    errors.push(item.name + ': ' + data.error);
                } else {
                    okList.push(item.name);
                }
                return bkBatchSequential(serverId, action, items, index + 1, errors, okList);
            })
            .catch(function () {
                errors.push(item.name + ': request failed');
                return bkBatchSequential(serverId, action, items, index + 1, errors, okList);
            });
    }
});
