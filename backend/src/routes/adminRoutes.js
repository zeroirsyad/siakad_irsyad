const express = require('express');
const router = express.Router();
const { authMiddleware, roleMiddleware } = require('../middlewares/authMiddleware');
const {
  verifyPayment,
  createJadwal,
  getFinanceOverview,
  getAdminResources
} = require('../controllers/adminController');

// All admin routes require Admin role authentication
router.use(authMiddleware);
router.use(roleMiddleware(['Admin']));

router.get('/finance', getFinanceOverview);
router.post('/finance/verify', verifyPayment);
router.post('/jadwal', createJadwal);
router.get('/resources', getAdminResources);

module.exports = router;
