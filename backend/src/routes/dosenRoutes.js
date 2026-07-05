const express = require('express');
const router = express.Router();
const { authMiddleware, roleMiddleware } = require('../middlewares/authMiddleware');
const {
  getBimbinganList,
  reviewKrs,
  getKelasList,
  inputPresensi,
  inputNilai,
  createKelas,
  getKelasMahasiswa
} = require('../controllers/dosenController');

// All lecturer routes require Dosen role authentication
router.use(authMiddleware);
router.use(roleMiddleware(['Dosen']));

router.get('/bimbingan', getBimbinganList);
router.post('/bimbingan/review', reviewKrs);
router.get('/kelas', getKelasList);
router.post('/kelas', createKelas);
router.get('/kelas/:jadwalId/mahasiswa', getKelasMahasiswa);
router.post('/kelas/:jadwalId/presensi', inputPresensi);
router.post('/kelas/:jadwalId/nilai', inputNilai);

module.exports = router;
