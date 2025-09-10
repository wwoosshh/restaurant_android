const jwt = require('jsonwebtoken');
const User = require('../models/User');

// JWT 토큰 인증 미들웨어
const auth = async (req, res, next) => {
    try {
        // Authorization 헤더에서 토큰 추출
        const authHeader = req.header('Authorization');
        
        if (!authHeader) {
            return res.status(401).json({
                success: false,
                error: '접근 권한이 없습니다. 로그인이 필요합니다.'
            });
        }

        // "Bearer TOKEN" 형식에서 토큰 추출
        const token = authHeader.replace('Bearer ', '');
        
        if (!token) {
            return res.status(401).json({
                success: false,
                error: '유효하지 않은 토큰 형식입니다.'
            });
        }

        // 토큰 검증
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'fallback-secret-key');
        
        // 사용자 존재 확인
        const user = await User.findById(decoded.userId);
        if (!user || !user.isActive) {
            return res.status(401).json({
                success: false,
                error: '유효하지 않은 사용자입니다.'
            });
        }

        // 요청 객체에 사용자 정보 추가
        req.userId = decoded.userId;
        req.user = user;

        next();

    } catch (error) {
        console.error('JWT 인증 오류:', error);
        
        if (error.name === 'JsonWebTokenError') {
            return res.status(401).json({
                success: false,
                error: '잘못된 토큰입니다.'
            });
        }
        
        if (error.name === 'TokenExpiredError') {
            return res.status(401).json({
                success: false,
                error: '토큰이 만료되었습니다. 다시 로그인해주세요.'
            });
        }

        res.status(500).json({
            success: false,
            error: '토큰 인증 중 오류가 발생했습니다.'
        });
    }
};

// 선택적 인증 미들웨어 (토큰이 있으면 사용자 정보 추가, 없어도 진행)
const optionalAuth = async (req, res, next) => {
    try {
        const authHeader = req.header('Authorization');
        
        if (!authHeader) {
            return next(); // 토큰이 없어도 계속 진행
        }

        const token = authHeader.replace('Bearer ', '');
        
        if (!token) {
            return next(); // 토큰이 없어도 계속 진행
        }

        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'fallback-secret-key');
        const user = await User.findById(decoded.userId);
        
        if (user && user.isActive) {
            req.userId = decoded.userId;
            req.user = user;
        }

        next();

    } catch (error) {
        // 토큰 검증 실패해도 계속 진행 (선택적 인증)
        next();
    }
};

// 관리자 권한 확인 미들웨어
const adminAuth = async (req, res, next) => {
    try {
        await auth(req, res, () => {
            if (req.user && req.user.role === 'admin') {
                next();
            } else {
                return res.status(403).json({
                    success: false,
                    error: '관리자 권한이 필요합니다.'
                });
            }
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: '권한 확인 중 오류가 발생했습니다.'
        });
    }
};

module.exports = {
    auth,
    optionalAuth,
    adminAuth
};