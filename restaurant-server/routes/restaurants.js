const express = require('express');
const { body, query, validationResult } = require('express-validator');
const Restaurant = require('../models/Restaurant');
const { auth, optionalAuth } = require('../middleware/auth');

const router = express.Router();

// 모든 맛집 조회 (페이지네이션 지원)
router.get('/', [
    query('page').optional().isInt({ min: 1 }).withMessage('페이지는 1 이상의 정수여야 합니다'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('limit은 1-100 사이여야 합니다'),
    query('category').optional().isIn(['한식', '중식', '일식', '양식', '카페', '디저트', '패스트푸드', '기타']).withMessage('유효하지 않은 카테고리입니다'),
    query('minRating').optional().isFloat({ min: 0, max: 5 }).withMessage('최소 평점은 0-5 사이여야 합니다')
], optionalAuth, async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '쿼리 파라미터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;
        
        // 필터 조건 구성
        const filter = { isActive: true };
        
        if (req.query.category) {
            filter.category = req.query.category;
        }
        
        if (req.query.minRating) {
            filter.rating = { $gte: parseFloat(req.query.minRating) };
        }

        // 정렬 기준
        const sort = { rating: -1, reviewCount: -1, createdAt: -1 };

        const restaurants = await Restaurant.find(filter)
                                          .sort(sort)
                                          .skip(skip)
                                          .limit(limit)
                                          .lean();

        const total = await Restaurant.countDocuments(filter);

        console.log(`📋 맛집 목록 조회: ${restaurants.length}개 (페이지 ${page}/${Math.ceil(total/limit)})`);

        res.json({
            success: true,
            data: restaurants,
            pagination: {
                current: page,
                pages: Math.ceil(total / limit),
                total,
                limit
            }
        });

    } catch (error) {
        console.error('맛집 목록 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '맛집 목록 조회 중 오류가 발생했습니다'
        });
    }
});

// 맛집 검색
router.get('/search', [
    query('q').notEmpty().withMessage('검색어를 입력해주세요'),
    query('page').optional().isInt({ min: 1 }).withMessage('페이지는 1 이상의 정수여야 합니다'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('limit은 1-100 사이여야 합니다')
], optionalAuth, async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '검색 파라미터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const query = req.query.q;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;

        // 텍스트 검색 (이름, 설명, 태그)
        const restaurants = await Restaurant.find({
            $and: [
                { isActive: true },
                {
                    $or: [
                        { name: { $regex: query, $options: 'i' } },
                        { description: { $regex: query, $options: 'i' } },
                        { address: { $regex: query, $options: 'i' } },
                        { tags: { $regex: query, $options: 'i' } },
                        { category: { $regex: query, $options: 'i' } }
                    ]
                }
            ]
        })
        .sort({ rating: -1, reviewCount: -1 })
        .skip(skip)
        .limit(limit)
        .lean();

        const total = await Restaurant.countDocuments({
            $and: [
                { isActive: true },
                {
                    $or: [
                        { name: { $regex: query, $options: 'i' } },
                        { description: { $regex: query, $options: 'i' } },
                        { address: { $regex: query, $options: 'i' } },
                        { tags: { $regex: query, $options: 'i' } },
                        { category: { $regex: query, $options: 'i' } }
                    ]
                }
            ]
        });

        console.log(`🔍 맛집 검색: "${query}" - ${restaurants.length}개 결과`);

        res.json({
            success: true,
            data: restaurants,
            pagination: {
                current: page,
                pages: Math.ceil(total / limit),
                total,
                limit
            },
            query
        });

    } catch (error) {
        console.error('맛집 검색 오류:', error);
        res.status(500).json({
            success: false,
            error: '맛집 검색 중 오류가 발생했습니다'
        });
    }
});

// 주변 맛집 검색
router.get('/nearby', [
    query('lat').isFloat({ min: -90, max: 90 }).withMessage('위도는 -90~90 사이여야 합니다'),
    query('lng').isFloat({ min: -180, max: 180 }).withMessage('경도는 -180~180 사이여야 합니다'),
    query('radius').optional().isFloat({ min: 0.1, max: 50 }).withMessage('반경은 0.1~50km 사이여야 합니다'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('limit은 1-100 사이여야 합니다')
], optionalAuth, async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '위치 파라미터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const lat = parseFloat(req.query.lat);
        const lng = parseFloat(req.query.lng);
        const radius = parseFloat(req.query.radius) || 5; // 기본 5km
        const limit = parseInt(req.query.limit) || 20;

        const restaurants = await Restaurant.find({
            location: {
                $near: {
                    $geometry: {
                        type: 'Point',
                        coordinates: [lng, lat]
                    },
                    $maxDistance: radius * 1000 // km를 미터로 변환
                }
            },
            isActive: true
        })
        .limit(limit)
        .lean();

        console.log(`📍 주변 맛집 검색: (${lat}, ${lng}) 반경 ${radius}km - ${restaurants.length}개`);

        res.json({
            success: true,
            data: restaurants,
            searchArea: {
                center: { lat, lng },
                radius: radius + 'km'
            }
        });

    } catch (error) {
        console.error('주변 맛집 검색 오류:', error);
        res.status(500).json({
            success: false,
            error: '주변 맛집 검색 중 오류가 발생했습니다'
        });
    }
});

