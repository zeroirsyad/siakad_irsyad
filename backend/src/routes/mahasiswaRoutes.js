const express = require('express');
const router = express.Router();
const { authMiddleware, roleMiddleware } = require('../middlewares/authMiddleware');
const {
  getDashboard,
  getKrsPage,
  pilihMataKuliah,
  hapusMataKuliah,
  ajukanKrs,
  getKhs
} = require('../controllers/mahasiswaController');

// All mahasiswa routes require Mahasiswa role authentication
router.use(authMiddleware);
router.use(roleMiddleware(['Mahasiswa']));

router.get('/dashboard', getDashboard);
router.get('/krs', getKrsPage);
router.post('/krs/pilih', pilihMataKuliah);
router.delete('/krs/hapus/:jadwalId', hapusMataKuliah);
router.post('/krs/ajukan', ajukanKrs);
router.get('/khs', getKhs);

module.exports = router;
