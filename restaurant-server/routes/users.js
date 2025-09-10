const express = require('express');
const { body, validationResult } = require('express-validator');
const User = require('../models/User');
const Restaurant = require('../models/Restaurant');
const { auth } = require('../middleware/auth');

const router = express.Router();

// 사용자 프로필 조회
router.get('/profile', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId)
                               .populate('bookmarks', 'name category rating imageUrl address')
                               .populate('favorites', 'name category rating imageUrl address');

        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        res.json({
            success: true,
            data: user
        });

    } catch (error) {
        console.error('프로필 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '프로필 조회 중 오류가 발생했습니다'
        });
    }
});

// 사용자 프로필 업데이트
router.put('/profile', auth, [
    body('username').optional().trim().isLength({ min: 2, max: 30 }).withMessage('사용자명은 2자 이상 30자 이하여야 합니다'),
    body('preferences.priceRange').optional().isIn(['저렴', '보통', '비싸', '고급']).withMessage('유효하지 않은 가격대입니다'),
    body('preferences.favoriteCategories').optional().isArray().withMessage('선호 카테고리는 배열이어야 합니다')
], async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '입력 데이터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const allowedUpdates = ['username', 'preferences', 'location'];
        const updates = {};

        // 허용된 필드만 업데이트
        Object.keys(req.body).forEach(key => {
            if (allowedUpdates.includes(key)) {
                updates[key] = req.body[key];
            }
        });

        const user = await User.findByIdAndUpdate(
            req.userId, 
            updates, 
            { new: true, runValidators: true }
        );

        console.log(`✏️ 프로필 업데이트: ${user.username}`);

        res.json({
            success: true,
            message: '프로필이 업데이트되었습니다',
            data: user
        });

    } catch (error) {
        console.error('프로필 업데이트 오류:', error);
        res.status(500).json({
            success: false,
            error: '프로필 업데이트 중 오류가 발생했습니다'
        });
    }
});

// 북마크 토글 (CLAUDE.md 규칙 준수: 서버 우선 동작)
router.post('/bookmarks/:restaurantId', auth, async (req, res) => {
    try {
        const restaurantId = req.params.restaurantId;

        // 맛집 존재 확인
        const restaurant = await Restaurant.findOne({ 
            _id: restaurantId, 
            isActive: true 
        });

        if (!restaurant) {
            return res.status(404).json({
                success: false,
                error: '맛집을 찾을 수 없습니다'
            });
        }

        // 사용자 찾기
        const user = await User.findById(req.userId);
        
        // 북마크 토글
        const isBookmarked = user.toggleBookmark(restaurantId);
        await user.save();

        const action = isBookmarked ? '추가' : '제거';
        console.log(`🔖 북마크 ${action}: ${user.username} → ${restaurant.name}`);

        res.json({
            success: true,
            message: `북마크가 ${action}되었습니다`,
            data: {
                isBookmarked,
                restaurantId,
                restaurantName: restaurant.name
            }
        });

    } catch (error) {
        console.error('북마크 토글 오류:', error);
        res.status(500).json({
            success: false,
            error: '북마크 처리 중 오류가 발생했습니다'
        });
    }
});

// 즐겨찾기 토글 (CLAUDE.md 규칙 준수: 서버 우선 동작)
router.post('/favorites/:restaurantId', auth, async (req, res) => {
    try {
        const restaurantId = req.params.restaurantId;

        // 맛집 존재 확인
        const restaurant = await Restaurant.findOne({ 
            _id: restaurantId, 
            isActive: true 
        });

        if (!restaurant) {
            return res.status(404).json({
                success: false,
                error: '맛집을 찾을 수 없습니다'
            });
        }

        // 사용자 찾기
        const user = await User.findById(req.userId);
        
        // 즐겨찾기 토글
        const isFavorite = user.toggleFavorite(restaurantId);
        await user.save();

        const action = isFavorite ? '추가' : '제거';
        console.log(`⭐ 즐겨찾기 ${action}: ${user.username} → ${restaurant.name}`);

        res.json({
            success: true,
            message: `즐겨찾기가 ${action}되었습니다`,
            data: {
                isFavorite,
                restaurantId,
                restaurantName: restaurant.name
            }
        });

    } catch (error) {
        console.error('즐겨찾기 토글 오류:', error);
        res.status(500).json({
            success: false,
            error: '즐겨찾기 처리 중 오류가 발생했습니다'
        });
    }
});

