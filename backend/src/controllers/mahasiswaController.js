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

const getDashboard = async (req, res) => {
  try {
    const mahasiswa = req.user.mahasiswa;
    if (!mahasiswa) {
      return res.status(404).json({ message: 'Profil mahasiswa tidak ditemukan' });
    }

    // Get today's schedule from added KRS (Draft, Menunggu Persetujuan, Divalidasi)
    const dayNames = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
    const todayName = dayNames[new Date().getDay()];

    const studentKrs = await prisma.kRS.findMany({
      where: {
        mahasiswaId: mahasiswa.id,
        status: { in: ['Draft', 'Menunggu Persetujuan', 'Divalidasi'] }
      },
      include: {
        jadwal: {
          include: {
            mataKuliah: true,
            dosen: true
          }
        }
      }
    });

    let jadwalHariIni = studentKrs
      .map(k => {
        // Embed the KRS status inside the jadwal object for reference if needed
        const j = k.jadwal;
        j.statusKrs = k.status;
        return j;
      })
      .filter(j => j.hari === todayName);

    // Fallback: If today is a free day/weekend or there are no classes today, show all added classes
    if (jadwalHariIni.length === 0) {
      jadwalHariIni = studentKrs.map(k => {
        const j = k.jadwal;
        j.statusKrs = k.status;
        return j;
      });
    }

    return res.status(200).json({
      mahasiswa: {
        id: mahasiswa.id,
        nim: mahasiswa.nim,
        nama: mahasiswa.nama,
        status: mahasiswa.status,
        uktStatus: mahasiswa.uktStatus,
        ipk: mahasiswa.ipk,
        sksTotal: mahasiswa.sksTotal,
        dosenPa: req.user.mahasiswa.dosenPa ? req.user.mahasiswa.dosenPa.nama : 'Belum Ditentukan'
      },
      jadwalHariIni
    });
  } catch (error) {
    console.error('Error getDashboard:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const getKrsPage = async (req, res) => {
  try {
    const mahasiswa = req.user.mahasiswa;
    if (!mahasiswa) {
      return res.status(404).json({ message: 'Profil mahasiswa tidak ditemukan' });
    }

    // Return the lock state immediately if UKT is not paid
    if (mahasiswa.uktStatus !== 'Lunas') {
      return res.status(200).json({
        isLocked: true,
        message: 'Akses KRS Terkunci. Silakan lakukan pembayaran UKT terlebih dahulu.',
        uktStatus: mahasiswa.uktStatus
      });
    }

    // Get current SKS cap (custom logic based on IPK: IPK >= 3.00 max 24, else max 20)
    const maxSks = mahasiswa.ipk >= 3.00 ? 24 : 20;

    // Get student's draft/selected courses
    const selectedKrs = await prisma.kRS.findMany({
      where: { mahasiswaId: mahasiswa.id },
      include: {
        jadwal: {
          include: {
            mataKuliah: true,
            dosen: true
          }
        }
      }
    });

    const totalSelectedSks = selectedKrs.reduce((acc, curr) => acc + curr.jadwal.mataKuliah.sks, 0);

    // Get all available schedules (active scheduling catalogs)
    const allSchedules = await prisma.jadwal.findMany({
      include: {
        mataKuliah: true,
        dosen: true
      }
    });

    // Determine collisions among selected courses
    const collisions = [];
    for (let i = 0; i < selectedKrs.length; i++) {
      for (let j = i + 1; j < selectedKrs.length; j++) {
        if (isScheduleOverlap(selectedKrs[i].jadwal, selectedKrs[j].jadwal)) {
          collisions.push({
            course1: selectedKrs[i].jadwal.mataKuliah.nama,
            course2: selectedKrs[j].jadwal.mataKuliah.nama,
            hari: selectedKrs[i].jadwal.hari,
            jam: `${selectedKrs[i].jadwal.jamMulai} - ${selectedKrs[i].jadwal.jamSelesai}`
          });
        }
      }
    }

    return res.status(200).json({
      isLocked: false,
      maxSks,
      totalSelectedSks,
      selectedKrs,
      allSchedules,
      collisions
    });
  } catch (error) {
    console.error('Error getKrsPage:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const pilihMataKuliah = async (req, res) => {
  try {
    const mahasiswa = req.user.mahasiswa;
    const { jadwalId } = req.body;

    if (!jadwalId) {
      return res.status(400).json({ message: 'jadwalId wajib dikirim' });
    }

    // 1. UKT Check
    if (mahasiswa.uktStatus !== 'Lunas') {
      return res.status(403).json({ message: 'Pembayaran UKT belum diselesaikan. KRS Terkunci.' });
    }

    // Fetch the target schedule
    const targetJadwal = await prisma.jadwal.findUnique({
      where: { id: jadwalId },
      include: {
        mataKuliah: true,
        dosen: true
      }
    });

    if (!targetJadwal) {
      return res.status(404).json({ message: 'Jadwal kuliah tidak ditemukan' });
    }

    // 2. Class Quota Check
    const activeEnrolledCount = await prisma.kRS.count({
      where: {
        jadwalId: targetJadwal.id,
        status: { in: ['Draft', 'Menunggu Persetujuan', 'Divalidasi'] }
      }
    });

    if (activeEnrolledCount >= targetJadwal.mataKuliah.kuota) {
      return res.status(400).json({ message: `Kuota kelas untuk ${targetJadwal.mataKuliah.nama} sudah penuh.` });
    }

    // Get current selections to check SKS cap and collisions
    const currentSelections = await prisma.kRS.findMany({
      where: { mahasiswaId: mahasiswa.id },
      include: {
        jadwal: {
          include: {
            mataKuliah: true
          }
        }
      }
    });

    // Check if already selected
    const alreadySelected = currentSelections.some(k => k.jadwalId === jadwalId);
    if (alreadySelected) {
      return res.status(400).json({ message: 'Mata kuliah ini sudah masuk dalam draft Anda.' });
    }

    // 3. SKS Limit Check
    const maxSks = mahasiswa.ipk >= 3.00 ? 24 : 20;
    const currentSksTotal = currentSelections.reduce((sum, curr) => sum + curr.jadwal.mataKuliah.sks, 0);

    if (currentSksTotal + targetJadwal.mataKuliah.sks > maxSks) {
      return res.status(400).json({ message: `Pengambilan SKS melebihi batas maksimal Anda (${maxSks} SKS).` });
    }

    // 4. Schedule Collision Check
    const collisionJadwal = currentSelections.find(k => isScheduleOverlap(k.jadwal, targetJadwal));
    if (collisionJadwal) {
      return res.status(400).json({
        message: `Jadwal bentrok dengan mata kuliah ${collisionJadwal.jadwal.mataKuliah.nama} pada hari ${targetJadwal.hari} pukul ${targetJadwal.jamMulai}`
      });
    }

    // If passed all validations, create the KRS Draft entry
    const newKrsEntry = await prisma.kRS.create({
      data: {
        mahasiswaId: mahasiswa.id,
        jadwalId: targetJadwal.id,
        status: 'Draft'
      },
      include: {
        jadwal: {
          include: {
            mataKuliah: true,
            dosen: true
          }
        }
      }
    });

    return res.status(201).json({
      message: 'Mata kuliah berhasil ditambahkan ke draft KRS',
      krs: newKrsEntry
    });
  } catch (error) {
    console.error('Error pilihMataKuliah:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const hapusMataKuliah = async (req, res) => {
  try {
    const mahasiswa = req.user.mahasiswa;
    const { jadwalId } = req.params;

    const krsItem = await prisma.kRS.findFirst({
      where: {
        mahasiswaId: mahasiswa.id,
        jadwalId
      }
    });

    if (!krsItem) {
      return res.status(404).json({ message: 'Mata kuliah tidak ditemukan di draft KRS' });
    }

    // Only allow deletion if status is not validated
    if (krsItem.status === 'Divalidasi') {
      return res.status(400).json({ message: 'KRS yang sudah divalidasi Dosen PA tidak dapat dihapus.' });
    }

    await prisma.kRS.delete({
      where: { id: krsItem.id }
    });

    return res.status(200).json({ message: 'Mata kuliah berhasil dihapus dari draft KRS' });
  } catch (error) {
    console.error('Error hapusMataKuliah:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const ajukanKrs = async (req, res) => {
  try {
    const mahasiswa = req.user.mahasiswa;

    const draftKrs = await prisma.kRS.findMany({
      where: {
        mahasiswaId: mahasiswa.id,
        status: { in: ['Draft', 'Ditolak / Revisi'] }
      }
    });

    if (draftKrs.length === 0) {
      return res.status(400).json({ message: 'Tidak ada mata kuliah draft atau revisi untuk diajukan.' });
    }

    // Update statuses to "Menunggu Persetujuan"
    await prisma.kRS.updateMany({
      where: {
        mahasiswaId: mahasiswa.id,
        status: { in: ['Draft', 'Ditolak / Revisi'] }
      },
      data: {
        status: 'Menunggu Persetujuan'
      }
    });

    return res.status(200).json({ message: 'KRS berhasil diajukan ke Dosen Pembimbing Akademik' });
  } catch (error) {
    console.error('Error ajukanKrs:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const getKhs = async (req, res) => {
  try {
    const mahasiswa = req.user.mahasiswa;
    if (!mahasiswa) {
      return res.status(404).json({ message: 'Profil mahasiswa tidak ditemukan' });
    }

    const nilaiList = await prisma.nilai.findMany({
      where: { mahasiswaId: mahasiswa.id },
      include: {
        mataKuliah: true
      }
    });

    return res.status(200).json({
      ipk: mahasiswa.ipk,
      sksTotal: mahasiswa.sksTotal,
      nilaiList
    });
  } catch (error) {
    console.error('Error getKhs:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

module.exports = {
  getDashboard,
  getKrsPage,
  pilihMataKuliah,
  hapusMataKuliah,
  ajukanKrs,
  getKhs
};
