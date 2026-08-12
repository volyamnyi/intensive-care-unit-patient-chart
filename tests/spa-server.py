import http.server
import os

os.chdir('C:/projects/intensive-care-unit-patient-chart/frontend/dist')

class SPAHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        p = self.path.split('?')[0]
        if p == '/' or not os.path.exists('.' + p) or os.path.isdir('.' + p):
            self.path = '/index.html'
        return super().do_GET()

    def log_message(self, *args):
        pass

http.server.ThreadingHTTPServer(('0.0.0.0', 5173), SPAHandler).serve_forever()
