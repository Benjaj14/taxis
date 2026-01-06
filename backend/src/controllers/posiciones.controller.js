import pool from '../db.js';

export const registrarPosicion = async (req, res) => {
  try {
    const { idrecorrido, latitud, longitud, fechahora } = req.body;
    const user = req.user;

    if (!idrecorrido || latitud === undefined || longitud === undefined) {
      return res.status(400).json({ message: 'Los campos idrecorrido, latitud y longitud son requeridos.' });
    }

    // Verificar permisos para el rol 'trabajador'
    if (user.role === 'trabajador') {
      const recorridoResult = await pool.query('SELECT idvehiculo FROM recorridos WHERE idrecorrido = $1', [idrecorrido]);
      if (recorridoResult.rows.length === 0 || recorridoResult.rows[0].idvehiculo !== user.idvehiculo) {
        return res.status(403).json({ message: 'Acceso denegado: no puedes registrar posiciones para este recorrido.' });
      }
    }

    const result = await pool.query(
      'INSERT INTO posicionesgps (idrecorrido, latitud, longitud, fechahora) VALUES ($1, $2, $3, $4) RETURNING *',
      [idrecorrido, latitud, longitud, fechahora || new Date()]
    );
    
    res.status(201).json(result.rows[0]);
  } catch (error) {
    console.error('Error en registrarPosicion:', error);
    res.status(500).json({ error: 'Error al registrar la posición' });
  }
};

export const obtenerPosiciones = async (req, res) => {
  try {
    const user = req.user;
    let result;

    if (user.role === 'admin') {
      result = await pool.query('SELECT * FROM posicionesgps ORDER BY fechahora DESC');
    } else {
      // Un trabajador solo ve las posiciones de los recorridos de su vehículo
      result = await pool.query(
        `SELECT p.* FROM posicionesgps p
         JOIN recorridos r ON p.idrecorrido = r.idrecorrido
         WHERE r.idvehiculo = $1
         ORDER BY p.fechahora DESC`,
        [user.idvehiculo]
      );
    }
    res.json(result.rows);
  } catch (error) {
    console.error('Error en obtenerPosiciones:', error);
    res.status(500).json({ message: 'Error al obtener posiciones' });
  }
};

export const obtenerPosicionesPorRecorrido = async (req, res) => {
  try {
    const { id } = req.params;
    const user = req.user;

    // Verificar permisos para el rol 'trabajador'
    if (user.role === 'trabajador') {
      const recorridoResult = await pool.query('SELECT idvehiculo FROM recorridos WHERE idrecorrido = $1', [id]);
      if (recorridoResult.rows.length === 0 || recorridoResult.rows[0].idvehiculo !== user.idvehiculo) {
        return res.status(403).json({ message: 'Acceso denegado: no puedes ver posiciones de este recorrido.' });
      }
    }

    const result = await pool.query('SELECT latitud, longitud FROM posicionesgps WHERE idrecorrido = $1 ORDER BY fechahora ASC', [id]);
    res.json(result.rows);
  } catch (error) {
    console.error('Error en obtenerPosicionesPorRecorrido:', error);
    res.status(500).json({ message: 'Error al obtener posiciones del recorrido' });
  }
};
