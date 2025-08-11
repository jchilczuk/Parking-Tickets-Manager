# jedna zmiana
from dotenv import load_dotenv
import os

load_dotenv()

from app import create_app
from flask import send_file
import threading
import webbrowser
from app.models import db
import time
import psycopg2

def wait_for_db():
    database_url = os.environ.get("DATABASE_URL", "postgresql://user:password@localhost:5432/parkingdb")
    for _ in range(30):
        try:
            conn = psycopg2.connect(database_url) 
            conn.close()
            print("Połączono z bazą danych.")
            return
        except psycopg2.OperationalError:
            print("Czekam na bazę danych...")
            time.sleep(1)
    raise Exception("Nie udało się połączyć z bazą danych.")

app = create_app()

# [ZMIANA] niepotrzebne w sytuacji, kiedy hostujemy frontend osobno
#@app.route("/")
#def serve_index():
#    return send_file(os.path.join(os.getcwd(), "static", "index.html"))

def open_browser():
    webbrowser.open_new("http://localhost:5000/")

if __name__ == "__main__":
    with app.app_context():
        #db.drop_all()
        db.create_all()
        wait_for_db()
        from app.notifications import check_expired_tickets
        check_expired_tickets(app)
    threading.Timer(1.0, open_browser).start()
    app.run(host="0.0.0.0", port=5000)

