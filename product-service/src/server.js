import express from 'express';
import pg from 'pg';
import grpc from '@grpc/grpc-js';
import protoLoader from '@grpc/proto-loader';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const pool = new pg.Pool({ connectionString: process.env.DATABASE_URL });
const app = express();
app.use(express.json());

async function initialise() {
  await pool.query(`CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY, name TEXT NOT NULL, price NUMERIC(10,2) NOT NULL, stock INT NOT NULL DEFAULT 0
  )`);
  const { rows } = await pool.query('SELECT COUNT(*)::int AS count FROM products');
  if (!rows[0].count) await pool.query("INSERT INTO products(name, price, stock) VALUES ('Demo keyboard', 40, 10)");
}

app.get('/health', (_, res) => res.json({ status: 'UP', service: 'product-service' }));
app.get('/metrics', (_, res) => res.type('text/plain').send('course_service_up{service="product"} 1\n'));
app.get('/products', async (_, res) => res.json(await pool.query('SELECT * FROM products ORDER BY id').then(r => r.rows)));
app.post('/products', async (req, res) => {
  const { name, price, stock } = req.body;
  if (!name || !Number.isFinite(Number(price)) || !Number.isInteger(Number(stock))) return res.status(400).json({ error: 'name, numeric price and integer stock are required' });
  const result = await pool.query('INSERT INTO products(name,price,stock) VALUES ($1,$2,$3) RETURNING *', [name, price, stock]);
  console.log(JSON.stringify({ level: 'info', event: 'product.created', productId: result.rows[0].id }));
  res.status(201).json(result.rows[0]);
});

const here = path.dirname(fileURLToPath(import.meta.url));
const definition = protoLoader.loadSync(path.join(here, '../proto/product.proto'));
const proto = grpc.loadPackageDefinition(definition).product;
const grpcServer = new grpc.Server();
grpcServer.addService(proto.ProductStock.service, {
  CheckStock: async (call, callback) => {
    try {
      const { rows } = await pool.query('SELECT stock FROM products WHERE id=$1', [call.request.product_id]);
      const available = Boolean(rows[0]) && rows[0].stock >= call.request.quantity;
      callback(null, { available, message: available ? 'Stock available' : 'Insufficient stock' });
    } catch (error) { callback(error); }
  }
});

initialise().then(() => {
  app.listen(process.env.PORT || 3001, () => console.log(JSON.stringify({ level: 'info', event: 'product.http.started' })));
  grpcServer.bindAsync('0.0.0.0:50051', grpc.ServerCredentials.createInsecure(), () => grpcServer.start());
}).catch(error => { console.error(error); process.exit(1); });
