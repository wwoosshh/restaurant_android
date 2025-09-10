const express = require('express');
const jwt = require('jsonwebtoken');
const { body, validationResult } = require('express-validator');
const User = require('../models/User');
const { auth } = require('../middleware/auth');

const router = express.Router();

// JWT 토큰 생성 함수
const generateToken = (userId) => {
    return jwt.sign(
        { userId },
        process.env.JWT_SECRET || 'fallback-secret-key',
        { expiresIn: process.env.JWT_EXPIRES_IN || '7d' }
    );
};

// 회원가입
router.post('/register', [
    body('username')
        .trim()
        .isLength({ min: 2, max: 30 })
        .withMessage('사용자명은 2자 이상 30자 이하여야 합니다'),
    body('email')
        .isEmail()
        .normalizeEmail()
        .withMessage('올바른 이메일 주소를 입력해주세요'),
    body('password')
        .isLength({ min: 6 })
        .withMessage('비밀번호는 최소 6자 이상이어야 합니다')
        .matches(/^(?=.*[a-zA-Z])(?=.*[0-9])/)
        .withMessage('비밀번호는 영문자와 숫자를 포함해야 합니다')
], async (req, res) => {
    try {
        // 유효성 검사 결과 확인
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '입력 데이터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const { username, email, password } = req.body;

        // 이메일 중복 확인
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            return res.status(400).json({
                success: false,
                error: '이미 가입된 이메일입니다'
            });
        }

        // 새 사용자 생성
        const user = new User({
            username,
            email,
            password
        });

        await user.save();

        // JWT 토큰 생성
        const token = generateToken(user._id);

        // 이메일 인증 코드 생성 (향후 구현)
        const verificationCode = user.generateEmailVerificationCode();
        await user.save();

        console.log(`✅ 새 사용자 가입: ${username} (${email})`);

        res.status(201).json({
            success: true,
            message: '회원가입이 완료되었습니다',
            data: {
                token,
                user: {
                    id: user._id,
                    username: user.username,
                    email: user.email,
                    isEmailVerified: user.isEmailVerified,
                    bookmarks: user.bookmarks,
                    favorites: user.favorites,
                    createdAt: user.createdAt,
                    updatedAt: user.updatedAt
                }
            }
        });

    } catch (error) {
        console.error('회원가입 오류:', error);
        res.status(500).json({
            success: false,
            error: '회원가입 중 오류가 발생했습니다'
        });
    }
});

// 로그인
router.post('/login', [
    body('email')
        .isEmail()
        .normalizeEmail()
        .withMessage('올바른 이메일 주소를 입력해주세요'),
    body('password')
        .notEmpty()
        .withMessage('비밀번호를 입력해주세요')
], async (req, res) => {
    try {
        // 유효성 검사 결과 확인
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '입력 데이터가 올바르지 않습니다',
                details: errors.array()
            });
        }

        const { email, password } = req.body;

        // 사용자 찾기 (비밀번호 포함)
        const user = await User.findByEmailWithPassword(email);
        if (!user) {
            return res.status(401).json({
                success: false,
                error: '이메일 또는 비밀번호가 올바르지 않습니다'
            });
        }

        // 비밀번호 확인
        const isPasswordValid = await user.comparePassword(password);
        if (!isPasswordValid) {
            return res.status(401).json({
                success: false,
                error: '이메일 또는 비밀번호가 올바르지 않습니다'
            });
        }

        // 사용자 활동 업데이트
        await user.updateActivity();

        // JWT 토큰 생성
        const token = generateToken(user._id);

        console.log(`✅ 사용자 로그인: ${user.username} (${email})`);

        res.json({
            success: true,
            message: '로그인이 완료되었습니다',
            data: {
                token,
                user: {
                    id: user._id,
                    username: user.username,
                    email: user.email,
                    isEmailVerified: user.isEmailVerified,
                    bookmarks: user.bookmarks,
                    favorites: user.favorites,
                    createdAt: user.createdAt,
                    updatedAt: user.updatedAt
                }
            }
        });

    } catch (error) {
        console.error('로그인 오류:', error);
        res.status(500).json({
            success: false,
            error: '로그인 중 오류가 발생했습니다'
        });
    }
});

