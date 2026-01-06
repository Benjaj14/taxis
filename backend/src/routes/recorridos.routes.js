import express from 'express';
import { obtenerRecorridos, crearRecorrido, cerrarRecorrido } from '../controllers/recorridos.controller.js';
import { authRequired } from '../middlewares/validateToken.js';

const router = express.Router();

router.get('/', authRequired, obtenerRecorridos);
router.post('/', authRequired, crearRecorrido);
router.put('/:id', authRequired, cerrarRecorrido);

export default router;
