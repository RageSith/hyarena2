document.addEventListener('DOMContentLoaded', function () {
    var metricsInterval = setInterval(refreshMetrics, 5000);
    var serversInterval = setInterval(refreshServers, 5000);
    var consoleInterval = null;
    var consoleServerId = null;
    var consoleSeq = 0;

    function refreshMetrics() {
        fetch('/admin/api/hywarden/metrics')
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d.error) return;
                var cpu = document.getElementById('hw-cpu');
                var mem = document.getElementById('hw-mem');
                var disk = document.getElementById('hw-disk');
                var host = document.getElementById('hw-host');
                if (cpu) cpu.textContent = d.cpu_percent !== undefined ? d.cpu_percent.toFixed(1) + '%' : '--';
                if (mem) mem.textContent = d.mem_percent !== undefined ? d.mem_percent.toFixed(1) + '%' : '--';
                if (disk) disk.textContent = d.disk_percent !== undefined ? d.disk_percent.toFixed(1) + '%' : '--';
                if (host) host.textContent = d.hostname || '--';
            })
            .catch(function () {});
    }

    function refreshServers() {
        fetch('/admin/api/hywarden/servers')
            .then(function (r) { return r.json(); })
            .then(function (servers) {
                if (servers.error || !Array.isArray(servers)) return;
                var tbody = document.getElementById('hw-servers');
                if (!tbody) return;

                if (servers.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="9" class="empty">No servers configured.</td></tr>';
                    return;
                }

                var html = '';
                for (var i = 0; i < servers.length; i++) {
                    var s = servers[i];
                    html += '<tr>';
                    html += '<td>' + esc(s.name) + '</td>';

                    // Status
                    html += '<td>';
                    if (s.crashed) {
                        html += '<span class="badge badge-hw-crashed">Crashed</span>';
                    } else if (s.running) {
                        html += '<span class="badge badge-hw-running">Running</span>';
                    } else {
                        html += '<span class="badge badge-hw-stopped">Stopped</span>';
                    }
                    if (s.update_available) {
                        html += ' <span class="badge badge-warning">Update</span>';
                    }
                    html += '</td>';

                    html += '<td>' + esc(s.patchline || '-') + '</td>';
                    html += '<td>' + esc(s.version || '-') + '</td>';
                    html += '<td>' + (s.running ? s.cpu_percent.toFixed(1) + '%' : '-') + '</td>';
                    html += '<td>' + (s.running ? s.memory_mb + ' MB' : '-') + '</td>';
                    html += '<td>' + (s.players != null ? s.players : 0) + '</td>';
                    html += '<td>' + (s.running ? formatUptime(s.uptime) : '-') + '</td>';

                    // Actions
                    html += '<td class="actions">';
                    if (s.running) {
                        html += '<button class="btn btn-sm btn-danger" onclick="hwAction(\'stop\', \'' + esc(s.id) + '\')">Stop</button> ';
                        html += '<button class="btn btn-sm btn-danger" onclick="hwAction(\'kill\', \'' + esc(s.id) + '\')">Kill</button> ';
                    } else {
                        html += '<button class="btn btn-sm btn-primary" onclick="hwAction(\'start\', \'' + esc(s.id) + '\')">Start</button> ';
                    }
                    html += '<button class="btn btn-sm btn-secondary" onclick="hwOpenConsole(\'' + esc(s.id) + '\', \'' + esc(s.name) + '\')">Console</button> ';
                    html += '<button class="btn btn-sm btn-secondary" onclick="hwOpenPrefabs(\'' + esc(s.id) + '\', \'' + esc(s.name) + '\')">Prefabs</button>';
                    html += '</td></tr>';
                }
                tbody.innerHTML = html;
            })
            .catch(function () {});
    }

    function formatUptime(seconds) {
        if (!seconds || seconds <= 0) return '-';
        var h = Math.floor(seconds / 3600);
        var m = Math.floor((seconds % 3600) / 60);
        var s = seconds % 60;
        if (h > 0) return h + 'h ' + m + 'm';
        if (m > 0) return m + 'm ' + s + 's';
        return s + 's';
    }

    function esc(str) {
        if (str == null) return '';
        var d = document.createElement('div');
        d.textContent = String(str);
        return d.innerHTML;
    }

    // Reauth
    window.hwReauth = function () {
        var form = new URLSearchParams();
        form.append('_csrf_token', window.HW_CSRF_TOKEN);

        fetch('/admin/api/hywarden/reauth', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: form.toString(),
        })
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d.error) {
                    alert('Reauth failed: ' + d.error);
                } else {
                    refreshMetrics();
                    refreshServers();
                }
            })
            .catch(function () { alert('Reauth request failed.'); });
    };

    // Server actions (start/stop/kill)
    window.hwAction = function (action, id) {
        if (action === 'kill' && !confirm('Kill this server process? This will not save data.')) {
            return;
        }
        var form = new URLSearchParams();
        form.append('_csrf_token', window.HW_CSRF_TOKEN);

        fetch('/admin/api/hywarden/servers/' + encodeURIComponent(id) + '/' + action, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: form.toString(),
        })
            .then(function (r) { return r.json(); })
            .then(function () { refreshServers(); })
            .catch(function () {});
    };

    // Console overlay
    window.hwOpenConsole = function (id, name) {
        consoleServerId = id;
        consoleSeq = 0;
        document.getElementById('hw-console-title').textContent = 'Console — ' + name;
        document.getElementById('hw-console-body').innerHTML = '';
        document.getElementById('hw-console-overlay').classList.add('show');
        fetchConsole(true);
        consoleInterval = setInterval(function () { fetchConsole(false); }, 2000);
    };

    window.hwCloseConsole = function () {
        document.getElementById('hw-console-overlay').classList.remove('show');
        consoleServerId = null;
        if (consoleInterval) {
            clearInterval(consoleInterval);
            consoleInterval = null;
        }
    };

    function fetchConsole(initial) {
        if (!consoleServerId) return;
        var url = '/admin/api/hywarden/servers/' + encodeURIComponent(consoleServerId) + '/console';
        if (!initial && consoleSeq > 0) {
            url += '?offset=' + consoleSeq;
        }

        fetch(url)
            .then(function (r) { return r.json(); })
            .then(function (d) {
                if (d.error || !d.lines) return;
                var body = document.getElementById('hw-console-body');
                if (!body) return;

                if (initial) {
                    body.innerHTML = '';
                }

                var lines = d.lines;
                for (var i = 0; i < lines.length; i++) {
                    var div = document.createElement('div');
                    div.className = 'hw-console-line';
                    div.textContent = lines[i];
                    body.appendChild(div);
                }

                if (d.seq !== undefined) {
                    consoleSeq = d.seq;
                } else if (d.total !== undefined) {
                    consoleSeq = d.total;
                }

                if (lines.length > 0) {
                    body.scrollTop = body.scrollHeight;
                }
            })
            .catch(function () {});
    }

    // Prefab overlay
    var prefabServerId = null;
    var prefabServerName = null;

    window.hwOpenPrefabs = function (id, name) {
        prefabServerId = id;
        prefabServerName = name;
        document.getElementById('hw-prefab-title').textContent = 'Prefabs — ' + name;
        document.getElementById('hw-prefab-file').value = '';
        hidePrefabMessage();
        document.getElementById('hw-prefab-overlay').classList.add('show');
        hwRefreshPrefabs();
    };

    window.hwClosePrefabs = function () {
        document.getElementById('hw-prefab-overlay').classList.remove('show');
        prefabServerId = null;
    };

    window.hwRefreshPrefabs = function () {
        if (!prefabServerId) return;
        var tbody = document.getElementById('hw-prefab-list');
        tbody.innerHTML = '<tr><td colspan="4" class="empty">Loading...</td></tr>';

        fetch('/admin/api/hywarden/servers/' + encodeURIComponent(prefabServerId) + '/prefabs')
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.error) {
                    if (data.error.indexOf('not found') !== -1 || data.error.indexOf('does not exist') !== -1 || data.error.indexOf('no such') !== -1) {
                        tbody.innerHTML = '<tr><td colspan="4" class="empty">No prefab folder found on this server.</td></tr>';
                    } else {
                        tbody.innerHTML = '<tr><td colspan="4" class="empty">Error: ' + esc(data.error) + '</td></tr>';
                    }
                    return;
                }

                var files = Array.isArray(data) ? data : (data.files || []);
                if (files.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="4" class="empty">No prefab files.</td></tr>';
                    return;
                }

                var html = '';
                for (var i = 0; i < files.length; i++) {
                    var f = files[i];
                    var fname = f.name || f.Name || '';
                    var fsize = f.size || f.Size || 0;
                    var fmod = f.modified || f.Modified || f.mod_time || '';
                    html += '<tr>';
                    html += '<td>' + esc(fname) + '</td>';
                    html += '<td>' + formatFileSize(fsize) + '</td>';
                    html += '<td>' + formatDate(fmod) + '</td>';
                    html += '<td class="actions">';
                    html += '<a class="btn btn-sm btn-secondary" href="/admin/api/hywarden/servers/' + encodeURIComponent(prefabServerId) + '/prefabs/' + encodeURIComponent(fname) + '/download">Download</a> ';
                    html += '<button class="btn btn-sm btn-danger" onclick="hwDeletePrefab(\'' + esc(fname) + '\')">Delete</button>';
                    html += '</td></tr>';
                }
                tbody.innerHTML = html;
            })
            .catch(function () {
                tbody.innerHTML = '<tr><td colspan="4" class="empty">Failed to load prefab list.</td></tr>';
            });
    };

    window.hwUploadPrefab = function () {
        if (!prefabServerId) return;
        var input = document.getElementById('hw-prefab-file');
        if (!input.files || input.files.length === 0) {
            showPrefabMessage('Please select a file.', true);
            return;
        }

        var file = input.files[0];
        if (!file.name.toLowerCase().endsWith('.prefab.json')) {
            showPrefabMessage('File must end with .prefab.json', true);
            return;
        }

        var form = new FormData();
        form.append('file', file);
        form.append('_csrf_token', window.HW_CSRF_TOKEN);

        showPrefabMessage('Uploading...', false);

        fetch('/admin/api/hywarden/servers/' + encodeURIComponent(prefabServerId) + '/prefabs/upload', {
            method: 'POST',
            body: form,
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.error) {
                    showPrefabMessage('Upload failed: ' + data.error, true);
                } else {
                    showPrefabMessage('Uploaded successfully.', false);
                    input.value = '';
                    hwRefreshPrefabs();
                }
            })
            .catch(function () {
                showPrefabMessage('Upload failed.', true);
            });
    };

    window.hwDeletePrefab = function (name) {
        if (!prefabServerId) return;
        if (!confirm('Delete "' + name + '"? This cannot be undone.')) return;

        var form = new URLSearchParams();
        form.append('_csrf_token', window.HW_CSRF_TOKEN);

        fetch('/admin/api/hywarden/servers/' + encodeURIComponent(prefabServerId) + '/prefabs/' + encodeURIComponent(name) + '/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: form.toString(),
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.error) {
                    showPrefabMessage('Delete failed: ' + data.error, true);
                } else {
                    showPrefabMessage('Deleted.', false);
                    hwRefreshPrefabs();
                }
            })
            .catch(function () {
                showPrefabMessage('Delete failed.', true);
            });
    };

    function showPrefabMessage(text, isError) {
        var el = document.getElementById('hw-prefab-message');
        el.textContent = text;
        el.className = 'hw-prefab-message' + (isError ? ' hw-prefab-message-error' : ' hw-prefab-message-success');
        el.style.display = 'block';
    }

    function hidePrefabMessage() {
        var el = document.getElementById('hw-prefab-message');
        el.style.display = 'none';
    }

    function formatFileSize(bytes) {
        if (!bytes || bytes <= 0) return '0 B';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    function formatDate(dateStr) {
        if (!dateStr) return '-';
        try {
            var d = new Date(dateStr);
            if (isNaN(d.getTime())) return esc(String(dateStr));
            return d.toLocaleString();
        } catch (e) {
            return esc(String(dateStr));
        }
    }

    // Close overlays on Escape
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            if (document.getElementById('hw-prefab-overlay').classList.contains('show')) {
                hwClosePrefabs();
            } else if (document.getElementById('hw-console-overlay').classList.contains('show')) {
                hwCloseConsole();
            }
        }
    });
});
