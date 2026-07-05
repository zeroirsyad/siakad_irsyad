const prisma = require('../config/db');

// Helper to check for schedule overlaps (collisions)
const isScheduleOverlap = (s1, s2) => {
  if (s1.hari !== s2.hari) return false;

  const toMinutes = (timeStr) => {
    const [h, m] = timeStr.split(':').map(Number);
    return h * 60 + m;
  };

  const start1 = toMinutes(s1.jamMulai);
  const end1 = toMinutes(s1.jamSelesai);
  const start2 = toMinutes(s2.jamMulai);
  const end2 = toMinutes(s2.jamSelesai);

  return start1 < end2 && start2 < end1;
};

const verifyPayment = async (req, res) => {
  try {
    const { mahasiswaId } = req.body;

    if (!mahasiswaId) {
      return res.status(400).json({ message: 'mahasiswaId wajib diisi' });
    }

    const mahasiswa = await prisma.mahasiswa.findUnique({
      where: { id: mahasiswaId }
    });

    if (!mahasiswa) {
      return res.status(404).json({ message: 'Mahasiswa tidak ditemukan' });
    }

    const updatedMahasiswa = await prisma.mahasiswa.update({
      where: { id: mahasiswaId },
      data: { uktStatus: 'Lunas' }
    });

    return res.status(200).json({
      message: `Pembayaran UKT untuk ${updatedMahasiswa.nama} berhasil diverifikasi (Status: Lunas)`,
      mahasiswa: updatedMahasiswa
    });
  } catch (error) {
    console.error('Error verifyPayment:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const createJadwal = async (req, res) => {
  try {
    const { mataKuliahId, dosenId, hari, jamMulai, jamSelesai, ruangan } = req.body;

    if (!mataKuliahId || !dosenId || !hari || !jamMulai || !jamSelesai || !ruangan) {
      return res.status(400).json({ message: 'Semua field penjadwalan wajib diisi' });
    }

    const targetInput = { hari, jamMulai, jamSelesai };

    // 1. Collision Detection: Lecturer check
    const existingDosenJadwal = await prisma.jadwal.findMany({
      where: { dosenId, hari },
      include: { mataKuliah: true }
    });

    const dosenConflict = existingDosenJadwal.find(j => isScheduleOverlap(j, targetInput));
    if (dosenConflict) {
      return res.status(400).json({
        message: `Konflik Terdeteksi! Dosen pengampu sudah dijadwalkan mengajar ${dosenConflict.mataKuliah.nama} pada hari ${hari} pukul ${dosenConflict.jamMulai} - ${dosenConflict.jamSelesai}.`
      });
    }

    // 2. Collision Detection: Room check
    const existingRoomJadwal = await prisma.jadwal.findMany({
      where: { ruangan, hari },
      include: { mataKuliah: true }
    });

    const roomConflict = existingRoomJadwal.find(j => isScheduleOverlap(j, targetInput));
    if (roomConflict) {
      return res.status(400).json({
        message: `Konflik Terdeteksi! Ruangan ${ruangan} sudah digunakan oleh mata kuliah ${roomConflict.mataKuliah.nama} pada hari ${hari} pukul ${roomConflict.jamMulai} - ${roomConflict.jamSelesai}.`
      });
    }

    // If no conflicts, create new schedule
    const newJadwal = await prisma.jadwal.create({
      data: {
        mataKuliahId,
        dosenId,
        hari,
        jamMulai,
        jamSelesai,
        ruangan
      },
      include: {
        mataKuliah: true,
        dosen: true
      }
    });

    return res.status(201).json({
      message: 'Jadwal kuliah berhasil dibuat tanpa konflik',
      jadwal: newJadwal
    });
  } catch (error) {
    console.error('Error createJadwal:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const getFinanceOverview = async (req, res) => {
  try {
    const listMahasiswa = await prisma.mahasiswa.findMany();
    const totalMahasiswa = listMahasiswa.length;
    const totalLunas = listMahasiswa.filter(m => m.uktStatus === 'Lunas').length;
    const totalBelumLunas = totalMahasiswa - totalLunas;
    const persentaseLunas = totalMahasiswa > 0 ? Math.round((totalLunas / totalMahasiswa) * 100) : 0;

    return res.status(200).json({
      summary: {
        totalMahasiswa,
        totalLunas,
        totalBelumLunas,
        persentaseLunas
      },
      mahasiswa: listMahasiswa
    });
  } catch (error) {
    console.error('Error getFinanceOverview:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const getAdminResources = async (req, res) => {
  try {
    const allMataKuliah = await prisma.mataKuliah.findMany();
    const allDosen = await prisma.dosen.findMany();
    return res.status(200).json({
      mataKuliah: allMataKuliah,
      dosen: allDosen
    });
  } catch (error) {
    console.error('Error getAdminResources:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

module.exports = {
  verifyPayment,
  createJadwal,
  getFinanceOverview,
  getAdminResources
};
