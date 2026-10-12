# AIRWISE Flask Deployment

## Local setup

Requirements: Python 3.13 and pip. From the repository root in PowerShell:

```powershell
py -3.13 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
Copy-Item .env.example .env
python app.py
```

Open `http://localhost:5000`. The default local database is the persistent
SQLite file `instance/airwise.db`; it is not an in-memory database. Demo
seeding is disabled by default. It runs once when `AIRWISE_DEMO_MODE=true` and
the user table is empty.
Before starting in demo mode, set your own values for
`AIRWISE_DEMO_ADMIN_PASSWORD`, `AIRWISE_DEMO_PASSENGER_PASSWORD`,
`AIRWISE_DEMO_OPERATOR_PASSWORD`, and `AIRWISE_DEMO_PRIYA_PASSWORD` in the
ignored `.env` file. Demo usernames are `admin`, `passenger`, `operator`, and
`priya`; no password defaults are provided. Do not expose demo mode publicly.

For an existing MySQL database, set one of these configurations in the
environment or an ignored `.env` file:

```text
DATABASE_URL=mysql+pymysql://<user>:<url-encoded-password>@<host>:3306/airwise?charset=utf8mb4
```

or use the original Java-style variables `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD`. The application maps the original table and column names. It
creates missing tables but does not alter existing schema or migrate data;
back up and verify the target database before connecting.

Run tests with:

```powershell
py -3.13 -m unittest discover -s tests -v
```

## Render deployment

The repository includes `render.yaml`. Render's native managed database is
PostgreSQL, while this project preserves its original MySQL schema. Use a
reachable persistent MySQL-compatible database and pass its connection URL as
`DATABASE_URL`; do not use Render's ephemeral filesystem as the production
database. Review provider pricing, TLS requirements, and network restrictions.

1. Push the repository to GitHub.
2. In Render, choose **New → Blueprint** and select the GitHub repository, or
   create a **Web Service** from that repository.
3. Use the Python runtime, build command `pip install -r requirements.txt`,
   and start command `gunicorn app:app --bind 0.0.0.0:$PORT`.
4. Configure `DATABASE_URL` with the MySQL-compatible database URL. Set
   `AIRWISE_DEMO_MODE=false`. Generate a unique `SECRET_KEY` in Render's
   environment settings; do not commit secrets to GitHub.
5. Deploy and check `/health` and `/`. Register a passenger and test the public
   flight pages. Confirm the database persists bookings and reports across a
   service restart.
6. Provision an administrator securely. If the Render service shell is
   available, run `flask --app app create-admin` and enter a long password at
   the hidden prompt. Alternatively run that command from a trusted machine
   configured with the same `DATABASE_URL`. Never put the password in the
   command line or source control.

The default Blueprint disables demo seeding. New production databases therefore
start without sample operational records; load authorized airport data through
an approved process before relying on analytics.

## Docker

```powershell
docker build -t airwise .
docker run --rm -p 10000:10000 -e SECRET_KEY="<secret>" -e DATABASE_URL="<persistent-mysql-url>" -e AIRWISE_DEMO_MODE=false airwise
```

## Limitations and verification notes

- Flights, weather, passenger flow, and taxi data are demonstrations; there are
  no live airline, airport, or weather integrations.
- JavaFX medical incidents and complaint examples remain generated simulated
  records in the migrated console and reset when the Python process restarts.
- Complaint attachments preserve the original filename metadata only; uploaded
  file contents are not durably stored. Add managed object storage before using
  attachments in production.
- Existing MySQL rows remain addressable because table and column names are
  mapped to the Java schema. Verify representative existing records, indexes,
  timezones, BCrypt login, and provider-specific SQL behavior against a copy of
  the actual database before switching production traffic.
- The legacy Java sources remain in the repository for comparison; `python
  app.py` and `gunicorn app:app` run the Flask application.
