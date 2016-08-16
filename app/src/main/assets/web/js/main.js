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
        if (pty) resizePty(pty, e.rows, e.cols);
    });

    window.onresize = function() {
        term.fit();
    };
}

function resizePty(pty, rows, cols) {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', 'resize?dev=' + pty + '&rows=' + rows + '&cols=' + cols);
    xhr.send();
}
