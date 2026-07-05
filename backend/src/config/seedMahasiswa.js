const bcrypt = require('bcryptjs');
const prisma = require('./db');

/**
 * Seed khusus untuk menambahkan 1 akun Mahasiswa baru (UKT Belum Lunas)
 * Tanpa menghapus data yang sudah ada di database.
 * 
 * Jalankan dengan: node src/config/seedMahasiswa.js
 */
const seedMahasiswa = async () => {
  try {
    console.log('🎓 Memulai seeding akun Mahasiswa...');

    // -----------------------------------------------------------
    // 1. Cari Dosen PA yang sudah ada untuk dijadikan pembimbing
    // -----------------------------------------------------------
    const dosenPa = await prisma.dosen.findFirst();
    if (!dosenPa) {
      console.error('❌ Tidak ada data Dosen di database. Jalankan seeder utama (seeder.js) terlebih dahulu.');
      process.exit(1);
    }
    console.log(`✅ Menggunakan Dosen PA: ${dosenPa.nama} (ID: ${dosenPa.id})`);

    // -----------------------------------------------------------
    // 2. Buat akun User untuk login
    // -----------------------------------------------------------
    const username = 'mhs_lunas';
    const existingUser = await prisma.user.findUnique({ where: { username } });

    if (existingUser) {
      console.warn(`⚠️  Akun dengan username "${username}" sudah ada. Seeding dilewati.`);
      process.exit(0);
    }

    const hashedPassword = await bcrypt.hash('password123', 10);

    const newUser = await prisma.user.create({
      data: {
        username,
        password: hashedPassword,
        role: 'Mahasiswa',
      },
    });
    console.log(`✅ Akun User berhasil dibuat (ID: ${newUser.id})`);

    // -----------------------------------------------------------
    // 3. Buat profil Mahasiswa yang terhubung ke akun user
    // -----------------------------------------------------------
    const newMahasiswa = await prisma.mahasiswa.create({
      data: {
        nim: '20210801100',
        nama: 'Rina Aulia',
        status: 'Aktif',
        uktStatus: 'Lunas',  // ✅ Status UKT sudah lunas
        ipk: 3.50,
        sksTotal: 18,
        userId: newUser.id,
        dosenPaId: dosenPa.id,
      },
    });

    console.log('\n✅ ========== Seeding Berhasil! ==========');
    console.log('   Nama     :', newMahasiswa.nama);
    console.log('   NIM      :', newMahasiswa.nim);
    console.log('   Username : mhs_lunas');
    console.log('   Password : password123');
    console.log('   UKT      : Lunas ✅');
    console.log('==========================================\n');

  } catch (error) {
    console.error('❌ Terjadi error saat seeding:', error);
  } finally {
    await prisma.$disconnect();
  }
};

seedMahasiswa();