// 카테고리 목록 조회
router.get('/categories', async (req, res) => {
    try {
        const categories = ['한식', '중식', '일식', '양식', '카페', '디저트', '패스트푸드', '기타'];
        
        // 각 카테고리별 맛집 수 계산
        const categoriesWithCount = await Promise.all(
            categories.map(async (category) => {
                const count = await Restaurant.countDocuments({ 
                    category, 
                    isActive: true 
                });
                return { name: category, count };
            })
        );

        res.json({
            success: true,
            data: categoriesWithCount
        });

    } catch (error) {
        console.error('카테고리 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '카테고리 조회 중 오류가 발생했습니다'
        });
    }
});

// 인기 맛집 조회
router.get('/popular', [
    query('category').optional().isIn(['한식', '중식', '일식', '양식', '카페', '디저트', '패스트푸드', '기타']).withMessage('유효하지 않은 카테고리입니다'),
    query('limit').optional().isInt({ min: 1, max: 50 }).withMessage('limit은 1-50 사이여야 합니다')
], optionalAuth, async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '쿼리 파라미터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const category = req.query.category;
        const limit = parseInt(req.query.limit) || 10;

        const filter = { 
            isActive: true,
            rating: { $gte: 4.0 }, // 평점 4.0 이상
            reviewCount: { $gte: 5 } // 리뷰 5개 이상
        };

        if (category) {
            filter.category = category;
        }

        const restaurants = await Restaurant.find(filter)
                                          .sort({ rating: -1, reviewCount: -1 })
                                          .limit(limit)
                                          .lean();

        console.log(`⭐ 인기 맛집 조회: ${category || '전체'} - ${restaurants.length}개`);

        res.json({
            success: true,
            data: restaurants,
            category: category || '전체'
        });

    } catch (error) {
        console.error('인기 맛집 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '인기 맛집 조회 중 오류가 발생했습니다'
        });
    }
});

// 메인 카테고리별 맛집 조회
router.get('/main-category/:mainCategory', [
    query('page').optional().isInt({ min: 1 }).withMessage('페이지는 1 이상의 정수여야 합니다'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('limit은 1-100 사이여야 합니다')
], optionalAuth, async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '쿼리 파라미터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const mainCategory = req.params.mainCategory;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;

        const filter = { 
            isActive: true,
            mainCategory: mainCategory
        };

        const restaurants = await Restaurant.find(filter)
                                          .sort({ rating: -1, reviewCount: -1, createdAt: -1 })
                                          .skip(skip)
                                          .limit(limit)
                                          .lean();

        const total = await Restaurant.countDocuments(filter);

        console.log(`📋 메인카테고리 조회 [${mainCategory}]: ${restaurants.length}개 (페이지 ${page}/${Math.ceil(total/limit)})`);

        res.json({
            success: true,
            data: restaurants,
            pagination: {
                current: page,
                pages: Math.ceil(total / limit),
                total,
                limit
            },
            category: mainCategory
        });

    } catch (error) {
        console.error('메인카테고리 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '메인카테고리 조회 중 오류가 발생했습니다'
        });
    }
});

// 서브 카테고리별 맛집 조회
router.get('/sub-category', [
    query('mainCategory').notEmpty().withMessage('메인 카테고리는 필수입니다'),
    query('subCategory').notEmpty().withMessage('서브 카테고리는 필수입니다'),
    query('page').optional().isInt({ min: 1 }).withMessage('페이지는 1 이상의 정수여야 합니다'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('limit은 1-100 사이여야 합니다')
], optionalAuth, async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '쿼리 파라미터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const mainCategory = req.query.mainCategory;
        const subCategory = req.query.subCategory;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;

        const filter = { 
            isActive: true,
            mainCategory: mainCategory,
            subCategory: subCategory
        };

        const restaurants = await Restaurant.find(filter)
                                          .sort({ rating: -1, reviewCount: -1, createdAt: -1 })
                                          .skip(skip)
                                          .limit(limit)
                                          .lean();

        const total = await Restaurant.countDocuments(filter);

        console.log(`📋 서브카테고리 조회 [${mainCategory}>${subCategory}]: ${restaurants.length}개`);

        res.json({
            success: true,
            data: restaurants,
            pagination: {
                current: page,
                pages: Math.ceil(total / limit),
                total,
                limit
            },
            mainCategory,
            subCategory
        });

    } catch (error) {
        console.error('서브카테고리 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '서브카테고리 조회 중 오류가 발생했습니다'
        });
    }
});

