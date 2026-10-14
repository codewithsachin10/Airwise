import sys
from pathlib import Path
from urllib.parse import urlsplit

from werkzeug.middleware.proxy_fix import ProxyFix

# Ensure project root is in sys.path
ROOT_DIR = Path(__file__).resolve().parent.parent
if str(ROOT_DIR) not in sys.path:
    sys.path.insert(0, str(ROOT_DIR))

from app import app


class VercelPathFixMiddleware:
    """
    WSGI middleware to ensure PATH_INFO accurately reflects the original
    requested URI when running behind Vercel edge proxy / rewrites.
    """
    def __init__(self, wsgi_app):
        self.wsgi_app = wsgi_app

    def __call__(self, environ, start_response):
        # Vercel supplies original request path in X-Forwarded-Uri or X-Matched-Path
        forwarded_uri = (
            environ.get("HTTP_X_FORWARDED_URI")
            or environ.get("HTTP_X_MATCHED_PATH")
            or environ.get("HTTP_X_ORIGINAL_URI")
        )
        if forwarded_uri:
            path = urlsplit(forwarded_uri).path
            environ["PATH_INFO"] = path
        else:
            path_info = environ.get("PATH_INFO", "")
            for prefix in ("/api/index.py", "/api/index", "/api"):
                if path_info.startswith(prefix):
                    remainder = path_info[len(prefix):]
                    environ["PATH_INFO"] = remainder if remainder.startswith("/") else "/" + remainder.lstrip("/")
                    break

        return self.wsgi_app(environ, start_response)


# Apply ProxyFix for HTTPS and host headers from Vercel edge proxy
app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1, x_host=1, x_port=1, x_prefix=1)
# Apply path fix middleware so Flask routes match the original client URL
app.wsgi_app = VercelPathFixMiddleware(app.wsgi_app)
