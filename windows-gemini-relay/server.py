import getpass, http.server, json, os, secrets, socket, urllib.error, urllib.parse, urllib.request

BASE = os.path.dirname(os.path.abspath(__file__))
CONFIG = os.path.join(BASE, "config.json")
PORT = 8765
LIMIT = 1024 * 1024

def load_config():
    if os.path.exists(CONFIG):
        with open(CONFIG, "r", encoding="utf-8") as f:
            c = json.load(f)
        if c.get("gemini_api_key") and c.get("relay_token"):
            return c
    print("\nהגדרה ראשונית של שרת Gemini")
    key = getpass.getpass("הדבק מפתח Gemini API (הטקסט יוסתר): ").strip()
    if not key:
        raise SystemExit("לא הוזן מפתח API")
    token = secrets.token_urlsafe(24)
    c = {"gemini_api_key": key, "relay_token": token}
    with open(CONFIG, "w", encoding="utf-8") as f:
        json.dump(c, f, ensure_ascii=False, indent=2)
    print("\nקוד הגישה לאפליקציה (שמור בסוד):\n" + token)
    print("המפתח והקוד נשמרו רק בקובץ המקומי config.json")
    return c

def local_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(("8.8.8.8", 80))
        return s.getsockname()[0]
    except OSError:
        return "IP-של-המחשב"
    finally:
        s.close()

class Handler(http.server.BaseHTTPRequestHandler):
    def reply(self, status, data):
        raw = json.dumps(data, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(raw)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(raw)

    def do_GET(self):
        if self.path == "/health":
            self.reply(200, {"ok": True, "service": "Gemini relay"})
        else:
            self.reply(404, {"error": {"message": "Not found"}})

    def do_POST(self):
        if self.path != "/gemini":
            self.reply(404, {"error": {"message": "Unknown endpoint"}})
            return
        cfg = self.server.config
        if not secrets.compare_digest(self.headers.get("X-Relay-Token", ""), cfg["relay_token"]):
            self.reply(401, {"error": {"message": "קוד הגישה לשרת שגוי"}})
            return
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if size < 1 or size > LIMIT:
                self.reply(413, {"error": {"message": "Request size is invalid"}})
                return
            body = self.rfile.read(size)
            json.loads(body.decode("utf-8"))
            model = self.headers.get("X-Gemini-Model", "gemini-3.8-flash").strip()
            if not model or any(ch not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-" for ch in model):
                raise ValueError("שם המודל אינו תקין")
            url = "https://generativelanguage.googleapis.com/v1beta/models/" + urllib.parse.quote(model, safe="._-") + ":generateContent"
            req = urllib.request.Request(url, data=body, method="POST", headers={
                "Content-Type": "application/json",
                "x-goog-api-key": cfg["gemini_api_key"],
                "User-Agent": "WindowsGeminiRelay/1.0"
            })
            try:
                with urllib.request.urlopen(req, timeout=70) as response:
                    result = response.read(LIMIT)
                    self.send_response(response.status)
                    self.send_header("Content-Type", "application/json; charset=utf-8")
                    self.send_header("Content-Length", str(len(result)))
                    self.end_headers()
                    self.wfile.write(result)
            except urllib.error.HTTPError as e:
                result = e.read(LIMIT)
                self.send_response(e.code)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_header("Content-Length", str(len(result)))
                self.end_headers()
                self.wfile.write(result)
        except Exception as e:
            self.reply(502, {"error": {"message": str(e)}})

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.client_address[0], fmt % args))

if __name__ == "__main__":
    cfg = load_config()
    httpd = http.server.ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    httpd.config = cfg
    ip = local_ip()
    print("\nשרת Gemini פועל.")
    print("כתובת לאפליקציה: http://%s:%d/gemini" % (ip, PORT))
    print("בדיקה: http://%s:%d/health" % (ip, PORT))
    print("השאר חלון זה פתוח. לעצירה Ctrl+C.")
    print("השתמש רק ברשת Wi-Fi פרטית ומהימנה.")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nהשרת נעצר.")
    finally:
        httpd.server_close()
