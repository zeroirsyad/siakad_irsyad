const jwt = require('jsonwebtoken');
const prisma = require('../config/db');

const authMiddleware = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ message: 'Token tidak valid atau tidak ditemukan' });
    }

    const token = authHeader.split(' ')[1];
    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'siakad_super_secret_jwt_key_2026_antigravity');

    const user = await prisma.user.findUnique({
      where: { id: decoded.id },
      include: {
        mahasiswa: {
          include: {
            dosenPa: true
          }
        },
        dosen: true
      }
    });

    if (!user) {
      return res.status(401).json({ message: 'User tidak ditemukan' });
    }

    req.user = user;
    next();
  } catch (error) {
    return res.status(401).json({ message: 'Token tidak valid atau kedaluwarsa' });
  }
};

const roleMiddleware = (allowedRoles) => {
  return (req, res, next) => {
    if (!req.user || !allowedRoles.includes(req.user.role)) {
      return res.status(403).json({ message: 'Akses ditolak: Peran tidak sah' });
    }
    next();
  };
};

module.exports = { authMiddleware, roleMiddleware };
