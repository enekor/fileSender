import os
import socket
import struct
import sys
import threading
import time

HOST = "0.0.0.0"
UDP_PORT = 47821
TCP_PORT = 47822
PROBE = b"FILESENDER_DISCOVER_V1"
REPLY = b"FILESENDER_DEVICE_V1|android-test-device|Android Tablet|47822"
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fake_received")
os.makedirs(OUT, exist_ok=True)


def udp_server():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((HOST, UDP_PORT))
    print("UDP listening", flush=True)
    while True:
        data, addr = sock.recvfrom(2048)
        if data == PROBE:
            sock.sendto(REPLY, addr)
            print("Discovery reply to", addr, flush=True)


def read_exact(stream, count):
    data = b""
    while len(data) < count:
        chunk = stream.recv(count - len(data))
        if not chunk:
            raise EOFError
        data += chunk
    return data


def read_utf(stream):
    length = struct.unpack(">H", read_exact(stream, 2))[0]
    return read_exact(stream, length).decode("utf-8")


def handle_client(conn, addr):
    try:
        conn.settimeout(30)
        magic, version, count = struct.unpack(">iii", read_exact(conn, 12))
        print("Connection", addr, hex(magic), version, "files", count, flush=True)
        if magic != 0x46533102 or version != 1 or count <= 0:
            raise ValueError("bad header")
        metas = []
        for _ in range(count):
            name = read_utf(conn)
            size = struct.unpack(">q", read_exact(conn, 8))[0]
            mime = read_utf(conn)
            metas.append((name, size, mime))
        for name, size, mime in metas:
            safe = os.path.basename(name).replace("\\", "_")
            path = os.path.join(OUT, safe)
            with open(path, "wb") as output:
                if size >= 0:
                    remaining = size
                    while remaining:
                        chunk = conn.recv(min(65536, remaining))
                        if not chunk:
                            raise EOFError
                        output.write(chunk)
                        remaining -= len(chunk)
                else:
                    while True:
                        length = struct.unpack(">i", read_exact(conn, 4))[0]
                        if length == 0:
                            break
                        output.write(read_exact(conn, length))
            print("Saved", path, os.path.getsize(path), flush=True)
        conn.sendall(struct.pack(">i", 0x4F4B))
        print("ACK sent", flush=True)
    except Exception as exc:
        print("Client error:", exc, flush=True)
    finally:
        conn.close()


def tcp_server():
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind((HOST, TCP_PORT))
    sock.listen(5)
    print("TCP listening", flush=True)
    while True:
        conn, addr = sock.accept()
        threading.Thread(target=handle_client, args=(conn, addr), daemon=True).start()


if __name__ == "__main__":
    threading.Thread(target=udp_server, daemon=True).start()
    tcp_server()
