const prisma = require('../config/db');

// Helper to determine letter grade
const getLetterGrade = (finalGrade) => {
  if (finalGrade >= 85) return 'A';
  if (finalGrade >= 75) return 'B';
  if (finalGrade >= 65) return 'C';
  if (finalGrade >= 50) return 'D';
  return 'E';
};

const getBimbinganList = async (req, res) => {
  try {
    const dosen = req.user.dosen;
    if (!dosen) {
      return res.status(404).json({ message: 'Profil dosen tidak ditemukan' });
    }

    const bimbinganList = await prisma.mahasiswa.findMany({
      where: { dosenPaId: dosen.id },
      include: {
        krs: {
          include: {
            jadwal: {
              include: {
                mataKuliah: true
              }
            }
          }
        }
      }
    });

    return res.status(200).json({ bimbinganList });
  } catch (error) {
    console.error('Error getBimbinganList:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const reviewKrs = async (req, res) => {
  try {
    const dosen = req.user.dosen;
    const { mahasiswaId, action, catatan } = req.body; // action: "Approve" or "Reject"

    if (!mahasiswaId || !action) {
      return res.status(400).json({ message: 'mahasiswaId dan action wajib diisi' });
    }

    const mahasiswa = await prisma.mahasiswa.findUnique({
      where: { id: mahasiswaId, dosenPaId: dosen.id }
    });

    if (!mahasiswa) {
      return res.status(404).json({ message: 'Mahasiswa bimbingan tidak ditemukan' });
    }

    const targetStatus = action === 'Approve' ? 'Divalidasi' : 'Ditolak / Revisi';

    // Update all pending KRS entries for this student
    const updatedCount = await prisma.kRS.updateMany({
      where: {
        mahasiswaId,
        status: 'Menunggu Persetujuan'
      },
      data: {
        status: targetStatus,
        catatan: catatan || null
      }
    });

    // If approved, update student's cumulative SKS (sksTotal)
    if (action === 'Approve') {
      const approvedKrs = await prisma.kRS.findMany({
        where: { mahasiswaId, status: 'Divalidasi' },
        include: {
          jadwal: {
            include: {
              mataKuliah: true
            }
          }
        }
      });
      const totalSks = approvedKrs.reduce((sum, curr) => sum + curr.jadwal.mataKuliah.sks, 0);
      await prisma.mahasiswa.update({
        where: { id: mahasiswaId },
        data: { sksTotal: totalSks }
      });
    }

    return res.status(200).json({
      message: `KRS mahasiswa berhasil di-${action === 'Approve' ? 'setujui' : 'tolak'}`,
      updatedCount
    });
  } catch (error) {
    console.error('Error reviewKrs:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const getKelasList = async (req, res) => {
  try {
    const dosen = req.user.dosen;
    if (!dosen) {
      return res.status(404).json({ message: 'Profil dosen tidak ditemukan' });
    }

    const kelasList = await prisma.jadwal.findMany({
      where: { dosenId: dosen.id },
      include: {
        mataKuliah: true
      }
    });

    return res.status(200).json({ kelasList });
  } catch (error) {
    console.error('Error getKelasList:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const inputPresensi = async (req, res) => {
  try {
    const { jadwalId } = req.params;
    const { tanggal, presensiList } = req.body; // presensiList: [{ mahasiswaId, status: "Hadir"/"Sakit"/"Izin"/"Alpa" }]

    if (!tanggal || !presensiList || !Array.isArray(presensiList)) {
      return res.status(400).json({ message: 'tanggal dan presensiList wajib diisi' });
    }

    const records = [];
    for (const item of presensiList) {
      const record = await prisma.presensi.upsert({
        where: {
          mahasiswaId_jadwalId_tanggal: {
            mahasiswaId: item.mahasiswaId,
            jadwalId,
            tanggal
          }
        },
        update: {
          status: item.status
        },
        create: {
          mahasiswaId: item.mahasiswaId,
          jadwalId,
          tanggal,
          status: item.status
        }
      });
      records.push(record);
    }

    return res.status(200).json({
      message: 'Presensi berhasil disimpan',
      count: records.length,
      records
    });
  } catch (error) {
    console.error('Error inputPresensi:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const inputNilai = async (req, res) => {
  try {
    const { jadwalId } = req.params;
    const { nilaiList } = req.body; // nilaiList: [{ mahasiswaId, tugas, uts, uas }]

    if (!nilaiList || !Array.isArray(nilaiList)) {
      return res.status(400).json({ message: 'nilaiList wajib diisi' });
    }

    const jadwal = await prisma.jadwal.findUnique({
      where: { id: jadwalId },
      include: { mataKuliah: true }
    });

    if (!jadwal) {
      return res.status(404).json({ message: 'Jadwal kuliah tidak ditemukan' });
    }

    const updatedNilai = [];
    for (const item of nilaiList) {
      const tugas = parseFloat(item.tugas) || 0;
      const uts = parseFloat(item.uts) || 0;
      const uas = parseFloat(item.uas) || 0;

      // Realtime final grade calculations (Tugas 20%, UTS 30%, UAS 50%)
      const akhir = tugas * 0.2 + uts * 0.3 + uas * 0.5;
      const huruf = getLetterGrade(akhir);

      const record = await prisma.nilai.upsert({
        where: {
          mahasiswaId_mataKuliahId: {
            mahasiswaId: item.mahasiswaId,
            mataKuliahId: jadwal.mataKuliahId
          }
        },
        update: { tugas, uts, uas, akhir, huruf },
        create: {
          mahasiswaId: item.mahasiswaId,
          mataKuliahId: jadwal.mataKuliahId,
          tugas,
          uts,
          uas,
          akhir,
          huruf
        }
      });

      // Update student overall GPA (ipk) - dummy / mock calculation based on average of available grades
      const allStudentGrades = await prisma.nilai.findMany({
        where: { mahasiswaId: item.mahasiswaId }
      });

      const gradePoints = { 'A': 4.0, 'B': 3.0, 'C': 2.0, 'D': 1.0, 'E': 0.0 };
      const totalPoints = allStudentGrades.reduce((sum, g) => sum + gradePoints[g.huruf], 0);
      const ipk = allStudentGrades.length > 0 ? (totalPoints / allStudentGrades.length) : 0.0;

      await prisma.mahasiswa.update({
        where: { id: item.mahasiswaId },
        data: { ipk: parseFloat(ipk.toFixed(2)) }
      });

      updatedNilai.push(record);
    }

    return res.status(200).json({
      message: 'Nilai mahasiswa berhasil di-input dan kalkulasi akhir selesai',
      records: updatedNilai
    });
  } catch (error) {
    console.error('Error inputNilai:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const createKelas = async (req, res) => {
  try {
    const dosen = req.user.dosen;
    if (!dosen) return res.status(404).json({ message: 'Profil dosen tidak ditemukan' });

    // Dosen can either pass an existing mataKuliahId, or provide details to create a new one
    const { mataKuliahId, kodeMk, namaMk, sks, semester, hari, jamMulai, jamSelesai, ruangan, kuota } = req.body;
    
    let finalMkId = mataKuliahId;

    if (!finalMkId) {
      if (!kodeMk || !namaMk || !sks || !semester) {
        return res.status(400).json({ message: 'Data kriteria mata kuliah tidak lengkap' });
      }
      
      // Upsert MataKuliah
      const mk = await prisma.mataKuliah.upsert({
        where: { kode: kodeMk },
        update: { nama: namaMk, sks: parseInt(sks), semester: parseInt(semester), kuota: parseInt(kuota) || 40 },
        create: { kode: kodeMk, nama: namaMk, sks: parseInt(sks), semester: parseInt(semester), kuota: parseInt(kuota) || 40 }
      });
      finalMkId = mk.id;
    }

    const finalHari = hari || "Menunggu Admin";
    const finalJamMulai = jamMulai || "00:00";
    const finalJamSelesai = jamSelesai || "00:00";
    const finalRuangan = ruangan || "TBA";

    const jadwal = await prisma.jadwal.create({
      data: {
        mataKuliahId: finalMkId,
        dosenId: dosen.id,
        hari: finalHari,
        jamMulai: finalJamMulai,
        jamSelesai: finalJamSelesai,
        ruangan: finalRuangan
      },
      include: { mataKuliah: true }
    });

    return res.status(201).json({ message: 'Kelas berhasil dibuat', jadwal });
  } catch (error) {
    console.error('Error createKelas:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

const getKelasMahasiswa = async (req, res) => {
  try {
    const { jadwalId } = req.params;
    
    // Find all validated KRS for this Jadwal to get the enrolled students
    const enrolled = await prisma.kRS.findMany({
      where: { 
        jadwalId: jadwalId,
        status: 'Divalidasi' 
      },
      include: { 
        mahasiswa: true 
      }
    });

    const mahasiswaList = enrolled.map(krs => krs.mahasiswa);

    return res.status(200).json({ mahasiswaList });
  } catch (error) {
    console.error('Error getKelasMahasiswa:', error);
    return res.status(500).json({ message: 'Terjadi kesalahan server' });
  }
};

module.exports = {
  getBimbinganList,
  reviewKrs,
  getKelasList,
  inputPresensi,
  inputNilai,
  createKelas,
  getKelasMahasiswa
};
