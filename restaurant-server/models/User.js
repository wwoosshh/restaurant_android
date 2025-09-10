const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema({
    username: {
        type: String,
        required: [true, '사용자명은 필수입니다'],
        trim: true,
        minlength: [2, '사용자명은 최소 2자 이상이어야 합니다'],
        maxlength: [30, '사용자명은 최대 30자까지 입니다']
    },
    email: {
        type: String,
        required: [true, '이메일은 필수입니다'],
        trim: true,
        lowercase: true,
        validate: {
            validator: function(email) {
                return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
            },
            message: '올바른 이메일 형식이 아닙니다'
        }
    },
    password: {
        type: String,
        required: [true, '비밀번호는 필수입니다'],
        minlength: [6, '비밀번호는 최소 6자 이상이어야 합니다'],
        select: false // 기본적으로 비밀번호는 조회되지 않음
    },
    isEmailVerified: {
        type: Boolean,
        default: false
    },
    emailVerificationCode: {
        type: String,
        select: false
    },
    emailVerificationExpires: {
        type: Date,
        select: false
    },
    bookmarks: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Restaurant'
    }],
    favorites: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Restaurant'
    }],
    profileImage: {
        type: String,
        default: null
    },
    preferences: {
        favoriteCategories: [{
            type: String,
            enum: ['한식', '중식', '일식', '양식', '카페', '디저트', '패스트푸드', '기타']
        }],
        priceRange: {
            type: String,
            enum: ['저렴', '보통', '비싸', '고급'],
            default: '보통'
        },
        dietaryRestrictions: [{
            type: String,
            enum: ['채식', '비건', '글루텐프리', '할랄', '코셔']
        }]
    },
    location: {
        type: {
            type: String,
            enum: ['Point'],
            default: 'Point'
        },
        coordinates: {
            type: [Number], // [longitude, latitude]
            default: [126.9784, 37.5666] // 서울 시청 기본 좌표
        }
    },
    lastLoginAt: {
        type: Date,
        default: Date.now
    },
    isActive: {
        type: Boolean,
        default: true
    },
    resetPasswordToken: {
        type: String,
        select: false
    },
    resetPasswordExpires: {
        type: Date,
        select: false
    }
}, {
    timestamps: true,
    toJSON: { virtuals: true },
    toObject: { virtuals: true }
});

// 인덱스 설정 (unique 인덱스로 이메일 중복 방지)
userSchema.index({ email: 1 }, { unique: true });
userSchema.index({ location: '2dsphere' });
userSchema.index({ createdAt: -1 });
userSchema.index({ bookmarks: 1 });
userSchema.index({ favorites: 1 });

// 가상 필드: 북마크 개수
userSchema.virtual('bookmarkCount').get(function() {
    return this.bookmarks ? this.bookmarks.length : 0;
});

// 가상 필드: 즐겨찾기 개수
userSchema.virtual('favoriteCount').get(function() {
    return this.favorites ? this.favorites.length : 0;
});

// 비밀번호 해싱 미들웨어
userSchema.pre('save', async function(next) {
    // 비밀번호가 수정되지 않았으면 스킵
    if (!this.isModified('password')) return next();
    
    try {
        // 비밀번호 해싱
        const saltRounds = parseInt(process.env.BCRYPT_ROUNDS) || 12;
        this.password = await bcrypt.hash(this.password, saltRounds);
        next();
    } catch (error) {
        next(error);
    }
});

// 비밀번호 검증 메서드
userSchema.methods.comparePassword = async function(candidatePassword) {
    try {
        return await bcrypt.compare(candidatePassword, this.password);
    } catch (error) {
        throw new Error('비밀번호 검증 중 오류가 발생했습니다');
    }
};

// 이메일 인증 코드 생성
userSchema.methods.generateEmailVerificationCode = function() {
    const code = Math.floor(100000 + Math.random() * 900000).toString(); // 6자리 숫자
    this.emailVerificationCode = code;
    this.emailVerificationExpires = Date.now() + 10 * 60 * 1000; // 10분 후 만료
    return code;
};

// 비밀번호 재설정 토큰 생성
userSchema.methods.generatePasswordResetToken = function() {
    const resetToken = require('crypto').randomBytes(32).toString('hex');
    this.resetPasswordToken = require('crypto')
        .createHash('sha256')
        .update(resetToken)
        .digest('hex');
    this.resetPasswordExpires = Date.now() + 10 * 60 * 1000; // 10분 후 만료
    return resetToken;
};

// 북마크 추가/제거
userSchema.methods.toggleBookmark = function(restaurantId) {
    const bookmarkIndex = this.bookmarks.indexOf(restaurantId);
    if (bookmarkIndex > -1) {
        this.bookmarks.splice(bookmarkIndex, 1);
        return false; // 북마크 제거됨
    } else {
        this.bookmarks.push(restaurantId);
        return true; // 북마크 추가됨
    }
};

// 즐겨찾기 추가/제거
userSchema.methods.toggleFavorite = function(restaurantId) {
    const favoriteIndex = this.favorites.indexOf(restaurantId);
    if (favoriteIndex > -1) {
        this.favorites.splice(favoriteIndex, 1);
        return false; // 즐겨찾기 제거됨
    } else {
        this.favorites.push(restaurantId);
        return true; // 즐겨찾기 추가됨
    }
};

// 북마크 상태 확인
userSchema.methods.isBookmarked = function(restaurantId) {
    return this.bookmarks.includes(restaurantId);
};

// 즐겨찾기 상태 확인
userSchema.methods.isFavorited = function(restaurantId) {
    return this.favorites.includes(restaurantId);
};

// 사용자 활동 업데이트
userSchema.methods.updateActivity = function() {
    this.lastLoginAt = Date.now();
    return this.save();
};

// JSON 출력 시 민감한 정보 제거
userSchema.methods.toJSON = function() {
    const userObject = this.toObject();
    delete userObject.password;
    delete userObject.emailVerificationCode;
    delete userObject.emailVerificationExpires;
    delete userObject.resetPasswordToken;
    delete userObject.resetPasswordExpires;
    delete userObject.__v;
    return userObject;
};

// 정적 메서드: 이메일로 사용자 찾기 (비밀번호 포함)
userSchema.statics.findByEmailWithPassword = function(email) {
    return this.findOne({ email }).select('+password');
};

// 정적 메서드: 활성 사용자만 조회
userSchema.statics.findActiveUsers = function() {
    return this.find({ isActive: true });
};

module.exports = mongoose.model('User', userSchema);