"""This is a trivial test script for testing of Python in AppRunner"""
import os
import platform
import flask

app = flask.Flask(__name__)
app.url_map.strict_slashes = False #Tolerate trailing slashes
app_name = os.getenv("APP_NAME", 'python3')

HTML = """
<html>
<head><title>Python 3 in AppRunner - App {APP_NAME}</title></head>
<body>
<h1>Hello World!</h1>
APP_NAME is {APP_NAME}</br>
APP_PORT is {APP_PORT}</br>
APP_DATA is {APP_DATA}</br>
TEMP is {TEMP}</br>
Python version is {PYVER}</br>
</body>
</html>
"""

@app.route('/', methods=['GET'])
@app.route('/' + app_name, methods=['GET'])
def showForm():
    return HTML.format(APP_NAME=os.getenv("APP_NAME", "Unknown"), APP_PORT=os.getenv("APP_PORT", "Unknown"), APP_DATA=os.getenv("APP_DATA", "Unknown"), TEMP=os.getenv("TEMP","Unknown"), PYVER=platform.python_version())

if __name__ == "__main__":
    port = int(os.getenv("APP_PORT", 5050))
    host = os.getenv("APP_HOST", '0.0.0.0')
    #Set debug=False and threaded=True to prevent Flask forking a child process that doesn't get shut down
    app.run(host=host, port=port, debug=False, threaded=True)
