const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5159;

// 보안 미들웨어
app.use(helmet({
    crossOriginEmbedderPolicy: false,
    contentSecurityPolicy: false
}));

// CORS 설정 (한국어 앱 개발 환경 고려)
const corsOptions = {
    origin: [
        'http://localhost:3000',
        'http://10.0.2.2:5159',  // Android 에뮬레이터
        'http://127.0.0.1:5159', // 로컬 테스트
        'http://58.233.102.165:5159' // 배포 서버 (예시)
    ],
    credentials: true,
    optionsSuccessStatus: 200
};
app.use(cors(corsOptions));

// 요청 압축
app.use(compression());

// 로깅 (개발 환경)
if (process.env.NODE_ENV === 'development') {
    app.use(morgan('dev'));
}

// Rate limiting (API 남용 방지) - 현재 비활성화
// const limiter = rateLimit({
//     windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 15 * 60 * 1000, // 15분
//     max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 100, // 최대 100회 요청
//     message: {
//         success: false,
//         error: '너무 많은 요청이 발생했습니다. 잠시 후 다시 시도해주세요.'
//     },
//     standardHeaders: true,
//     legacyHeaders: false
// });
// app.use('/api/', limiter);

// Body parser 미들웨어
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// MongoDB 연결 (CLAUDE.md 규칙: restaurant_app 데이터베이스)
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/restaurant_app')
.then(() => {
    console.log('✅ MongoDB 연결 성공: restaurant_app 데이터베이스');
})
.catch((error) => {
    console.error('❌ MongoDB 연결 실패:', error);
    process.exit(1);
});

// 정적 파일 제공 (업로드된 이미지 등)
app.use('/uploads', express.static('uploads'));

// API 라우트 등록
app.use('/api/auth', require('./routes/auth'));
app.use('/api/restaurants', require('./routes/restaurants'));
app.use('/api/users', require('./routes/users'));

// 기본 라우트 (서버 상태 확인)
app.get('/', (req, res) => {
    res.json({
        success: true,
        message: '🍽️ 맛집 추천 서버가 정상 동작 중입니다!',
        version: '2.1.0',
        port: PORT,
        environment: process.env.NODE_ENV || 'development',
        database: 'restaurant_app',
        timestamp: new Date().toISOString()
    });
});

// API 상태 확인 엔드포인트
app.get('/api/health', async (req, res) => {
    try {
        // 데이터베이스 연결 상태 확인
        const dbState = mongoose.connection.readyState;
        const dbStatus = dbState === 1 ? 'connected' : 'disconnected';
        
        res.json({
            success: true,
            status: 'healthy',
            database: dbStatus,
            uptime: process.uptime(),
            timestamp: new Date().toISOString(),
            memory: process.memoryUsage(),
            version: '2.1.0'
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            status: 'unhealthy',
            error: error.message
        });
    }
});

// 404 핸들러
app.use('*', (req, res) => {
    res.status(404).json({
        success: false,
        message: '요청한 API 엔드포인트를 찾을 수 없습니다.',
        path: req.originalUrl
    });
});

// 전역 에러 핸들러
app.use((error, req, res, next) => {
    console.error('🚨 서버 오류:', error);
    
    // Mongoose 유효성 검사 오류
    if (error.name === 'ValidationError') {
        const errors = Object.values(error.errors).map(err => err.message);
        return res.status(400).json({
            success: false,
            error: '입력 데이터가 올바르지 않습니다.',
            details: errors
        });
    }
    
    // JWT 토큰 오류
    if (error.name === 'JsonWebTokenError') {
        return res.status(401).json({
            success: false,
            error: '잘못된 인증 토큰입니다.'
        });
    }
    
    // MongoDB 중복 키 오류
    if (error.code === 11000) {
        return res.status(400).json({
            success: false,
            error: '이미 존재하는 데이터입니다.'
        });
    }
    
    // 기본 서버 오류
    res.status(error.status || 500).json({
        success: false,
        error: error.message || '서버 내부 오류가 발생했습니다.',
        ...(process.env.NODE_ENV === 'development' && { stack: error.stack })
    });
});

// 서버 시작
const server = app.listen(PORT, () => {
    console.log('🚀 맛집 추천 서버 시작됨');
    console.log(`📡 포트: ${PORT} (CLAUDE.md 규칙 준수)`);
    console.log(`🗄️ 데이터베이스: restaurant_app`);
    console.log(`🌍 환경: ${process.env.NODE_ENV || 'development'}`);
    console.log(`⏰ 시작 시간: ${new Date().toLocaleString('ko-KR')}`);
    console.log('====================================');
});

// 정리 함수
process.on('SIGTERM', () => {
    console.log('SIGTERM 신호 받음, 서버 종료 중...');
    server.close(() => {
        console.log('서버 종료 완료');
        mongoose.connection.close();
        process.exit(0);
    });
});

process.on('SIGINT', () => {
    console.log('SIGINT 신호 받음, 서버 종료 중...');
    server.close(() => {
        console.log('서버 종료 완료');
        mongoose.connection.close();
        process.exit(0);
    });
});

module.exports = app;