#!/usr/bin/env python3
import json
import subprocess
from http import HTTPStatus
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


ROOT = Path(__file__).resolve().parent
SERVER_DIR = ROOT / "server"

SCREEN_FILES = {
    "home": ("config.json", "config.rc"),
    "detail": ("config_detail.json", "config_detail.rc"),
    "estimates": ("config_estimates.json", "config_estimates.rc"),
    "estimate_detail": ("config_estimate_detail.json", "config_estimate_detail.rc"),
}


class RemoteComposeHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def do_POST(self):
        if self.path != "/api/deploy":
            self.send_error(HTTPStatus.NOT_FOUND, "Not found")
            return

        length_header = self.headers.get("Content-Length")
        if not length_header:
            self.send_error(HTTPStatus.LENGTH_REQUIRED, "Missing Content-Length")
            return

        try:
            payload = json.loads(self.rfile.read(int(length_header)))
        except (ValueError, json.JSONDecodeError):
            self.send_error(HTTPStatus.BAD_REQUEST, "Invalid JSON body")
            return

        screen = payload.get("screen")
        config = payload.get("config")
        if screen not in SCREEN_FILES or not isinstance(config, dict):
            self.send_error(HTTPStatus.BAD_REQUEST, "Invalid screen or config")
            return

        json_name, rc_name = SCREEN_FILES[screen]
        json_path = ROOT / json_name
        rc_path = ROOT / rc_name

        try:
            json_path.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
            self._build_remote_compose(json_path, rc_path)
        except subprocess.CalledProcessError as exc:
            self._write_json(
                HTTPStatus.INTERNAL_SERVER_ERROR,
                {
                    "ok": False,
                    "message": "Local build failed",
                    "details": exc.stderr.strip() or exc.stdout.strip(),
                },
            )
            return
        except Exception as exc:  # pragma: no cover - defensive fallback
            self._write_json(
                HTTPStatus.INTERNAL_SERVER_ERROR,
                {
                    "ok": False,
                    "message": str(exc),
                },
            )
            return

        self._write_json(
            HTTPStatus.OK,
            {
                "ok": True,
                "screen": screen,
                "jsonFile": json_name,
                "rcFile": rc_name,
                "rcUrl": f"http://192.168.0.105:8000/{rc_name}",
            },
        )

    def _build_remote_compose(self, json_path: Path, rc_path: Path):
        subprocess.run(
            [
                "./gradlew",
                "run",
                f"--args={json_path} {rc_path}",
            ],
            cwd=SERVER_DIR,
            check=True,
            capture_output=True,
            text=True,
        )

    def _write_json(self, status: HTTPStatus, payload: dict):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)


def main():
    server = ThreadingHTTPServer(("0.0.0.0", 8000), RemoteComposeHandler)
    print("Serving Remote Compose locally at http://0.0.0.0:8000")
    server.serve_forever()


if __name__ == "__main__":
    main()
