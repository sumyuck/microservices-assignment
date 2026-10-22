import express from 'express';
import jwt from 'jsonwebtoken';
import { createProxyMiddleware } from 'http-proxy-middleware';

const app = express();
const secret = process.env.JWT_SECRET;
app.use(express.json());
app.get('/health', (_, res) => res.json({ status: 'UP', service: 'api-gateway' }));
app.get('/metrics', (_, res) => res.type('text/plain').send('course_service_up{service="gateway"} 1\n'));
app.post('/auth/demo-token', (req, res) => res.json({ token: jwt.sign({ role: req.body.role || 'SALES' }, secret, { expiresIn: '1h' }) }));
app.use((req, res, next) => {
  if (req.path === '/health' || req.path === '/metrics' || req.path === '/auth/demo-token') return next();
  const token = req.headers.authorization?.replace('Bearer ', '');
  try { req.user = jwt.verify(token, secret); next(); }
  catch { res.status(401).json({ error: 'A valid Bearer JWT is required' }); }
});
// Express removes the mounted prefix before forwarding. Add each service's own
// resource prefix back; Billing is the exception because it already exposes /invoices.
const proxy = (path, target, servicePrefix = path) => app.use(path, createProxyMiddleware({
  target,
  changeOrigin: true,
  pathRewrite: requestPath => `${servicePrefix}${requestPath.startsWith(path) ? requestPath.slice(path.length) : requestPath}` || '/'
}));
proxy('/products', process.env.PRODUCT_URL);
proxy('/customers', process.env.CUSTOMER_URL);
proxy('/orders', process.env.ORDER_URL);
proxy('/billing', process.env.BILLING_URL, '');
proxy('/notifications', process.env.NOTIFICATION_URL);
app.listen(8080, () => console.log(JSON.stringify({ level: 'info', event: 'gateway.started' })));
