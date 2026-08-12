import http from 'http';
import fs from 'fs';
import path from 'path';

const root = 'C:/projects/intensive-care-unit-patient-chart/frontend/dist';
const mime = {
  '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css',
  '.svg': 'image/svg+xml', '.png': 'image/png', '.jpg': 'image/jpeg',
  '.ico': 'image/x-icon', '.json': 'application/json', '.woff2': 'font/woff2',
  '.webmanifest': 'application/manifest+json',
};

http.createServer((req, res) => {
  let p = decodeURIComponent(req.url.split('?')[0]);
  let fp = path.join(root, p === '/' ? 'index.html' : p);
  if (!fs.existsSync(fp) || fs.statSync(fp).isDirectory()) fp = path.join(root, 'index.html');
  res.setHeader('Content-Type', mime[path.extname(fp)] || 'application/octet-stream');
  res.end(fs.readFileSync(fp));
}).listen(5173, () => console.log('SPA server on 5173'));
