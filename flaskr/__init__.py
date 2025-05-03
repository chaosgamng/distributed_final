import os
from flask import Flask
import db, views


def create_app():
    # create and configure the app
    app = Flask(__name__)
    app.config.from_mapping(
        SECRET_KEY='dev',
        DATABASE=os.path.join(app.instance_path, 'flaskr.sqlite'),
    )
    app.register_blueprint(views.bp)

    db.init_app(app)

    return app

