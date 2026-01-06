import jwt from 'jsonwebtoken';
import dotenv from 'dotenv';
dotenv.config();

const TOKEN_SECRET = process.env.TOKEN_SECRET || 'some secret key';

export const authRequired = (req, res, next) => {
    let token = null;

    // 1. Intentar obtener el token del encabezado de autorización
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
        token = authHeader.split(' ')[1];
    } 
    // 2. Si no está en el encabezado, intentar obtenerlo de las cookies
    else if (req.cookies.token) {
        token = req.cookies.token;
    }

    if (!token) {
        return res.status(401).json({ message: "No token, autorización denegada" });
    }

    jwt.verify(token, TOKEN_SECRET, (err, user) => {
        if (err) {
            return res.status(403).json({ message: "Token inválido" });
        }

        req.user = user; // Guarda el payload del token en req.user
        next();
    });
};
