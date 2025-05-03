import __init__ as init



if __name__ == "__main__":
    app = init.create_app()
    app.run("172.16.4.4", 8000)