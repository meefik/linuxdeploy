#!/system/bin/sh
# websocket.sh
# (C) 2016-2018 Anton Skshidlevsky <meefik@gmail.com>, MIT
# The cross platform WebSocket implementation for SH.
# https://github.com/meefik/websocket.sh

[ -n "$WS_SHELL" ] || WS_SHELL="sh"
export LANG="C"

# read pipe as hex without separating and add \x for each byte
split_hex()
{
  local hex code
  while read -n 2 code
  do
    if [ -n "$code" ]
    then
      hex="$hex\x$code"
    fi
  done
  echo -n "$hex"
}

# get arguments, first argument - 2
get_arg()
{
  eval "echo -n \$$1"
}

# check contains a byte 81 (text data flag) or 82 (binary data flag)
is_packet()
{
  echo -n "$1" | grep -q -e $(printf '\x81') -e $(printf '\x82')
}

# read N bytes from pipe and convert to unsigned decimal 1-byte units (space seporated)
read_dec()
{
  dd bs=$1 count=1 2>/dev/null | od -A n -t u1 -w$1
}

# read pipe and convert to websocket frame
# see RFC6455 "Base Framing Protocol" https://tools.ietf.org/html/rfc6455
ws_send()
{
  local data length
  while true
  do
    # Binary frame: 0x82 [length] [data]
    # Max length: 00-7D -> 125; 0000-FFFF -> 65535
    data=$(dd bs=65535 count=1 2>/dev/null)
    length=$(echo -n "$data" | wc -c)
    # exit if received 0 bytes
    [ "$length" -gt 0 ] || break
    if [ "$length" -gt 125 ]
    then
      printf "\x82\x7E$(printf '%04x' ${length} | split_hex)"
    else
      printf "\x82\x$(printf '%02x' ${length})"
    fi
    echo -n "$data"
  done
}

# initialize websocket connection
ws_connect()
{
  local line outkey
  while read line
  do
    if printf "%s" "$line" | grep -q $'^\r$'; then
      outkey=$(printf "%s" "$sec_websocket_key" | tr '\r' '\n')
      outkey="${outkey}258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
      outkey=$(printf "%s" "$outkey" | sha1sum | cut -d ' ' -f 1 | printf $(split_hex) | base64)
      #outkey=$(printf %s "$outkey" |  openssl dgst -binary -sha1 | openssl base64)
      printf "HTTP/1.1 101 Switching Protocols\r\n"
      printf "Upgrade: websocket\r\n"
      printf "Connection: Upgrade\r\n"
      printf "Sec-WebSocket-Accept: %s\r\n" "$outkey"
      printf "\r\n"
      break
    else
      case "$line" in
        Sec-WebSocket-Key*)
          sec_websocket_key=$(get_arg 3 $line)
          ;;
      esac
    fi
  done
}

# main loop
ws_server()
{
  local flag header length byte i
  while IFS= read -n 1 flag
  do
    # each packet starts at byte 81 or 82
    is_packet "$flag" || continue
    # read next 5 bytes:
    # 1 -> length
    # 2-5 -> encoding bytes
    header=$(read_dec 5)
    # get packet length
    let length=$(get_arg 2 $header)-128
    [ "$length" -gt 0 -a "$length" -le 125 ] || continue
    # read packet
    let i=0
    for byte in $(read_dec $length)
    do
      # decoding byte: byte ^ encoding_bytes[i % 4]
      let byte=byte^$(get_arg $(($i % 4 + 3)) $header)
      printf "\x$(printf '%02x' $byte)"
      let i=i+1
    done
  done | $WS_SHELL 2>&1 | ws_send
}

# start
ws_connect &&
ws_server
