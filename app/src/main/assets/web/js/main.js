var term,
    protocol,
    port,
    socketURL,
    socket,
    pty;

createTerminal(document.getElementById('terminal-container'), {
    cursorBlink: true
});

function createTerminal(terminalContainer, optionElements) {
    while (terminalContainer.children.length) {
        terminalContainer.removeChild(terminalContainer.children[0]);
    }
    term = new Terminal(optionElements);
    protocol = (location.protocol === 'https:') ? 'wss://' : 'ws://';
    port = parseInt(location.port) + 1;
    socketURL = protocol + location.hostname + ((port) ? (':' + port) : '');
    socket = new WebSocket(socketURL);

    term.open(terminalContainer);
    term.fit();

    socket.addEventListener('message', function(msg) {
        if (pty) return;
        var data = atob(msg.data);
        var match = data.match(/\/dev\/pts\/\d+/);
        if (match) {
            pty = match[0];
            resizePty(pty, term.rows, term.cols);
        }
    });

    socket.onopen = function() {
        term.attach(socket, true, true);
        term._initialized = true;
    };

    term.on('resize', function(e) {
        resizePty(pty, e.rows, e.cols);
    });

    // Hot key for resize: Ctrl + Alt + r
    window.addEventListener('keydown', function(e) {
        if (e.ctrlKey && e.altKey && e.keyCode == 82) {
            resizePty(pty, term.rows, term.cols, true);
        }
    });

    window.onresize = function() {
        term.fit();
    };
}

function resizePty(pty, rows, cols, refresh) {
    if (!pty) return;
    var xhr = new XMLHttpRequest();
    var uri = 'resize?dev=' + pty + '&rows=' + rows + '&cols=' + cols;
    if (refresh) uri += "&refresh=1";
    xhr.open('GET', uri);
    xhr.send();
}