// 북마크 목록 조회
router.get('/bookmarks', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId)
                               .populate({
                                   path: 'bookmarks',
                                   match: { isActive: true },
                                   select: 'name category rating imageUrl address location reviewCount priceRange'
                               });

        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        console.log(`📋 북마크 목록 조회: ${user.username} - ${user.bookmarks.length}개`);

        res.json({
            success: true,
            data: user.bookmarks
        });

    } catch (error) {
        console.error('북마크 목록 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '북마크 목록 조회 중 오류가 발생했습니다'
        });
    }
});

// 즐겨찾기 목록 조회
router.get('/favorites', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId)
                               .populate({
                                   path: 'favorites',
                                   match: { isActive: true },
                                   select: 'name category rating imageUrl address location reviewCount priceRange'
                               });

        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        console.log(`⭐ 즐겨찾기 목록 조회: ${user.username} - ${user.favorites.length}개`);

        res.json({
            success: true,
            data: user.favorites
        });

    } catch (error) {
        console.error('즐겨찾기 목록 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '즐겨찾기 목록 조회 중 오류가 발생했습니다'
        });
    }
});

// 맛집 북마크/즐겨찾기 상태 확인
router.get('/status/:restaurantId', auth, async (req, res) => {
    try {
        const restaurantId = req.params.restaurantId;
        const user = await User.findById(req.userId);

        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        const isBookmarked = user.isBookmarked(restaurantId);
        const isFavorite = user.isFavorited(restaurantId);

        res.json({
            success: true,
            data: {
                isBookmarked,
                isFavorite,
                restaurantId
            }
        });

    } catch (error) {
        console.error('상태 확인 오류:', error);
        res.status(500).json({
            success: false,
            error: '상태 확인 중 오류가 발생했습니다'
        });
    }
});

// 북마크 제거
router.delete('/bookmarks/:restaurantId', auth, async (req, res) => {
    try {
        const restaurantId = req.params.restaurantId;
        const user = await User.findById(req.userId);

        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        const bookmarkIndex = user.bookmarks.indexOf(restaurantId);
        if (bookmarkIndex === -1) {
            return res.status(400).json({
                success: false,
                error: '북마크되지 않은 맛집입니다'
            });
        }

        user.bookmarks.splice(bookmarkIndex, 1);
        await user.save();

        console.log(`🗑️ 북마크 제거: ${user.username} → ${restaurantId}`);

        res.json({
            success: true,
            message: '북마크가 제거되었습니다',
            data: '북마크 제거 성공'
        });

    } catch (error) {
        console.error('북마크 제거 오류:', error);
        res.status(500).json({
            success: false,
            error: '북마크 제거 중 오류가 발생했습니다'
        });
    }
});

// 즐겨찾기 제거
router.delete('/favorites/:restaurantId', auth, async (req, res) => {
    try {
        const restaurantId = req.params.restaurantId;
        const user = await User.findById(req.userId);

        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        const favoriteIndex = user.favorites.indexOf(restaurantId);
        if (favoriteIndex === -1) {
            return res.status(400).json({
                success: false,
                error: '즐겨찾기되지 않은 맛집입니다'
            });
        }

        user.favorites.splice(favoriteIndex, 1);
        await user.save();

        console.log(`🗑️ 즐겨찾기 제거: ${user.username} → ${restaurantId}`);

        res.json({
            success: true,
            message: '즐겨찾기가 제거되었습니다',
            data: '즐겨찾기 제거 성공'
        });

    } catch (error) {
        console.error('즐겨찾기 제거 오류:', error);
        res.status(500).json({
            success: false,
            error: '즐겨찾기 제거 중 오류가 발생했습니다'
        });
    }
});

// 사용자 통계 조회
router.get('/stats', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId);

        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        const stats = {
            bookmarkCount: user.bookmarks.length,
            favoriteCount: user.favorites.length,
            joinedDate: user.createdAt,
            lastLoginAt: user.lastLoginAt,
            isEmailVerified: user.isEmailVerified
        };

        res.json({
            success: true,
            data: stats
        });

    } catch (error) {
        console.error('사용자 통계 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '사용자 통계 조회 중 오류가 발생했습니다'
        });
    }
});

// 계정 삭제
router.delete('/account', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId);

        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        // 사용자 계정 비활성화 (실제 삭제 대신)
        user.isActive = false;
        user.email = `deleted_${Date.now()}_${user.email}`;
        await user.save();

        console.log(`🗑️ 계정 삭제 요청: ${user.username}`);

        res.json({
            success: true,
            message: '계정이 삭제되었습니다',
            data: '계정 삭제 완료'
        });

    } catch (error) {
        console.error('계정 삭제 오류:', error);
        res.status(500).json({
            success: false,
            error: '계정 삭제 중 오류가 발생했습니다'
        });
    }
});

module.exports = router;