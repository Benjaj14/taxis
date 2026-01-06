import { pool } from '../db.js';

const UsuarioAdmin = {
  async getAll() {
    const result = await pool.query('SELECT * FROM UsuarioAdmin');
    return result.rows;
  },

  async getById(id) {
    const result = await pool.query('SELECT * FROM UsuarioAdmin WHERE IdAdmin = $1', [id]);
    return result.rows[0];
  },

  async getByEmail(email) {
    const result = await pool.query('SELECT * FROM UsuarioAdmin WHERE email = $1', [email]);
    return result.rows[0];
  },

  async create({ email, password }) {
    // IMPORTANTE: En una aplicación real, la contraseña DEBE ser hasheada antes de almacenarla.
    // Este ejemplo la almacena en texto plano por simplicidad, pero es un riesgo de seguridad.
    const result = await pool.query(
      'INSERT INTO UsuarioAdmin (email, password) VALUES ($1, $2) RETURNING *',
      [email, password]
    );
    return result.rows[0];
  },

  async update(id, { email, password }) {
    // IMPORTANTE: En una aplicación real, la contraseña DEBE ser hasheada antes de almacenarla.
    // Este ejemplo la almacena en texto plano por simplicidad, pero es un riesgo de seguridad.
    const result = await pool.query(
      'UPDATE UsuarioAdmin SET email = $1, password = $2 WHERE IdAdmin = $3 RETURNING *',
      [email, password, id]
    );
    return result.rows[0];
  },

  async delete(id) {
    const result = await pool.query('DELETE FROM UsuarioAdmin WHERE IdAdmin = $1 RETURNING *', [id]);
    return result.rows[0];
  }
};

export default UsuarioAdmin;