// 이메일 인증
router.post('/verify-email', [
    body('email')
        .isEmail()
        .normalizeEmail()
        .withMessage('올바른 이메일 주소를 입력해주세요'),
    body('code')
        .isLength({ min: 6, max: 6 })
        .isNumeric()
        .withMessage('인증 코드는 6자리 숫자여야 합니다')
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

        const { email, code } = req.body;

        const user = await User.findOne({ 
            email,
            emailVerificationCode: code,
            emailVerificationExpires: { $gt: Date.now() }
        }).select('+emailVerificationCode +emailVerificationExpires');

        if (!user) {
            return res.status(400).json({
                success: false,
                error: '유효하지 않거나 만료된 인증 코드입니다'
            });
        }

        // 이메일 인증 완료
        user.isEmailVerified = true;
        user.emailVerificationCode = undefined;
        user.emailVerificationExpires = undefined;
        await user.save();

        console.log(`✅ 이메일 인증 완료: ${email}`);

        res.json({
            success: true,
            message: '이메일 인증이 완료되었습니다',
            data: '이메일 인증 성공'
        });

    } catch (error) {
        console.error('이메일 인증 오류:', error);
        res.status(500).json({
            success: false,
            error: '이메일 인증 중 오류가 발생했습니다'
        });
    }
});

// 토큰 갱신
router.post('/refresh', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId);
        if (!user) {
            return res.status(404).json({
                success: false,
                error: '사용자를 찾을 수 없습니다'
            });
        }

        // 새 토큰 생성
        const token = generateToken(user._id);

        console.log(`🔄 토큰 갱신: ${user.username}`);

        res.json({
            success: true,
            message: '토큰이 갱신되었습니다',
            data: {
                token,
                user: {
                    id: user._id,
                    username: user.username,
                    email: user.email,
                    isEmailVerified: user.isEmailVerified,
                    bookmarks: user.bookmarks,
                    favorites: user.favorites,
                    createdAt: user.createdAt,
                    updatedAt: user.updatedAt
                }
            }
        });

    } catch (error) {
        console.error('토큰 갱신 오류:', error);
        res.status(500).json({
            success: false,
            error: '토큰 갱신 중 오류가 발생했습니다'
        });
    }
});

// 로그아웃
router.post('/logout', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId);
        
        console.log(`👋 사용자 로그아웃: ${user ? user.username : req.userId}`);

        res.json({
            success: true,
            message: '로그아웃이 완료되었습니다',
            data: '로그아웃 성공'
        });

    } catch (error) {
        console.error('로그아웃 오류:', error);
        res.status(500).json({
            success: false,
            error: '로그아웃 중 오류가 발생했습니다'
        });
    }
});

// 비밀번호 재설정 요청 (향후 구현)
router.post('/forgot-password', [
    body('email')
        .isEmail()
        .normalizeEmail()
        .withMessage('올바른 이메일 주소를 입력해주세요')
], async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                success: false,
                error: '올바른 이메일 주소를 입력해주세요'
            });
        }

        const { email } = req.body;
        const user = await User.findOne({ email });

        // 보안상 사용자 존재 여부와 관계없이 동일한 응답
        res.json({
            success: true,
            message: '비밀번호 재설정 링크가 이메일로 전송되었습니다',
            data: '비밀번호 재설정 요청 처리됨'
        });

        if (user) {
            const resetToken = user.generatePasswordResetToken();
            await user.save();
            console.log(`🔑 비밀번호 재설정 요청: ${email}`);
            // 여기서 이메일 전송 로직 구현
        }

    } catch (error) {
        console.error('비밀번호 재설정 요청 오류:', error);
        res.status(500).json({
            success: false,
            error: '비밀번호 재설정 요청 중 오류가 발생했습니다'
        });
    }
});

// 현재 사용자 정보 조회
router.get('/me', auth, async (req, res) => {
    try {
        const user = await User.findById(req.userId)
                               .populate('bookmarks', 'name category rating imageUrl')
                               .populate('favorites', 'name category rating imageUrl');

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
        console.error('사용자 정보 조회 오류:', error);
        res.status(500).json({
            success: false,
            error: '사용자 정보 조회 중 오류가 발생했습니다'
        });
    }
});

module.exports = router;