import { Router } from 'express';
import { obtenerVehiculos, crearVehiculo, actualizarVehiculo, eliminarVehiculo } from '../controllers/vehiculos.controller.js';
import { authRequired } from '../middlewares/validateToken.js';
import { isAdmin } from '../middlewares/validateAdmin.js';

const router = Router();

router.get('/', authRequired, obtenerVehiculos);
router.post('/', authRequired, isAdmin, crearVehiculo);
router.put('/:id', authRequired, isAdmin, actualizarVehiculo);
router.delete('/:id', authRequired, isAdmin, eliminarVehiculo);

export default router;
