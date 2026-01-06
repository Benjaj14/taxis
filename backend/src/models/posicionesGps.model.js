import { pool } from '../db.js';

const PosicionGPS = {
  async getAll() {
    const result = await pool.query('SELECT * FROM PosicionesGPS');
    return result.rows;
  },

  async getById(id) {
    const result = await pool.query('SELECT * FROM PosicionesGPS WHERE IdPosicion = $1', [id]);
    return result.rows[0];
  },

  async getByRecorridoId(recorridoId) {
    const result = await pool.query('SELECT * FROM PosicionesGPS WHERE IdRecorrido = $1 ORDER BY FechaHora ASC', [recorridoId]);
    return result.rows;
  },

  async create({ IdRecorrido, latitud, longitud, FechaHora }) {
    const result = await pool.query(
      'INSERT INTO PosicionesGPS (IdRecorrido, latitud, longitud, FechaHora) VALUES ($1, $2, $3, $4) RETURNING *',
      [IdRecorrido, latitud, longitud, FechaHora]
    );
    return result.rows[0];
  },

  // Las posiciones GPS generalmente no se actualizan, solo se registran.
  // Si fuera necesario, se podría añadir una función de actualización.

  async delete(id) {
    const result = await pool.query('DELETE FROM PosicionesGPS WHERE IdPosicion = $1 RETURNING *', [id]);
    return result.rows[0];
  }
};

export default PosicionGPS;
