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
    // Backup Section
    // ==========================================

    backupSelect.addEventListener('change', function () {
        var id = backupSelect.value;
        var resultEl = document.getElementById('bk-result');
        hide(resultEl);

        if (!id) {
            document.getElementById('bk-list').innerHTML = '<tr><td colspan="4" class="empty">Select a server to view backups.</td></tr>';
            return;
        }
        loadBackups(id);
    });

    function loadBackups(serverId) {
        var tbody = document.getElementById('bk-list');
        tbody.innerHTML = '<tr><td colspan="4" class="empty">Loading...</td></tr>';

        fetch('/admin/api/transfer/backups/' + encodeURIComponent(serverId))
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.error) {
                    tbody.innerHTML = '<tr><td colspan="4" class="empty">Error: ' + esc(data.error) + '</td></tr>';
                    return;
                }

                var backups = data.backups || [];
                if (backups.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="4" class="empty">No backups found.</td></tr>';
                    return;
                }

                var html = '';
                for (var i = 0; i < backups.length; i++) {
                    var b = backups[i];
                    html += '<tr>';
                    html += '<td><span class="badge badge-info">' + esc(typeLabel(b.type)) + '</span></td>';
                    html += '<td>' + esc(b.original) + '</td>';
                    html += '<td>' + esc(b.timestamp) + '</td>';
                    html += '<td class="actions">';
                    html += '<button class="btn btn-sm btn-primary" onclick="bkAction(\'restore\', \'' + esc(b.type) + '\', \'' + esc(b.name) + '\')">Restore</button> ';
                    html += '<button class="btn btn-sm btn-danger" onclick="bkAction(\'delete\', \'' + esc(b.type) + '\', \'' + esc(b.name) + '\')">Delete</button>';
                    html += '</td></tr>';
                }
                tbody.innerHTML = html;
            })
            .catch(function () {
                tbody.innerHTML = '<tr><td colspan="4" class="empty">Failed to load backups.</td></tr>';
            });
    }

    window.bkAction = function (action, type, backupName) {
        var serverId = backupSelect.value;
        if (!serverId) return;

        var actionLabel = action === 'restore' ? 'Restore' : 'Delete';
        if (!confirm(actionLabel + ' backup "' + backupName + '"?')) return;

        var resultEl = document.getElementById('bk-result');
        hide(resultEl);

        var payload = {
            server: serverId,
            action: action,
            type: type,
            backup_name: backupName
        };

        fetch('/admin/api/transfer/backup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-Token': window.HW_CSRF_TOKEN
            },
            body: JSON.stringify(payload)
        })
            .then(function (r) { return r.json(); })
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
    };
});
