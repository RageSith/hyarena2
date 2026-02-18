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
                    html += '<button class="btn btn-sm btn-secondary" onclick="hwOpenConsole(\'' + esc(s.id) + '\', \'' + esc(s.name) + '\')">Console</button>';
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

    // Close console on Escape
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && document.getElementById('hw-console-overlay').classList.contains('show')) {
            hwCloseConsole();
        }
    });
});
