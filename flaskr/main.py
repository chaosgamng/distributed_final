import __init__ as init



if __name__ == "__main__":
    app = init.create_app()
    app.run("0.0.0.0", 8000, debug=True)