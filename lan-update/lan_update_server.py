import http.server
import socketserver
import os

PORT = 8765
HOST = "0.0.0.0"
DIR = os.path.dirname(os.path.abspath(__file__))


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {
        **http.server.SimpleHTTPRequestHandler.extensions_map,
        ".apk": "application/vnd.android.package-archive",
        ".json": "application/json",
    }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIR, **kwargs)

    def log_message(self, fmt, *args):
        print("[LAN-UPDATE] " + (fmt % args))


if __name__ == "__main__":
    with socketserver.TCPServer((HOST, PORT), Handler) as httpd:
        print(f"冲浪阅读 M2.5 局域网更新服务器已启动： http://{HOST}:{PORT}")
        print(f"根目录：{DIR}")
        print("手机 App 内「检查更新」即可拉取。Ctrl+C 停止。")
        httpd.serve_forever()
