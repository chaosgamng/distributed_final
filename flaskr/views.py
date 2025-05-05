import functools
import socket
import codecs
import time
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
    host = '127.0.0.1'
    port = 5000
    ss = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    ss.bind((host, 4999))
    
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((host, port))
    time.sleep(5)
    print(s.sendfile(file))
    s.close()
    ss.listen(0)
    print("listening")
    client, addr = ss.accept()
    data = codecs.decode(client.recv(1024), 'utf-8')
    return render_template('client.html', file_content=data)
