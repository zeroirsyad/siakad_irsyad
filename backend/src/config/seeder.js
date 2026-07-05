const bcrypt = require('bcryptjs');
const prisma = require('./db');

const seed = async () => {
  try {
    console.log('Clearing database...');
    await prisma.presensi.deleteMany();
    await prisma.nilai.deleteMany();
    await prisma.kRS.deleteMany();
    await prisma.jadwal.deleteMany();
    await prisma.mahasiswa.deleteMany();
    await prisma.dosen.deleteMany();
    await prisma.mataKuliah.deleteMany();
    await prisma.user.deleteMany();

    console.log('Seeding Users...');
    const hashedPassword = await bcrypt.hash('password123', 10);

    // Create Admin
    const adminUser = await prisma.user.create({
      data: {
        username: 'admin',
        password: hashedPassword,
        role: 'Admin'
      }
    });

    // Create Dosen PA
    const dosenUser = await prisma.user.create({
      data: {
        username: 'dosen1',
        password: hashedPassword,
        role: 'Dosen'
      }
    });

    const dosenProfile = await prisma.dosen.create({
      data: {
        nidn: 'dosen2',
        nama: 'Dr. Ahmad Fauzi',
        prodi: 'Informatika',
        userId: dosenUser.id
      }
    });

    // Create Mahasiswa
    const mhsUser = await prisma.user.create({
      data: {
        username: 'mhs1',
        password: hashedPassword,
        role: 'Mahasiswa'
      }
    });

    const mhsProfile = await prisma.mahasiswa.create({
      data: {
        nim: '20210801045',
        nama: 'Budi Santoso',
        status: 'Aktif',
        uktStatus: 'Belum Lunas', // Belum lunas so we can test the lock page
        ipk: 3.82,
        sksTotal: 18,
        userId: mhsUser.id,
        dosenPaId: dosenProfile.id
      }
    });

    console.log('Seeding Mata Kuliah...');
    const mkData = [
      { kode: 'CS201', nama: 'Data Structures', sks: 3, semester: 3, kuota: 40 },
      { kode: 'CS202', nama: 'Object Oriented Programming', sks: 3, semester: 3, kuota: 40 },
      { kode: 'CS304', nama: 'Web Dev', sks: 4, semester: 3, kuota: 40 },
      { kode: 'CS402', nama: 'Artificial Intelligence', sks: 3, semester: 3, kuota: 40 },
      { kode: 'CS205', nama: 'Database Systems', sks: 3, semester: 3, kuota: 40 },
      { kode: 'CS208', nama: 'Computer Architecture', sks: 3, semester: 3, kuota: 40 }
    ];

    const mks = {};
    for (const mk of mkData) {
      const createdMk = await prisma.mataKuliah.create({ data: mk });
      mks[mk.kode] = createdMk;
    }

    console.log('Seeding Jadwal...');
    // We create schedules. Note: CS201 (Data Structures) and CS208 (Computer Architecture) are on Monday 08:00 to create a conflict!
    const jadwalData = [
      {
        mataKuliahId: mks['CS201'].id,
        dosenId: dosenProfile.id,
        hari: 'Senin',
        jamMulai: '08:00',
        jamSelesai: '10:30',
        ruangan: 'Lab B.204'
      },
      {
        mataKuliahId: mks['CS202'].id,
        dosenId: dosenProfile.id,
        hari: 'Selasa',
        jamMulai: '08:00',
        jamSelesai: '10:30',
        ruangan: 'Lab A'
      },
      {
        mataKuliahId: mks['CS304'].id,
        dosenId: dosenProfile.id,
        hari: 'Rabu',
        jamMulai: '08:00',
        jamSelesai: '11:30',
        ruangan: 'Lab B.204'
      },
      {
        mataKuliahId: mks['CS402'].id,
        dosenId: dosenProfile.id,
        hari: 'Kamis',
        jamMulai: '13:00',
        jamSelesai: '15:30',
        ruangan: 'R.204'
      },
      {
        mataKuliahId: mks['CS205'].id,
        dosenId: dosenProfile.id,
        hari: 'Jumat',
        jamMulai: '08:00',
        jamSelesai: '10:30',
        ruangan: 'R.501'
      },
      {
        mataKuliahId: mks['CS208'].id,
        dosenId: dosenProfile.id,
        hari: 'Senin',
        jamMulai: '08:00',
        jamSelesai: '10:30',
        ruangan: 'Lab B.204' // Creating conflict on Monday 08:00 in Lab B.204
      }
    ];

    const jadwalList = [];
    for (const j of jadwalData) {
      const createdJadwal = await prisma.jadwal.create({ data: j });
      jadwalList.push(createdJadwal);
    }

    console.log('Seeding KRS Draft for Budi Santoso...');
    // We pre-select SKS.
    // And let's select CS208 (3) which is bentrok with CS201 to demo the conflict alert!
    const selectedSchedules = [
      jadwalList[0], // CS201 (Senin 08:00)
      jadwalList[1], // CS202 (Selasa 08:00)
      jadwalList[2], // CS304 (Rabu 08:00)
      jadwalList[4], // CS205 (Jumat 08:00)
      jadwalList[5]  // CS208 (Senin 08:00 - BENTROK dengan CS201)
    ];

    for (const s of selectedSchedules) {
      await prisma.kRS.create({
        data: {
          mahasiswaId: mhsProfile.id,
          jadwalId: s.id,
          status: 'Draft'
        }
      });
    }

    console.log('Database seeded successfully!');
  } catch (error) {
    console.error('Error seeding database:', error);
  } finally {
    await prisma.$disconnect();
  }
};

if (require.main === module) {
  seed();
}

module.exports = seed;
