/*
 * Implements the attach method, that
 * attaches the terminal to a WebSocket stream.
 *
 * The bidirectional argument indicates, whether the terminal should
 * send data to the socket as well and is true, by default.
 */

(function(attach) {
    if (typeof exports === 'object' && typeof module === 'object') {
        /*
         * CommonJS environment
         */
        module.exports = attach(require('../../src/xterm'));
    } else if (typeof define == 'function') {
        /*
         * Require.js is available
         */
        define(['../../src/xterm'], attach);
    } else {
        /*
         * Plain browser environment
         */
        attach(this.Xterm);
    }
})(function(Xterm) {
    'use strict';

    /**
     * This module provides methods for attaching a terminal to a WebSocket
     * stream.
     *
     * @module xterm/addons/attach/attach
     */
    var exports = {};

    /**
     * Attaches the given terminal to the given socket.
     *
     * @param {Xterm} term - The terminal to be attached to the given socket.
     * @param {WebSocket} socket - The socket to attach the current terminal.
     * @param {boolean} bidirectional - Whether the terminal should send data
     *                                  to the socket as well.
     * @param {boolean} buffered - Whether the rendering of incoming data
     *                             should happen instantly or at a maximum
     *                             frequency of 1 rendering per 10ms.
     */
    exports.attach = function(term, socket, bidirectional, buffered) {
        bidirectional = (typeof bidirectional == 'undefined') ? true : bidirectional;
        term.socket = socket;

        /**
         * Сonverts a single string representing a decimal number to a character
         * note that no checking is performed to ensure that this is just a hex number, eg. no spaces etc
         * dec: string, the dec codepoint to be converted
         *
         * @param {number} n - Char code
         * @returns {String} - Char
         */
        function dec2char(n) {
            var result = '';
            if (n <= 0xFFFF) {
                result += String.fromCharCode(n);
            } else if (n <= 0x10FFFF) {
                n -= 0x10000
                result += String.fromCharCode(0xD800 | (n >> 10)) + String.fromCharCode(0xDC00 | (n & 0x3FF));
            } else { // code point out of range
                // console.log('Code point out of range: ' + n);
            }
            return result;
        }

        /**
         * Encode unicode symbols.
         *
         * @param {String} str - Unicode string
         */
        function encodeUTF8(str) {
            var highsurrogate = 0;
            var suppCP; // decimal code point value for a supp char
            var n = 0;
            var outputString = '';
            for (var i = 0; i < str.length; i++) {
                var cc = str.charCodeAt(i);
                if (cc < 0 || cc > 0xFFFF) { // error
                    // console.log('Error code: ' + cc);
                }
                if (highsurrogate != 0) {
                    if (0xDC00 <= cc && cc <= 0xDFFF) {
                        suppCP = 0x10000 + ((highsurrogate - 0xD800) << 10) + (cc - 0xDC00);
                        outputString += String.fromCharCode(0xF0 | ((suppCP >> 18) & 0x07)) + ' ' + dec2hex2(0x80 | ((suppCP >> 12) & 0x3F)) + ' ' + dec2hex2(0x80 | ((suppCP >> 6) & 0x3F));
                        outputString += String.fromCharCode(0x80 | (suppCP & 0x3F));
                        highsurrogate = 0;
                        continue;
                    } else { // error
                        // console.log('Error code: ' + cc);
                        highsurrogate = 0;
                    }
                }
                if (0xD800 <= cc && cc <= 0xDBFF) { // high surrogate
                    highsurrogate = cc;
                } else {
                    if (cc <= 0x7F) {
                        outputString += String.fromCharCode(cc);
                    } else if (cc <= 0x7FF) {
                        outputString += String.fromCharCode(0xC0 | ((cc >> 6) & 0x1F));
                        outputString += String.fromCharCode(0x80 | (cc & 0x3F));
                    } else if (cc <= 0xFFFF) {
                        outputString += String.fromCharCode(0xE0 | ((cc >> 12) & 0x0F));
                        outputString += String.fromCharCode(0x80 | ((cc >> 6) & 0x3F));
                        outputString += String.fromCharCode(0x80 | (cc & 0x3F));
                    }
                }
            }
            return outputString;
        }

        /**
         * Decode unicode symbols.
         *
         * @param {String} str - Encoded chars sequence
         */
        function decodeUTF8(str) {
            var outputString = "";
            var counter = 0;
            var n = 0;
            var listArray = Array.prototype.map.call(str, function(c) {
                return ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            });
            for (var i = 0; i < listArray.length; i++) {
                var b = parseInt(listArray[i], 16);
                switch (counter) {
                    case 0:
                        if (0 <= b && b <= 0x7F) { // 0xxxxxxx
                            outputString += dec2char(b);
                        } else if (0xC0 <= b && b <= 0xDF) { // 110xxxxx
                            counter = 1;
                            n = b & 0x1F;
                        } else if (0xE0 <= b && b <= 0xEF) { // 1110xxxx
                            counter = 2;
                            n = b & 0xF;
                        } else if (0xF0 <= b && b <= 0xF7) { // 11110xxx
                            counter = 3;
                            n = b & 0x7;
                        } else { // error
                            // console.log('Error code: ' + n);
                        }
                        break;
                    case 1:
                        if (b < 0x80 || b > 0xBF) { // error
                            // console.log('Error code: ' + n);
                        }
                        counter--;
                        outputString += dec2char((n << 6) | (b - 0x80));
                        n = 0;
                        break;
                    case 2:
                    case 3:
                        if (b < 0x80 || b > 0xBF) { // error
                            // console.log('Error code: ' + n);
                        }
                        n = (n << 6) | (b - 0x80);
                        counter--;
                        break;
                }
            }
            return outputString;
        }

        term._flushBuffer = function() {
            term.write(decodeUTF8(term._attachSocketBuffer));
            term._attachSocketBuffer = null;
            clearTimeout(term._attachSocketBufferTimer);
            term._attachSocketBufferTimer = null;
        };

        term._pushToBuffer = function(data) {
            if (term._attachSocketBuffer) {
                term._attachSocketBuffer += data;
            } else {
                term._attachSocketBuffer = data;
                setTimeout(term._flushBuffer, 10);
            }
        };

        term._getMessage = function(ev) {
            var data = atob(ev.data); // decode base64
             // replace \n to \r\n
            data = data.replace(/([^\r])\n/g,
                function(item, saved) {
                    return saved + '\r\n';
                });
            if (buffered) {
                term._pushToBuffer(data);
            } else {
                term.write(decodeUTF8(data));
            }
        };

        term._sendData = function(data) {
            socket.send(btoa(encodeUTF8(data)));
        };

        socket.addEventListener('message', term._getMessage);

        if (bidirectional) {
            term.on('data', term._sendData);
        }

        socket.addEventListener('close', term.detach.bind(term, socket));
        socket.addEventListener('error', term.detach.bind(term, socket));
    };

    /**
     * Detaches the given terminal from the given socket
     *
     * @param {Xterm} term - The terminal to be detached from the given socket.
     * @param {WebSocket} socket - The socket from which to detach the current
     *                             terminal.
     */
    exports.detach = function(term, socket) {
        term.off('data', term._sendData);

        socket = (typeof socket == 'undefined') ? term.socket : socket;

        if (socket) {
            socket.removeEventListener('message', term._getMessage);
        }

        delete term.socket;
    };

    /**
     * Attaches the current terminal to the given socket
     *
     * @param {WebSocket} socket - The socket to attach the current terminal.
     * @param {boolean} bidirectional - Whether the terminal should send data
     *                                  to the socket as well.
     * @param {boolean} buffered - Whether the rendering of incoming data
     *                             should happen instantly or at a maximum
     *                             frequency of 1 rendering per 10ms.
     */
    Xterm.prototype.attach = function(socket, bidirectional, buffered) {
        return exports.attach(this, socket, bidirectional, buffered);
    };

    /**
     * Detaches the current terminal from the given socket.
     *
     * @param {WebSocket} socket - The socket from which to detach the current
     *                             terminal.
     */
    Xterm.prototype.detach = function(socket) {
        return exports.detach(this, socket);
    };

    return exports;
});
