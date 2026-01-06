import express from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import cookieParser from 'cookie-parser';

dotenv.config();

import authRoutes from './routes/auth.routes.js';
import vehiculosRoutes from './routes/vehiculos.routes.js';
import posicionesRoutes from './routes/posiciones.routes.js';
import recorridosRoutes from './routes/recorridos.routes.js'; // ✅

const app = express();
app.use(cors());
app.use(express.json());
app.use(cookieParser());

// Rutas
app.use('/api', authRoutes);
app.use('/vehiculos', vehiculosRoutes);
app.use('/posiciones', posicionesRoutes);
app.use('/recorridos', recorridosRoutes); // ✅

// Ruta de prueba
app.get('/', (req, res) => res.send('API Rodoviario funcionando correctamente'));

export default app;
console.log('Rutas registradas: /api/auth, /vehiculos, /posiciones, /recorridos');
