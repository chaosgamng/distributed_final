import functools
import socket
from flask import (
    Blueprint, flash, g, redirect, render_template, request, session, url_for
)
from werkzeug.security import check_password_hash, generate_password_hash

from db import get_db

bp = Blueprint('main', __name__, url_prefix='/')

@bp.route("/", methods=["GET"])
def home():
    return render_template("client.html")

@bp.route("/upload", methods=["POST"])
def upload():
    file = request.files.get("file")
    host = '172.16.4.1'
    port = 8080
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((host, port))
    s.sendfile(file)
    if file and file.filename.endswith('.txt'):
        content = file.read().decode("utf-8")
        return render_template('client.html', file_content=content)
    return render_template('client.html', file_content="Invalid file type. Please upload a .txt file.")
