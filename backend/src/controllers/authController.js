const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const prisma = require('../config/db');

const login = async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ message: 'Username/NIM/NIDN dan password wajib diisi' });
    }

    const user = await prisma.user.findUnique({
      where: { username },
      include: {
        mahasiswa: true,
        dosen: true
      }
    });

    if (!user) {
      return res.status(401).json({ message: 'Kredensial tidak valid' });
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      return res.status(401).json({ message: 'Kredensial tidak valid' });
    }

    // Generate JWT token
    const token = jwt.sign(
      { id: user.id, username: user.username, role: user.role },
      process.env.JWT_SECRET || 'siakad_super_secret_jwt_key_2026_antigravity',
      { expiresIn: '1d' }
    );

    let profile = null;
    if (user.role === 'Mahasiswa') profile = user.mahasiswa;
    if (user.role === 'Dosen') profile = user.dosen;

    return res.status(200).json({
      message: 'Login berhasil',
      token,
      user: {
        id: user.id,
        username: user.username,
        role: user.role
      },
      profile
    });
  } catch (error) {
    console.error('Error login:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan pada server' });
  }
};

const getProfile = async (req, res) => {
  try {
    const user = req.user;
    return res.status(200).json({
      user: {
        id: user.id,
        username: user.username,
        role: user.role
      },
      profile: user.role === 'Mahasiswa' ? user.mahasiswa : (user.role === 'Dosen' ? user.dosen : null)
    });
  } catch (error) {
    console.error('Error getProfile:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan pada server' });
  }
};

module.exports = { login, getProfile };