// 카테고리 내 검색
router.get('/search-in-category', [
    query('categoryName').notEmpty().withMessage('카테고리명은 필수입니다'),
    query('q').notEmpty().withMessage('검색어를 입력해주세요'),
    query('page').optional().isInt({ min: 1 }).withMessage('페이지는 1 이상의 정수여야 합니다'),
    query('limit').optional().isInt({ min: 1, max: 100 }).withMessage('limit은 1-100 사이여야 합니다')
], optionalAuth, async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '검색 파라미터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const categoryName = req.query.categoryName;
        const query = req.query.q;
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 20;
        const skip = (page - 1) * limit;

        // 카테고리 내에서 텍스트 검색
        const restaurants = await Restaurant.find({
            $and: [
                { isActive: true },
                { 
                    $or: [
                        { mainCategory: categoryName },
                        { category: categoryName }
                    ]
                },
                {
                    $or: [
                        { name: { $regex: query, $options: 'i' } },
                        { description: { $regex: query, $options: 'i' } },
                        { address: { $regex: query, $options: 'i' } },
                        { tags: { $regex: query, $options: 'i' } }
                    ]
                }
            ]
        })
        .sort({ rating: -1, reviewCount: -1 })
        .skip(skip)
        .limit(limit)
        .lean();

        const total = await Restaurant.countDocuments({
            $and: [
                { isActive: true },
                { 
                    $or: [
                        { mainCategory: categoryName },
                        { category: categoryName }
                    ]
                },
                {
                    $or: [
                        { name: { $regex: query, $options: 'i' } },
                        { description: { $regex: query, $options: 'i' } },
                        { address: { $regex: query, $options: 'i' } },
                        { tags: { $regex: query, $options: 'i' } }
                    ]
                }
            ]
        });

        console.log(`🔍 카테고리 내 검색 [${categoryName}]: "${query}" - ${restaurants.length}개 결과`);

        res.json({
            success: true,
            data: restaurants,
            pagination: {
                current: page,
                pages: Math.ceil(total / limit),
                total,
                limit
            },
            categoryName,
            query
        });

    } catch (error) {
        console.error('카테고리 내 검색 오류:', error);
        res.status(500).json({
            success: false,
            error: '카테고리 내 검색 중 오류가 발생했습니다'
        });
    }
});

// 특정 맛집 상세 조회 (파라미터 경로는 마지막에 위치)
router.get('/:id', optionalAuth, async (req, res) => {
    try {
        const restaurant = await Restaurant.findOne({ 
            _id: req.params.id, 
            isActive: true 
        }).lean();

        if (!restaurant) {
            return res.status(404).json({
                success: false,
                error: '맛집을 찾을 수 없습니다'
            });
        }

        console.log(`🍽️ 맛집 상세 조회: ${restaurant.name}`);

        res.json({
            success: true,
            data: restaurant
        });

    } catch (error) {
        console.error('맛집 상세 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '맛집 상세 조회 중 오류가 발생했습니다'
        });
    }
});

// 맛집 생성 (관리자용 - 향후 구현)
router.post('/', auth, [
    body('name').trim().notEmpty().withMessage('맛집 이름은 필수입니다'),
    body('category').isIn(['한식', '중식', '일식', '양식', '카페', '디저트', '패스트푸드', '기타']).withMessage('유효하지 않은 카테고리입니다'),
    body('address').trim().notEmpty().withMessage('주소는 필수입니다'),
    body('latitude').isFloat({ min: -90, max: 90 }).withMessage('위도는 -90~90 사이여야 합니다'),
    body('longitude').isFloat({ min: -180, max: 180 }).withMessage('경도는 -180~180 사이여야 합니다')
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

        const restaurantData = {
            ...req.body,
            location: {
                type: 'Point',
                coordinates: [req.body.longitude, req.body.latitude]
            },
            owner: req.userId
        };

        const restaurant = new Restaurant(restaurantData);
        await restaurant.save();

        console.log(`✅ 새 맛집 등록: ${restaurant.name} by ${req.user.username}`);

        res.status(201).json({
            success: true,
            message: '맛집이 등록되었습니다',
            data: restaurant
        });

    } catch (error) {
        console.error('맛집 등록 오류:', error);
        res.status(500).json({
            success: false,
            error: '맛집 등록 중 오류가 발생했습니다'
        });
    }
});

module.exports = router;