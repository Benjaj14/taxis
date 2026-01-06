import { Router } from 'express';
import { registrarPosicion, obtenerPosiciones, obtenerPosicionesPorRecorrido } from '../controllers/posiciones.controller.js';
import { authRequired } from '../middlewares/validateToken.js';

const router = Router();

router.post('/', authRequired, registrarPosicion);
router.get('/', authRequired, obtenerPosiciones);
router.get('/recorrido/:id', authRequired, obtenerPosicionesPorRecorrido);

export default router;
