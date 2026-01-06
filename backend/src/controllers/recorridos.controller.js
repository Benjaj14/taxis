import pool from '../db.js';

// Obtener recorridos con filtros, paginación y seguridad por rol
export const obtenerRecorridos = async (req, res) => {
  const { idvehiculo, fecha, page = 1, limit = 20 } = req.query;
  const user = req.user;

  const pageNum = parseInt(page);
  const limitNum = parseInt(limit);
  const offset = (pageNum - 1) * limitNum;

  let baseQuery = 'FROM recorridos';
  const countParams = [];
  const conditions = [];
  let paramIndex = 1;

  if (user.role === 'trabajador') {
    if (!user.idvehiculo) {
      return res.json({ recorridos: [], totalRecorridos: 0, paginaActual: pageNum, totalPaginas: 0 });
    }
    conditions.push(`idvehiculo = $${paramIndex++}`);
    countParams.push(user.idvehiculo);
  } else if (user.role === 'admin' && idvehiculo) {
    conditions.push(`idvehiculo = $${paramIndex++}`);
    countParams.push(idvehiculo);
  }

  if (fecha) {
    conditions.push(`CAST(inicio AS DATE) = $${paramIndex++}`);
    countParams.push(fecha);
  }

  if (conditions.length > 0) {
    baseQuery += ' WHERE ' + conditions.join(' AND ');
  }

  try {
    const countQuery = `SELECT COUNT(*) ${baseQuery}`;
    const totalResult = await pool.query(countQuery, countParams);
    const totalRecorridos = parseInt(totalResult.rows[0].count);
    const totalPaginas = Math.ceil(totalRecorridos / limitNum);

    const paginatedQuery = `SELECT * ${baseQuery} ORDER BY idrecorrido DESC LIMIT $${paramIndex++} OFFSET $${paramIndex++}`;
    const paginatedParams = [...countParams, limitNum, offset];
    
    const result = await pool.query(paginatedQuery, paginatedParams);
    
    res.json({
        recorridos: result.rows,
        totalRecorridos,
        paginaActual: pageNum,
        totalPaginas
    });
  } catch (error) {
    console.error('Error al obtener recorridos:', error);
    res.status(500).json({ message: 'Error al obtener recorridos' });
  }
};

// Crear un nuevo recorrido (iniciarlo) con seguridad por rol
export const crearRecorrido = async (req, res) => {
  try {
    const user = req.user;
    let idVehiculoParaRecorrido;

    if (user.role === 'trabajador') {
      idVehiculoParaRecorrido = user.idvehiculo;
      if (!idVehiculoParaRecorrido) {
        return res.status(403).json({ message: 'Acceso denegado: no tienes un vehículo asignado.' });
      }
    } else if (user.role === 'admin') {
      idVehiculoParaRecorrido = req.body.idvehiculo;
      if (!idVehiculoParaRecorrido) {
        return res.status(400).json({ message: 'El idvehiculo es requerido para administradores.' });
      }
    }

    const result = await pool.query(
      'INSERT INTO recorridos (idvehiculo, inicio, estado) VALUES ($1, NOW(), \'iniciado\') RETURNING *',
      [idVehiculoParaRecorrido]
    );
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error('Error al crear recorrido:', error);
    res.status(500).json({ message: 'Error al crear recorrido' });
  }
};

// Cerrar un recorrido (finalizarlo) con seguridad por rol
export const cerrarRecorrido = async (req, res) => {
  try {
    const { id } = req.params;
    const { km } = req.body;
    const user = req.user;

    // 1. Obtener el recorrido y su idvehiculo
    const recorridoResult = await pool.query('SELECT inicio, idvehiculo FROM recorridos WHERE idrecorrido = $1', [id]);
    if (recorridoResult.rows.length === 0) {
      return res.status(404).json({ message: 'Recorrido no encontrado' });
    }
    const recorridoActual = recorridoResult.rows[0];

    // 2. Verificar permisos para el rol 'trabajador'
    if (user.role === 'trabajador') {
      if (recorridoActual.idvehiculo !== user.idvehiculo) {
        return res.status(403).json({ message: 'Acceso denegado: no puedes modificar un recorrido de otro vehículo.' });
      }
    }

    // 3. Calcular duración y actualizar
    const inicio = new Date(recorridoActual.inicio);
    const fin = new Date();
    const duracion = Math.round((fin - inicio) / 60000); // en minutos

    const result = await pool.query(
      'UPDATE recorridos SET fin = $1, km = $2, duracion = $3, estado = $4 WHERE idrecorrido = $5 RETURNING *',
      [fin, km, duracion, 'finalizado', id]
    );

    res.json(result.rows[0]);
  } catch (error) {
    console.error('Error al cerrar recorrido:', error);
    res.status(500).json({ message: 'Error al cerrar recorrido' });
  }
};
